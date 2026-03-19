/* --- BLE Service --- */

#include "esp_log.h"
#include "esp_bt.h"
#include "esp_bt_main.h"
#include "esp_gap_ble_api.h"
#include "esp_gatts_api.h"
#include "esp_gatt_common_api.h"
#include <string.h>
#include "nvs_util.h"
#include "config_repo.h"
#include "ble_config_utils.h"
#include "ble_config.h"
#include "provisioning_service.h"
#include "ble_notify.h"

#define TAG "BLE_SERVICE"

// Config Repo instance
static config_repo_t repo;

/* Global GATT interface */
static esp_gatt_if_t gatts_if_global = 0;

/* Forward declarations */
static void gatts_event_handler(esp_gatts_cb_event_t event, esp_gatt_if_t gatts_if,
                                esp_ble_gatts_cb_param_t *param);
static void start_advertising(void);
static void ble_send_response(esp_gatt_if_t gatts_if, uint16_t conn_id, uint16_t trans_id,
                              uint16_t handle, const uint8_t *data, uint16_t len);
static void ble_send_config_json(esp_gatt_if_t gatts_if, uint16_t conn_id,
                                 uint16_t trans_id, size_t offset);

esp_err_t ble_service_init(void)
{
    ESP_ERROR_CHECK(nvs_util_init_default());
    ESP_ERROR_CHECK(nvs_util_init_secure("nvs_usr"));

    esp_bt_controller_mem_release(ESP_BT_MODE_CLASSIC_BT);

    esp_bt_controller_config_t bt_cfg = BT_CONTROLLER_INIT_CONFIG_DEFAULT();
    ESP_ERROR_CHECK(esp_bt_controller_init(&bt_cfg));
    ESP_ERROR_CHECK(esp_bt_controller_enable(ESP_BT_MODE_BLE));

    ESP_ERROR_CHECK(esp_bluedroid_init());
    ESP_ERROR_CHECK(esp_bluedroid_enable());

    ESP_ERROR_CHECK(esp_ble_gatts_register_callback(gatts_event_handler));
    ESP_ERROR_CHECK(esp_ble_gatts_app_register(0));
    ble_notify_init();

    return ESP_OK;
}

static void start_advertising(void)
{
    ESP_ERROR_CHECK(esp_ble_gap_start_advertising(&adv_params));
}

static void ble_send_response(esp_gatt_if_t gatts_if, uint16_t conn_id, uint16_t trans_id,
                              uint16_t handle, const uint8_t *data, uint16_t len)
{
    esp_gatt_rsp_t rsp;
    memset(&rsp, 0, sizeof(rsp));
    rsp.attr_value.handle = handle;
    rsp.attr_value.len = len;
    if (data && len > 0)
        memcpy(rsp.attr_value.value, data, len);

    esp_err_t ret = esp_ble_gatts_send_response(gatts_if, conn_id, trans_id, ESP_GATT_OK, &rsp);
    if (ret != ESP_OK)
        ESP_LOGE(TAG, "Failed to send response: 0x%x", ret);
}

static void ble_send_config_json(esp_gatt_if_t gatts_if, uint16_t conn_id,
                                 uint16_t trans_id, size_t offset)
{
    char *json = ble_config_build_read_json(&repo);
    if (!json)
        return;

    size_t json_len = strlen(json);
    size_t to_copy = 0;

    if (offset < json_len)
    {
        to_copy = json_len - offset;
        if (to_copy > ESP_GATT_MAX_ATTR_LEN)
            to_copy = ESP_GATT_MAX_ATTR_LEN;
    }

    ble_send_response(gatts_if, conn_id, trans_id, handle_table[IDX_CHAR_VAL_CONFIG],
                      (const uint8_t *)(json + offset), to_copy);

    cJSON_free(json);
}

