# ESP-YALH

## Introduction

## Device Provisioning

1. The user logs into the application (either the Web application UX or the provisioning application).

2. The user creates a new device entry in the system and provides initial configuration (e.g., device name, inference threshold, location, or other application settings).

3. The user opens the provisioning application and logs in if not already authenticated.

4. The provisioning application requests a **short-lived provisioning token** from the server for the device being provisioned.

5. The provisioning application connects to the device via BLE and sends:

   * WiFi SSID
   * WiFi password
   * provisioning_token

6. The device stores the WiFi credentials and attempts to connect to the network.

7. After successfully connecting to WiFi, the device calls the server **device registration endpoint** with:

   * device_id (e.g., MAC address)
   * provisioning_token

8. The server validates the provisioning token and determines the user account that initiated provisioning.

9. The server:

   * assigns the device to the user account
   * generates a permanent **device_token** used for device authentication

10. The server returns the **device_token** to the device.

11. The device stores the **device_token** in persistent storage (e.g., NVS).

12. The device authenticates with the server using the **device_token** and requests its configuration.

13. The server returns the configuration previously created by the user.

14. The device applies the configuration and enters normal operation.

```json

{"name":"Bobs Device","id":"1a6da463-fe22-40a3-94f4-9e66d17d0a9b","runMode":"ALWAYS_ON","targets":[{"specie":"VESPA_CABRO","threshold":0.75},{"specie":"VESPA_VELUTINA_NIGRITHORAX","threshold":0.71}]}
```