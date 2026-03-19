#pragma once
#include <stdint.h>
#include <stdbool.h>
#include "esp_gatts_api.h"

#define BLE_NOTIFY_MAX_PAYLOAD 16

// Event enum
typedef enum
{
    BLE_EVT_WIFI_CONNECTED,
    BLE_EVT_WIFI_FAILED,
    BLE_EVT_OTA_STARTED,
    BLE_EVT_OTA_COMPLETED,
    BLE_EVT_BUTTON_PRESSED,
    BLE_EVT_MAX,
    BLE_DEVICE_REGISTRATION_SUCCESS,
    BLE_DEVICE_REGISTRATION_FAILED,
    BLE_DEVICE_REGISTRATION_IN_PROGRESS,
} ble_notify_event_t;

// Notification message
typedef struct
{
    ble_notify_event_t event;
    uint8_t payload[BLE_NOTIFY_MAX_PAYLOAD];
    uint8_t payload_len;
} ble_notify_msg_t;

// Initialization
void ble_notify_init(void);

// Set GATT connection info
void ble_notify_set_char_handle(uint16_t handle);
void ble_notify_set_connection(esp_gatt_if_t gatts_if, uint16_t conn_id);
void ble_notify_set_enabled(bool enabled);

// Push event to queue
bool ble_notify_send_event(const ble_notify_msg_t *msg);