import tkinter as tk
from tkinter import ttk, messagebox
from serial.tools import list_ports
import serial
import threading
import requests

# --- Globals ---
ser = None
devices = {}  # name -> uuid
jwt_token = None

# --- Helper Functions ---
def list_serial_ports():
    ports = list_ports.comports()
    return [port.device for port in ports]

def start_serial():
    global ser
    port = port_var.get()
    try:
        baud = int(baud_var.get())
    except ValueError:
        messagebox.showerror("Error", "Invalid baud rate")
        return

    try:
        ser = serial.Serial(port, baud, timeout=0.5)
        messagebox.showinfo("Serial", f"Connected to {port} at {baud} baud")
        threading.Thread(target=serial_read_thread, daemon=True).start()
    except Exception as e:
        messagebox.showerror("Error", f"Failed to open serial port:\n{e}")

def serial_read_thread():
    while True:
        try:
            line = ser.readline().decode(errors="ignore").strip()
            if not line:
                continue
            if line.startswith(("I (", "E (", "W (")):
                continue
            print(line)
        except Exception as e:
            print(f"Serial read error: {e}")

def send_command(cmd):
    if ser:
        ser.write((cmd + "\n").encode())

# --- API Functions ---
def login_to_api():
    global jwt_token, devices
    username = username_entry.get()
    password = password_entry.get()
    api_url = API_URL_ENTRY.get()

    if not username or not password:
        messagebox.showwarning("Login", "Enter username and password")
        return

    try:
        resp = requests.post(f"{api_url}/login", json={"username": username, "password": password}, timeout=5)
        resp.raise_for_status()
        # JWT from JSON or headers
        jwt_token = resp.json().get("token") or resp.headers.get("Authorization")
        if not jwt_token:
            messagebox.showerror("Login", "No JWT returned from API")
            return

        # Fetch unprovisioned devices
        headers = {"Authorization": f"Bearer {jwt_token}"}
        resp = requests.get(f"{api_url}/devices/unprovisioned", headers=headers, timeout=5)
        resp.raise_for_status()
        devices_list = resp.json()  # Expect [{"name":..., "uuid":...}, ...]
        devices = {d["name"]: d["uuid"] for d in devices_list}

        device_dropdown['values'] = list(devices.keys())
        if devices:
            device_var.set(list(devices.keys())[0])
        messagebox.showinfo("Login", f"Login successful, {len(devices)} unprovisioned devices found")
    except Exception as e:
        messagebox.showerror("Login Error", str(e))

def provision_device():
    if not ser:
        messagebox.showwarning("Serial", "Connect to ESP32 first")
        return
    if not devices:
        messagebox.showwarning("Devices", "No devices available")
        return
    selected_name = device_var.get()
    if selected_name not in devices:
        messagebox.showwarning("Devices", "Select a valid device")
        return

    ssid = ssid_entry.get()
    password = wifi_password_entry.get()
    if not ssid or not password:
        messagebox.showwarning("Provision", "Enter Wi-Fi credentials")
        return

    # Get registration token for selected device
    uuid = devices[selected_name]
    api_url = API_URL_ENTRY.get()
    try:
        headers = {"Authorization": f"Bearer {jwt_token}"}
        resp = requests.get(f"{api_url}/devices/{uuid}/registration-token", headers=headers, timeout=5)
        resp.raise_for_status()
        registration_token = resp.json().get("registration_token")
        if not registration_token:
            messagebox.showerror("Provision", "No registration token received")
            return
        # Send to ESP32
        cmd = f"provision {ssid} {password} {registration_token}"
        send_command(cmd)
        print(f"Sent to ESP32: {cmd}")
    except Exception as e:
        messagebox.showerror("Provision Error", str(e))

# --- GUI ---
root = tk.Tk()
root.title("ESP32 Provisioner")

# API URL
tk.Label(root, text="API URL").pack()
API_URL_ENTRY = tk.Entry(root)
API_URL_ENTRY.pack()
API_URL_ENTRY.insert(0, "https://api.example.com")

# Login
tk.Label(root, text="Username").pack()
username_entry = tk.Entry(root)
username_entry.pack()
tk.Label(root, text="Password").pack()
password_entry = tk.Entry(root, show="*")
password_entry.pack()
tk.Button(root, text="Login & Fetch Devices", command=login_to_api).pack()


# Serial port
tk.Label(root, text="Serial Port").pack()
port_var = tk.StringVar()
ports = list_serial_ports()
port_dropdown = ttk.Combobox(root, textvariable=port_var, values=ports, state="readonly")
port_dropdown.pack()
if ports:
    port_var.set(ports[0])

tk.Label(root, text="Baud Rate").pack()
baud_var = tk.StringVar(value="115200")
baud_dropdown = ttk.Combobox(root, textvariable=baud_var,
                             values=["9600","19200","38400","57600","115200"], state="readonly")
baud_dropdown.pack()

tk.Button(root, text="Connect Serial", command=start_serial).pack()

# Device dropdown
tk.Label(root, text="Select Device").pack()
device_var = tk.StringVar()
device_dropdown = ttk.Combobox(root, textvariable=device_var, values=[], state="readonly")
device_dropdown.pack()

# Wi-Fi credentials
tk.Label(root, text="Wi-Fi SSID").pack()
ssid_entry = tk.Entry(root)
ssid_entry.pack()
tk.Label(root, text="Wi-Fi Password").pack()
wifi_password_entry = tk.Entry(root)
wifi_password_entry.pack()

tk.Button(root, text="Provision Selected Device", command=provision_device).pack()

# Optional manual command entry
tk.Label(root, text="Manual Command (Optional)").pack()
command_entry = tk.Entry(root)
command_entry.pack()
tk.Button(root, text="Send", command=lambda: send_command(command_entry.get())).pack()

root.mainloop()