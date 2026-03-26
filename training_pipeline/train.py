import os
import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import DataLoader, random_split
from torchvision import datasets, transforms
from torchvision.transforms import functional as TF
from PIL import Image

# ── Pad-then-resize transform ────────────────────────────────────────────────

class SquarePad:
    """Pad image to square with white background, preserving aspect ratio."""
    def __call__(self, img):
        w, h = img.size
        diff = abs(w - h)
        pad_a, pad_b = diff // 2, diff - diff // 2
        if w < h:
            padding = (pad_a, 0, pad_b, 0)   # left, top, right, bottom
        else:
            padding = (0, pad_a, 0, pad_b)
        return TF.pad(img, padding, fill=255)  # white fill

TARGET        = 32   # keep small – fits comfortably in ESP32 SRAM
VALID_CLASSES = ('A_Mellifera', 'V_Crabro', 'V_V_Nigrithorax')
DATA_DIR      = os.path.dirname(os.path.abspath(__file__))


class InsectImageFolder(datasets.ImageFolder):
    """ImageFolder restricted to VALID_CLASSES, ignoring __pycache__ and other dirs."""
    def find_classes(self, directory):
        valid = set(VALID_CLASSES)
        classes = sorted([
            d for d in os.listdir(directory)
            if os.path.isdir(os.path.join(directory, d)) and d in valid
        ])
        if not classes:
            raise FileNotFoundError(f"No valid class directories found in {directory}")
        return classes, {c: i for i, c in enumerate(classes)}

# RGB normalisation: per-channel mean/std matching ImageNet-style [-1, 1] range.
# Using identical values for all channels keeps preprocessing simple on the ESP32
# (one scale + zero-point applies to all three channels).
_MEAN = (0.5, 0.5, 0.5)
_STD  = (0.5, 0.5, 0.5)

train_tf = transforms.Compose([
    SquarePad(),
    transforms.Resize((TARGET, TARGET)),
    transforms.RandomRotation(10),
    transforms.RandomAffine(0, translate=(0.05, 0.05), scale=(0.9, 1.1)),
    transforms.ColorJitter(brightness=0.2, contrast=0.2, saturation=0.2),
    transforms.ToTensor(),               # [0,1], RGB
    transforms.Normalize(_MEAN, _STD),   # → [-1,1]
])

val_tf = transforms.Compose([
    SquarePad(),
    transforms.Resize((TARGET, TARGET)),
    transforms.ToTensor(),
    transforms.Normalize(_MEAN, _STD),
])

# ── Model ────────────────────────────────────────────────────────────────────

class LetterCNN(nn.Module):
    """Tiny RGB CNN designed to fit ESP32 SRAM (~25 K params, ~100 KB float32 / ~25 KB int8).
    Input: 3 × 32 × 32  (RGB)
    Block 1 →  8 × 16 × 16
    Block 2 → 16 ×  8 ×  8
    Block 3 → 32 ×  4 ×  4  (512-D flatten)
    FC      → 32 → num_classes
    """
    def __init__(self, num_classes=3):
        super().__init__()
        self.features = nn.Sequential(
            # Block 1 – 32→16
            nn.Conv2d(3,  8, 3, padding=1), nn.BatchNorm2d(8),  nn.ReLU(),
            nn.MaxPool2d(2),
            # Block 2 – 16→8
            nn.Conv2d(8, 16, 3, padding=1), nn.BatchNorm2d(16), nn.ReLU(),
            nn.MaxPool2d(2),
            # Block 3 – 8→4
            nn.Conv2d(16, 32, 3, padding=1), nn.BatchNorm2d(32), nn.ReLU(),
            nn.MaxPool2d(2),
        )
        self.classifier = nn.Sequential(
            nn.Flatten(),
            nn.Linear(32 * 4 * 4, 32), nn.ReLU(), nn.Dropout(0.3),
            nn.Linear(32, num_classes),
        )

    def forward(self, x):
        return self.classifier(self.features(x))

# ── Inference helpers ─────────────────────────────────────────────────────────

