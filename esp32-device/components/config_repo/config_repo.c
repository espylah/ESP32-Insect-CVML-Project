#include "config_repo.h"
#include "nvs.h"
#include "nvs_flash.h"
#include "esp_log.h"
#include <string.h>

static const char *TAG = "CONFIG_REPO";
static const char *NVS_NAMESPACE = "nvs_usr";
static const char *NVS_KEY = "device_config";

esp_err_t nvs_save_json(cJSON *root)
{
    nvs_handle_t handle;
    esp_err_t err = nvs_open(NVS_NAMESPACE, NVS_READWRITE, &handle);
    if (err != ESP_OK)
        return err;

    char *json_str = cJSON_PrintUnformatted(root);
    if (!json_str)
    {
        nvs_close(handle);
        return ESP_FAIL;
    }

    err = nvs_set_blob(handle, NVS_KEY, json_str, strlen(json_str));
    if (err == ESP_OK)
        nvs_commit(handle);

    cJSON_free(json_str);
    nvs_close(handle);
    return err;
}

esp_err_t config_repo_init(config_repo_t *repo)
{
    if (!repo)
        return ESP_ERR_INVALID_ARG;

    // Free any existing parsed JSON first
    if (repo->root)
    {
        cJSON_Delete(repo->root);
        repo->root = NULL;
    }

    nvs_handle_t handle;
    size_t len = sizeof(repo->buffer);

    esp_err_t err = nvs_open(NVS_NAMESPACE, NVS_READONLY, &handle);
    if (err != ESP_OK)
        return ESP_FAIL;

    err = nvs_get_blob(handle, NVS_KEY, repo->buffer, &len);
    nvs_close(handle);

    if (err != ESP_OK)
    {
        ESP_LOGW(TAG, "No config in NVS yet");
        repo->root = cJSON_CreateObject();
        return ESP_OK;
    }

    repo->root = cJSON_ParseWithLength(repo->buffer, len);
    if (!repo->root)
    {
        ESP_LOGE(TAG, "Failed to parse JSON from NVS");
        repo->root = cJSON_CreateObject();
        return ESP_FAIL;
    }

    return ESP_OK;
}

void config_repo_deinit(config_repo_t *repo)
{
    if (repo && repo->root)
    {
        cJSON_Delete(repo->root);
        repo->root = NULL;
    }
}

// ---------- Getters ----------
int config_repo_get_config_version(config_repo_t *repo)
{
    cJSON *item = cJSON_GetObjectItemCaseSensitive(repo->root, "configVersion");
    return cJSON_IsNumber(item) ? item->valueint : 0;
}

const char *config_repo_get_ssid(config_repo_t *repo)
{
    cJSON *wifi = cJSON_GetObjectItemCaseSensitive(repo->root, "wifi");
    if (!cJSON_IsObject(wifi))
        return NULL;

    cJSON *ssid = cJSON_GetObjectItemCaseSensitive(wifi, "ssid");
    return cJSON_IsString(ssid) ? ssid->valuestring : NULL;
}

const char *config_repo_get_password(config_repo_t *repo)
{
    cJSON *wifi = cJSON_GetObjectItemCaseSensitive(repo->root, "wifi");
    if (!cJSON_IsObject(wifi))
        return NULL;

    cJSON *pass = cJSON_GetObjectItemCaseSensitive(wifi, "password");
    return cJSON_IsString(pass) ? pass->valuestring : NULL;
}

int config_repo_get_threshold(config_repo_t *repo)
{
    cJSON *item = cJSON_GetObjectItemCaseSensitive(repo->root, "threshold");
    return cJSON_IsNumber(item) ? item->valueint : 0;
}

int config_repo_get_run_mode(config_repo_t *repo)
{
    cJSON *item = cJSON_GetObjectItemCaseSensitive(repo->root, "run_mode");
    return cJSON_IsNumber(item) ? item->valueint : 0;
}

