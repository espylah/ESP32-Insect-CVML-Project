#include "nvs_util.h"
#include "nvs_flash.h"
#include "esp_log.h"
#include <string.h>
#include "nvs_sec_provider.h"

static const char *TAG = "NVS_UTIL";

esp_err_t nvs_util_init_default(void)
{
    esp_err_t ret = nvs_flash_init();
    if (ret == ESP_ERR_NVS_NO_FREE_PAGES || ret == ESP_ERR_NVS_NEW_VERSION_FOUND)
    {
        ESP_ERROR_CHECK(nvs_flash_erase());
        ret = nvs_flash_init();
    }
    ESP_ERROR_CHECK(ret);
    ESP_LOGI(TAG, "Default NVS initialized");
    return ESP_OK;
}

esp_err_t nvs_util_init_secure(const char *partition_name)
{

    nvs_sec_cfg_t cfg = {};
    nvs_sec_scheme_t *sec_scheme_handle = NULL;
    nvs_sec_config_hmac_t sec_scheme_cfg = {};
    hmac_key_id_t hmac_key = HMAC_KEY0;
    sec_scheme_cfg.hmac_key_id = hmac_key;

    esp_err_t ret = nvs_flash_secure_init_partition(partition_name, &cfg);
    if (ret != ESP_OK)
    {
        ESP_LOGE(TAG, "Failed to init secure NVS partition '%s': 0x%x", partition_name, ret);
    }
    else
    {
        ESP_LOGI(TAG, "Secure NVS partition '%s' initialized", partition_name);
    }
    return ret;
}

esp_err_t nvs_util_set_str_from_partition(const char *partition_name, const char *namespace_name,
                                          const char *key, const char *value)
{
    nvs_handle_t handle;
    esp_err_t ret;

    if (partition_name)
    {
        ret = nvs_open_from_partition(partition_name, namespace_name, NVS_READWRITE, &handle);
    }
    else
    {
        ret = nvs_open(namespace_name, NVS_READWRITE, &handle);
    }
    ESP_ERROR_CHECK(ret);

    ret = nvs_set_str(handle, key, value);
    ESP_ERROR_CHECK(ret);

    ret = nvs_commit(handle);
    ESP_ERROR_CHECK(ret);

    nvs_close(handle);
    ESP_LOGI(TAG, "Key '%s' stored in namespace '%s' (partition '%s')", key, namespace_name,
             partition_name ? partition_name : "default");
    return ESP_OK;
}

esp_err_t nvs_util_get_str_from_partition(const char *partition_name, const char *namespace_name,
                                          const char *key, char *out_value, size_t max_len)
{
    nvs_handle_t handle;
    esp_err_t ret;

    if (partition_name)
    {
        ret = nvs_open_from_partition(partition_name, namespace_name, NVS_READWRITE, &handle);
    }
    else
    {
        ret = nvs_open(namespace_name, NVS_READWRITE, &handle);
    }
    ESP_ERROR_CHECK(ret);

    ret = nvs_get_str(handle, key, out_value, &max_len);
    if (ret == ESP_OK)
    {
        ESP_LOGI(TAG, "Key '%s' read from namespace '%s' (partition '%s')", key, namespace_name,
                 partition_name ? partition_name : "default");
    }
    else if (ret == ESP_ERR_NVS_NOT_FOUND)
    {
        ESP_LOGW(TAG, "Key '%s' not found in namespace '%s'", key, namespace_name);
    }
    nvs_close(handle);
    return ret;
}
