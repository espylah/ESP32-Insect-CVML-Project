// ble_config.c (top section)
/**
 * Implementation of the ble configuration for the ble config service.
 */
#include "ble_config.h"
#include "esp_gatts_api.h"

esp_ble_adv_params_t adv_params = {
    .adv_int_min = 0x20,
    .adv_int_max = 0x40,
    .adv_type = ADV_TYPE_IND,
    .own_addr_type = BLE_ADDR_TYPE_PUBLIC,
    .channel_map = ADV_CHNL_ALL,
    .adv_filter_policy = ADV_FILTER_ALLOW_SCAN_ANY_CON_ANY,
};

/**
 * The primary service uuid - I.e. the GATT Service ID
 */
const uint16_t service_uuid = 0xFFF0;
const uint16_t primary_service_uuid = ESP_GATT_UUID_PRI_SERVICE;
const uint16_t character_declaration_uuid = ESP_GATT_UUID_CHAR_DECLARE;

/**
 * Custom 16bye/128bit UUIDs for the 2 characteristics. One is the configuration characteristic (JSON I/O) and one the provisioning status.
 */
const uint8_t config_uuid[16] = {0x10, 0x00, 0x00, 0x00, 0x34, 0x12, 0x78, 0x56, 0x12, 0x34, 0x56, 0x78, 0x90, 0xab, 0xcd, 0xef};
const uint8_t status_uuid[16] = {0x11, 0x00, 0x00, 0x00, 0x34, 0x12, 0x78, 0x56, 0x12, 0x34, 0x56, 0x78, 0x90, 0xab, 0xcd, 0xef};

/**
 * Helper variables to declare the characteristics permissions for the client.
 */
const uint8_t char_prop_write = ESP_GATT_CHAR_PROP_BIT_WRITE;
const uint8_t char_prop_read_write = ESP_GATT_CHAR_PROP_BIT_READ | ESP_GATT_CHAR_PROP_BIT_WRITE;
const uint8_t char_prop_read_notify = ESP_GATT_CHAR_PROP_BIT_NOTIFY | ESP_GATT_CHAR_PROP_BIT_READ;
uint16_t cccd_default = 0x0000;
const uint16_t character_client_config_uuid = ESP_GATT_UUID_CHAR_CLIENT_CONFIG;

/* Table to store handles assigned by BLE stack */
uint16_t handle_table[GATT_IDX_NB];

/* GATT Attribute Database */
const esp_gatts_attr_db_t gatt_db[GATT_IDX_NB] = {

    // Primary Service
    [IDX_SVC] = {{ESP_GATT_AUTO_RSP},
                 {ESP_UUID_LEN_16, (uint8_t *)&primary_service_uuid, ESP_GATT_PERM_READ,
                  sizeof(uint16_t), sizeof(service_uuid), (uint8_t *)&service_uuid}},

    // Config Characteristic Declaration
    [IDX_CHAR_CONFIG] = {{ESP_GATT_AUTO_RSP},
                         {ESP_UUID_LEN_16, (uint8_t *)&character_declaration_uuid,
                          ESP_GATT_PERM_READ,
                          sizeof(char_prop_read_write),
                          sizeof(char_prop_read_write),
                          (uint8_t *)&char_prop_read_write}},

    // Config Value
    [IDX_CHAR_VAL_CONFIG] = {{ESP_GATT_RSP_BY_APP},
                             {ESP_UUID_LEN_128, config_uuid,
                              ESP_GATT_PERM_READ | ESP_GATT_PERM_WRITE,
                              512, 0, NULL}},

    // Status Characteristic Declaration
    [IDX_CHAR_STATUS] = {{ESP_GATT_AUTO_RSP},
                         {ESP_UUID_LEN_16, (uint8_t *)&character_declaration_uuid,
                          ESP_GATT_PERM_READ,
                          sizeof(char_prop_read_notify),
                          sizeof(char_prop_read_notify),
                          (uint8_t *)&char_prop_read_notify}},

    // Status Value
    [IDX_CHAR_VAL_STATUS] = {{ESP_GATT_RSP_BY_APP},
                             {ESP_UUID_LEN_128, status_uuid,
                              ESP_GATT_PERM_READ,
                              128, 0, NULL}},

    // Status CCCD
    [IDX_CHAR_CFG_STATUS] = {{ESP_GATT_AUTO_RSP},
                             {ESP_UUID_LEN_16, (uint8_t *)&character_client_config_uuid,
                              ESP_GATT_PERM_READ | ESP_GATT_PERM_WRITE,
                              sizeof(uint16_t), sizeof(cccd_default),
                              (uint8_t *)&cccd_default}},
};