long config_repo_get_unixtime(config_repo_t *repo)
{
    cJSON *item = cJSON_GetObjectItemCaseSensitive(repo->root, "unixtime");
    return cJSON_IsNumber(item) ? (long)item->valuedouble : 0;
}

// ---------- Sanitized JSON for BLE read ----------
const char *config_repo_get_sanitized_json(config_repo_t *repo)
{
    static char buffer[512];

    cJSON *copy = cJSON_Duplicate(repo->root, 1); // deep copy
    if (!copy)
        return "{}";

    // Remove sensitive info
    cJSON *wifi = cJSON_GetObjectItemCaseSensitive(copy, "wifi");
    if (cJSON_IsObject(wifi))
    {
        cJSON_DeleteItemFromObject(wifi, "password");
    }

    char *str = cJSON_PrintUnformatted(copy);
    strncpy(buffer, str ? str : "{}", sizeof(buffer) - 1);
    buffer[sizeof(buffer) - 1] = '\0';

    cJSON_free(str);
    cJSON_Delete(copy);

    return buffer;
}

// ---------- Setters ----------
esp_err_t config_repo_set_ssid(config_repo_t *repo, const char *ssid)
{
    cJSON *wifi = cJSON_GetObjectItemCaseSensitive(repo->root, "wifi");
    if (!wifi)
    {
        wifi = cJSON_CreateObject();
        cJSON_AddItemToObject(repo->root, "wifi", wifi);
    }

    cJSON_ReplaceItemInObject(wifi, "ssid", cJSON_CreateString(ssid));
    return nvs_save_json(repo->root);
}

esp_err_t config_repo_set_password(config_repo_t *repo, const char *password)
{
    cJSON *wifi = cJSON_GetObjectItemCaseSensitive(repo->root, "wifi");
    if (!wifi)
    {
        wifi = cJSON_CreateObject();
        cJSON_AddItemToObject(repo->root, "wifi", wifi);
    }

    cJSON_ReplaceItemInObject(wifi, "password", cJSON_CreateString(password));
    return nvs_save_json(repo->root);
}

esp_err_t config_repo_set_threshold(config_repo_t *repo, int threshold)
{
    cJSON_ReplaceItemInObject(repo->root, "threshold", cJSON_CreateNumber(threshold));
    return nvs_save_json(repo->root);
}

esp_err_t config_repo_set_run_mode(config_repo_t *repo, int run_mode)
{
    cJSON_ReplaceItemInObject(repo->root, "run_mode", cJSON_CreateNumber(run_mode));
    return nvs_save_json(repo->root);
}

esp_err_t config_repo_set_unixtime(config_repo_t *repo, long unixtime)
{
    cJSON_ReplaceItemInObject(repo->root, "unixtime", cJSON_CreateNumber(unixtime));
    return nvs_save_json(repo->root);
}

esp_err_t config_repo_set_config_version(config_repo_t *repo, int version)
{
    cJSON_ReplaceItemInObject(repo->root, "configVersion", cJSON_CreateNumber(version));
    return nvs_save_json(repo->root);
}

const char *config_repo_get_name(config_repo_t *repo)
{
    cJSON *item = cJSON_GetObjectItemCaseSensitive(repo->root, "name");
    return cJSON_IsString(item) ? item->valuestring : NULL;
}

esp_err_t config_repo_set_name(config_repo_t *repo, const char *name)
{
    cJSON_ReplaceItemInObject(repo->root, "name", cJSON_CreateString(name));
    return nvs_save_json(repo->root);
}

cJSON *config_repo_get_targets(config_repo_t *repo)
{
    return cJSON_GetObjectItemCaseSensitive(repo->root, "targets");
}

esp_err_t config_repo_set_targets(config_repo_t *repo, cJSON *targets_array)
{
    cJSON_ReplaceItemInObject(repo->root, "targets", cJSON_Duplicate(targets_array, 1));
    return nvs_save_json(repo->root);
}