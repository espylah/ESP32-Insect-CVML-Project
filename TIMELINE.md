# Project Timeline & Status

This document tracks the current implementation status of Apisentra. It is updated as features are completed.

Legend: ✅ Done &nbsp; 🚧 In progress &nbsp; ⬜ Not started

---

## Platform & Backend

| # | Feature | Status |
|---|---------|--------|
| 1 | User sign-up & OTP login (email + stub mode) | ✅ |
| 2 | CSRF protection | ✅ |
| 3 | Device CRUD (create, list, filter, get) | ✅ |
| 4 | Species endpoint (`/api/species`) | ✅ |
| 5 | Device registration token generation | ✅ |
| 6 | Device provisioning & API key issuance | ✅ |
| 7 | Device authentication filter (stateless API key) | ✅ |
| 8 | Detection event ingestion endpoint | ⬜ |
| 9 | Detection event storage & querying | ⬜ |
| 10 | Detection aggregation / statistics API | ⬜ |

---

## ESP32 Firmware

| # | Feature | Status |
|---|---------|--------|
| 11 | Camera capture (OV2640, SVGA JPEG) | ✅ |
| 12 | SD card image storage | ✅ |
| 13 | Deep-sleep duty cycle (30 s) | ✅ |
| 14 | BLE GATT config service | ✅ |
| 15 | Serial provisioning console (`wifi` + `register`) | ✅ |
| 16 | Wi-Fi connection with event-based confirmation | ✅ |
| 17 | Device registration over HTTP(S) | ✅ |
| 18 | API key persistence in NVS after registration | ⬜ |
| 19 | Authenticated detection reporting to backend | ⬜ |
| 20 | On-device YOLO inference | ⬜ |
| 21 | Species configuration pull from backend | ⬜ |

---

## Provisioning Tools

| # | Feature | Status |
|---|---------|--------|
| 22 | GUI provisioner (Python/Tkinter) | ✅ |
| 23 | Two-step serial flow (Wi-Fi → Register) | ✅ |
| 24 | Simple serial bridge (`serialy.py`) | ✅ |
| 25 | BLE mobile provisioning app | ⬜ |

---

## Frontend Dashboard

| # | Feature | Status |
|---|---------|--------|
| 26 | Login page (password + OTP) | ✅ |
| 27 | Device list & create device | ✅ |
| 28 | Device detail / edit page | ⬜ |
| 29 | Detection event feed per device | ⬜ |
| 30 | Detection charts & statistics | ⬜ |
| 31 | Species configuration UI | ⬜ |

---

## Contributing

Apisentra is open source. Contributions are welcome.

### How to contribute

1. **Open an issue** — describe the bug or feature you want to work on. Wait for a maintainer to confirm the issue is in scope before starting work.

2. **Fork & branch** — once the issue is approved, fork the repository and create a branch named after the issue:
   ```
   git checkout -b issue-42-detection-endpoint
   ```

3. **Make your changes** — follow the existing code style. For the backend, add tests covering your changes (`*IT.java` / `*E2EIT.java`).

4. **Open a pull request** — reference the issue in the PR description (`Closes #42`). PRs without a linked issue will not be reviewed.

### Areas most needing help

- Detection event ingestion & storage (items 8–10)
- On-device YOLO inference integration (item 20)
- API key NVS persistence (item 18)
- Frontend detection visualisation (items 29–30)
