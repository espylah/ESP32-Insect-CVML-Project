// ble_config_utils.h
/**
 * Helper utility functions for the ble_config_server.
 */
#pragma once
#include "cJSON.h"
#include "config_repo.h"
#include <esp_err.h>

/**
 * Handle a write event that was sent to the GATT Config Characteristic.
 */
esp_err_t ble_config_handle_write(config_repo_t *repo, const char *json, size_t len);

/**
 * Handle a READ EVENT from the BLE GATT service (the configuration read).
 */
char *ble_config_build_read_json(config_repo_t *repo); // returns masked JSON, caller must free