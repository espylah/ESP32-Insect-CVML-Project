// ble_config.h
/**
 * Bluetooth Config Server configuration details.
 */
#pragma once
#include "esp_gatt_defs.h"
#include "esp_gap_ble_api.h"
#include <stdint.h>

extern const uint16_t service_uuid;
extern const uint16_t primary_service_uuid;
extern const uint16_t character_declaration_uuid;

extern const uint8_t config_uuid[16];
extern const uint8_t status_uuid[16];

extern const uint8_t char_prop_write;
extern const uint8_t char_prop_read_write;
extern const uint8_t char_prop_read_notify;

extern esp_ble_adv_params_t adv_params;
extern const esp_gatts_attr_db_t gatt_db[];
extern uint16_t handle_table[];


// GATT ATTRIBUTE INDEX: Used to reference the attributes in the table.
enum
{
    IDX_SVC,             // Primary service declaration attribute
    IDX_CHAR_CONFIG,     // Characteristic declaration for the device configuration
    IDX_CHAR_VAL_CONFIG, // Characteristic value holding the JSON provisioning/config data
    IDX_CHAR_STATUS,     // Characteristic declaration for device provisioning/status updates
    IDX_CHAR_VAL_STATUS, // Characteristic value used to read or notify current device status
    IDX_CHAR_CFG_STATUS,
    GATT_IDX_NB          // Total number of attributes in the GATT database
};