static void gatts_event_handler(esp_gatts_cb_event_t event, esp_gatt_if_t gatts_if,
                                esp_ble_gatts_cb_param_t *param)
{
    switch (event)
    {

    case ESP_GATTS_CONNECT_EVT:
        ESP_LOGI(TAG, "ESP_GATTS_CONNECT_EVT:%i", param->connect.conn_id);
        ble_notify_set_connection(gatts_if, param->connect.conn_id);
        break;

    case ESP_GATTS_REG_EVT:
        gatts_if_global = gatts_if;
        ESP_LOGI(TAG, "GATT app registered, creating attribute table");
        if (esp_ble_gatts_create_attr_tab(gatt_db, gatts_if, GATT_IDX_NB, 0) != ESP_OK)
            ESP_LOGE(TAG, "Failed to create GATT attribute table");
        break;

    case ESP_GATTS_CREAT_ATTR_TAB_EVT:
        if (param->add_attr_tab.status != ESP_GATT_OK)
        {
            ESP_LOGE(TAG, "Create attr table failed, status %d", param->add_attr_tab.status);
            break;
        }

        memcpy(handle_table, param->add_attr_tab.handles, sizeof(uint16_t) * GATT_IDX_NB);
        ESP_LOGI(TAG, "Attribute table created, starting service");
        // Status characteristic value handle
        ble_notify_set_char_handle(handle_table[IDX_CHAR_VAL_STATUS]);
        esp_ble_gatts_start_service(handle_table[IDX_SVC]);
        start_advertising();
        break;

    case ESP_GATTS_WRITE_EVT:
    {
        uint16_t handle = param->write.handle;
        ESP_LOGI(TAG, "WRITE EVENT: handle=%i, len=%i", handle, param->write.len);

        if (handle == handle_table[IDX_CHAR_CFG_STATUS] && param->write.len == 2)
        {
            uint16_t descr_value = param->write.value[1] << 8 | param->write.value[0];
            if (descr_value == 0x0001)
            {
                ESP_LOGI(TAG, "Enabling Notifications.");
                ble_notify_set_enabled(true);
            }
            else
            {
                ESP_LOGI(TAG, "Disabling Notifications.");
                ble_notify_set_enabled(false);
            }
        }
        if (handle == handle_table[IDX_CHAR_VAL_CONFIG])
        {
            if (ble_config_handle_write(&repo, (char *)param->write.value, param->write.len) == ESP_OK)
            {

                const char *ssid     = config_repo_get_ssid(&repo);
                const char *password = config_repo_get_password(&repo);

                if (ssid && password)
                {
                    if (!provisioning_service_connect_wifi(ssid, password))
                    {
                        ESP_LOGW(TAG, "Provisioning busy, ignoring new credentials");
                    }
                }
            }
        }

        if (param->write.need_rsp)
            ble_send_response(gatts_if, param->write.conn_id, param->write.trans_id,
                              handle, param->write.value, param->write.len);
        break;
    }

    case ESP_GATTS_READ_EVT:
    {
        uint16_t handle = param->read.handle;
        ESP_LOGI(TAG, "READ EVENT: handle=%i, offset=%i", handle, param->read.offset);

        if (handle == handle_table[IDX_CHAR_VAL_CONFIG])
            ble_send_config_json(gatts_if, param->read.conn_id, param->read.trans_id,
                                 param->read.offset);
        break;
    }

    case ESP_GATTS_MTU_EVT:
    {

        ESP_LOGI(TAG, "MTU Negotiation Init: %d", param->mtu.mtu);

        break;
    }

    case ESP_GATTS_CONF_EVT:

    {
        ESP_LOGI(TAG, "MTU Negotiation Client Confirmed: %d", param->conf.status);
        break;
    }

    case ESP_GATTS_RESPONSE_EVT:
    {
        ESP_LOGI(TAG, "ESP_GATTS_RESPONSE_EVT:%i", param->rsp.status);
        break;
    }

    default:
        ESP_LOGW(TAG, "Unhandled Event: %i", event);
        break;
    }
}