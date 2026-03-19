// ble_config_utils.c
#include "ble_config_utils.h"
#include "esp_log.h"
#include "nvs_util.h"
#include <string.h>
#include <time.h>
#include <sys/time.h>

static const char *TAG = "BLE_UTILS";

esp_err_t ble_config_handle_write(config_repo_t *repo, const char *json, size_t len)
{
    cJSON *root = cJSON_Parse(json);
    if (!root)
    {
        ESP_LOGE(TAG, "Failed to parse JSON write");
        return ESP_FAIL;
    }

    nvs_save_json(root); // store to NVS

    if (repo->root)
        cJSON_Delete(repo->root);
    repo->root = root;

    size_t copy_len = len > sizeof(repo->buffer) - 1 ? sizeof(repo->buffer) - 1 : len;
    memcpy(repo->buffer, json, copy_len);
    repo->buffer[copy_len] = '\0';

    // Update RTC
    struct timeval tv = {.tv_sec = cJSON_GetObjectItem(root, "unixtime")->valueint, .tv_usec = 0};
    settimeofday(&tv, NULL);

    return ESP_OK;
}

char *ble_config_build_read_json(config_repo_t *repo)
{
    // No configuration yet → return fallback JSON
    if (!repo->root)
    {
        return strdup("{\"ERROR\":\"NO_CONFIGURATION_FOUND\"}");
    }

    // Duplicate the current repo JSON
    cJSON *copy = cJSON_Duplicate(repo->root, 1);
    if (!copy)
        return strdup("{\"ERROR\":\"JSON_DUP_FAIL\"}");

    // Mask the WiFi password
    cJSON *wifi = cJSON_GetObjectItemCaseSensitive(copy, "wifi");
    if (wifi)
    {
        cJSON *password = cJSON_GetObjectItemCaseSensitive(wifi, "password");
        if (password)
            cJSON_SetValuestring(password, "[REDACTED]");
    }

    // Update unixtime
    cJSON *unixtime_item = cJSON_GetObjectItemCaseSensitive(copy, "unixtime");
    if (unixtime_item)
        cJSON_SetIntValue(unixtime_item, time(NULL));

    // Convert to string
    char *masked = cJSON_PrintUnformatted(copy);
    cJSON_Delete(copy);

    // Fallback if printing fails
    if (!masked)
        masked = strdup("{\"ERROR\":\"JSON_PRINT_FAIL\"}");

    return masked;
}