def load_model(path="best_model.pth", device=None):
    """Load the saved model; returns (model, idx_to_class, img_size)."""
    if device is None:
        device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    ckpt = torch.load(path, map_location=device, weights_only=False)
    m = LetterCNN(num_classes=len(ckpt["class_to_idx"])).to(device)
    m.load_state_dict(ckpt["model_state"])
    m.eval()
    return m, ckpt["idx_to_class"], ckpt["img_size"]


def predict(image_path, model=None, idx_to_class=None, img_size=None, device=None):
    """Return predicted class label for a single image file."""
    if model is None:
        model, idx_to_class, img_size = load_model()
    if device is None:
        device = next(model.parameters()).device
    img = Image.open(image_path).convert("RGB")
    tf = transforms.Compose([
        SquarePad(),
        transforms.Resize((img_size, img_size)),
        transforms.ToTensor(),
        transforms.Normalize(_MEAN, _STD),
    ])
    x = tf(img).unsqueeze(0).to(device)
    with torch.no_grad():
        idx = model(x).argmax(1).item()
    return idx_to_class[idx]

# ── Training ──────────────────────────────────────────────────────────────────

def make_dataset(transform):
    ds = InsectImageFolder(DATA_DIR, transform=transform)
    ds.targets = [s[1] for s in ds.samples]
    return ds


if __name__ == "__main__":
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print(f"Device: {device}")

    full_ds = make_dataset(train_tf)
    print(f"Classes: {full_ds.classes}  |  Total samples: {len(full_ds)}")

    # 80/20 train/val split
    n_val   = max(1, int(len(full_ds) * 0.2))
    n_train = len(full_ds) - n_val
    train_ds, val_ds = random_split(full_ds, [n_train, n_val],
                                    generator=torch.Generator().manual_seed(42))

    # Give val split clean (non-augmented) transforms
    val_ds.dataset = make_dataset(val_tf)

    train_loader = DataLoader(train_ds, batch_size=16, shuffle=True,  num_workers=2)
    val_loader   = DataLoader(val_ds,   batch_size=16, shuffle=False, num_workers=2)

    model     = LetterCNN(num_classes=len(full_ds.classes)).to(device)
    criterion = nn.CrossEntropyLoss()
    optimizer = optim.AdamW(model.parameters(), lr=1e-3, weight_decay=1e-4)
    scheduler = optim.lr_scheduler.CosineAnnealingLR(optimizer, T_max=50)

    EPOCHS       = 50
    best_val_acc = 0.0

    for epoch in range(1, EPOCHS + 1):
        # --- train ---
        model.train()
        train_loss, train_correct = 0.0, 0
        for imgs, labels in train_loader:
            imgs, labels = imgs.to(device), labels.to(device)
            optimizer.zero_grad()
            out  = model(imgs)
            loss = criterion(out, labels)
            loss.backward()
            optimizer.step()
            train_loss    += loss.item() * imgs.size(0)
            train_correct += (out.argmax(1) == labels).sum().item()
        scheduler.step()

        # --- val ---
        model.eval()
        val_loss, val_correct = 0.0, 0
        with torch.no_grad():
            for imgs, labels in val_loader:
                imgs, labels = imgs.to(device), labels.to(device)
                out       = model(imgs)
                val_loss += criterion(out, labels).item() * imgs.size(0)
                val_correct += (out.argmax(1) == labels).sum().item()

        t_acc  = train_correct / n_train * 100
        v_acc  = val_correct   / n_val   * 100
        t_loss = train_loss    / n_train
        v_loss = val_loss      / n_val

        print(f"Epoch {epoch:3d}/{EPOCHS}  "
              f"train loss={t_loss:.4f} acc={t_acc:.1f}%  |  "
              f"val loss={v_loss:.4f} acc={v_acc:.1f}%")

        if v_acc > best_val_acc:
            best_val_acc = v_acc
            torch.save({
                "model_state": model.state_dict(),
                "class_to_idx": full_ds.class_to_idx,
                "idx_to_class": {v: k for k, v in full_ds.class_to_idx.items()},
                "img_size": TARGET,
            }, "best_model.pth")

    print(f"\nBest val accuracy: {best_val_acc:.1f}%  →  saved to best_model.pth")
    print(f"Class mapping: {full_ds.class_to_idx}")
