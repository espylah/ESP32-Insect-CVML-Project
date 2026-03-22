#pragma once
#include "esp_err.h"
#include "cJSON.h"

typedef struct {
    cJSON *root;           // parsed JSON in memory
    char buffer[512];      // raw JSON blob loaded from NVS
} config_repo_t;

/**
 * Initialize the config repository and load JSON from NVS.
 * Returns ESP_OK if loaded successfully, else ESP_FAIL.
 */
esp_err_t config_repo_init(config_repo_t *repo);

/**
 * Free any allocated resources
 */
void config_repo_deinit(config_repo_t *repo);

/**
 * Save the cJSON
 */
esp_err_t nvs_save_json(cJSON *root);

/**
 * Getters
 */
const char* config_repo_get_ssid(config_repo_t *repo);
const char* config_repo_get_password(config_repo_t *repo);
int config_repo_get_threshold(config_repo_t *repo);
int config_repo_get_run_mode(config_repo_t *repo);
long config_repo_get_unixtime(config_repo_t *repo);
int config_repo_get_config_version(config_repo_t *repo);

/**
 * Get sanitized JSON (for BLE read-back, hides sensitive fields)
 * Returns pointer to internal buffer; do not free
 */
const char* config_repo_get_sanitized_json(config_repo_t *repo);

/**
 * Optional setters (updates memory + NVS)
 */
esp_err_t config_repo_set_ssid(config_repo_t *repo, const char *ssid);
esp_err_t config_repo_set_password(config_repo_t *repo, const char *password);
esp_err_t config_repo_set_threshold(config_repo_t *repo, int threshold);
esp_err_t config_repo_set_run_mode(config_repo_t *repo, int run_mode);
esp_err_t config_repo_set_unixtime(config_repo_t *repo, long unixtime);
esp_err_t config_repo_set_config_version(config_repo_t *repo, int version);

/**
 * Backend-assigned device configuration
 */
const char* config_repo_get_name(config_repo_t *repo);
esp_err_t   config_repo_set_name(config_repo_t *repo, const char *name);

// targets: cJSON array of {"specie":"...","threshold":0.7} objects.
// Getter returns a borrowed pointer into repo->root — do not free.
cJSON*    config_repo_get_targets(config_repo_t *repo);
esp_err_t config_repo_set_targets(config_repo_t *repo, cJSON *targets_array);