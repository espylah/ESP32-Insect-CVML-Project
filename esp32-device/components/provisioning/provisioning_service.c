#include "provisioning_service.h"
#include "esp_wifi.h"
#include "esp_http_client.h"
#include "esp_event.h"
#include "esp_netif.h"
#include "esp_log.h"
#include "esp_err.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/queue.h"
#include "freertos/semphr.h"
#include "freertos/event_groups.h"
#include "nvs_flash.h"
#include <string.h>
#include "ble_notify.h"
#include "esp_system.h"
#include <cJSON.h>
#include "esp_mac.h"
#include <strings.h>
#include "nvs_util.h"
#include "config_repo.h"

static const char *TAG = "PROV_SVC";

#if CONFIG_API_USE_HTTPS
#define CONFIG_API_PROTOCOL "https"
#else
#define CONFIG_API_PROTOCOL "http"
#endif

#define STRINGIFY2(x) #x
#define STRINGIFY(x) STRINGIFY2(x)

#define REGISTER_DEVICE_URL \
    CONFIG_API_PROTOCOL "://" CONFIG_API_HOST ":" STRINGIFY(CONFIG_API_PORT) CONFIG_DEVICE_REGISTER_PATH

#define DEVICE_CONFIG_URL \
    CONFIG_API_PROTOCOL "://" CONFIG_API_HOST ":" STRINGIFY(CONFIG_API_PORT) "/device-api/configuration"

// Internal message types
typedef struct { char ssid[64]; char password[64]; } prov_wifi_msg_t;
typedef struct { char token[64]; }                   prov_register_msg_t;

static QueueHandle_t      wifi_queue       = NULL;
static QueueHandle_t      register_queue   = NULL;
static SemaphoreHandle_t  status_mutex     = NULL;
static EventGroupHandle_t wifi_event_group = NULL;

#define WIFI_CONNECTED_BIT BIT0
#define WIFI_FAIL_BIT      BIT1

static prov_status_t current_status = PROV_STATUS_IDLE;

static void provisioning_task(void *arg);

static void wifi_event_handler(void *arg, esp_event_base_t base,
                                int32_t id, void *data)
{
    if (base == IP_EVENT && id == IP_EVENT_STA_GOT_IP)
        xEventGroupSetBits(wifi_event_group, WIFI_CONNECTED_BIT);
    else if (base == WIFI_EVENT && id == WIFI_EVENT_STA_DISCONNECTED)
        xEventGroupSetBits(wifi_event_group, WIFI_FAIL_BIT);
}

void provisioning_service_init(void)
{
    if (wifi_queue)
        return; // already initialised

    wifi_queue       = xQueueCreate(PROV_QUEUE_LEN, sizeof(prov_wifi_msg_t));
    register_queue   = xQueueCreate(PROV_QUEUE_LEN, sizeof(prov_register_msg_t));
    status_mutex     = xSemaphoreCreateMutex();
    wifi_event_group = xEventGroupCreate();

    esp_event_handler_register(WIFI_EVENT, WIFI_EVENT_STA_DISCONNECTED,
                                wifi_event_handler, NULL);
    esp_event_handler_register(IP_EVENT, IP_EVENT_STA_GOT_IP,
                                wifi_event_handler, NULL);

    xTaskCreate(provisioning_task, "provisioning_task", 8192, NULL, 5, NULL);
    ESP_LOGI(TAG, "Provisioning service initialised.");
}

bool provisioning_service_connect_wifi(const char *ssid, const char *password)
{
    if (!wifi_queue)
        return false;

    xSemaphoreTake(status_mutex, portMAX_DELAY);
    bool idle = (current_status == PROV_STATUS_IDLE   ||
                 current_status == PROV_STATUS_FAILED  ||
                 current_status == PROV_STATUS_SUCCESS);
    xSemaphoreGive(status_mutex);
    if (!idle)
        return false;

    prov_wifi_msg_t msg = {0};
    strncpy(msg.ssid,     ssid,     sizeof(msg.ssid)     - 1);
    strncpy(msg.password, password, sizeof(msg.password) - 1);
    return xQueueSend(wifi_queue, &msg, pdMS_TO_TICKS(100)) == pdPASS;
}

bool provisioning_service_register(const char *token)
{
    if (!register_queue)
        return false;

    xSemaphoreTake(status_mutex, portMAX_DELAY);
    bool ready = (current_status == PROV_STATUS_WIFI_CONNECTED);
    xSemaphoreGive(status_mutex);
    if (!ready)
        return false;

    prov_register_msg_t msg = {0};
    strncpy(msg.token, token, sizeof(msg.token) - 1);
    return xQueueSend(register_queue, &msg, pdMS_TO_TICKS(100)) == pdPASS;
}

prov_status_t provisioning_service_get_status(void)
{
    prov_status_t status;
    xSemaphoreTake(status_mutex, portMAX_DELAY);
    status = current_status;
    xSemaphoreGive(status_mutex);
    return status;
}

void provisioning_service_reset(void)
{
    xSemaphoreTake(status_mutex, portMAX_DELAY);
    current_status = PROV_STATUS_IDLE;
    xSemaphoreGive(status_mutex);
}

esp_err_t provisioning_service_load_api_key(char *out_buf, size_t max_len)
{
    return nvs_util_get_str_from_partition("nvs_usr", "device", "api_key",
                                           out_buf, max_len);
}

esp_err_t provisioning_service_load_device_id(char *out_buf, size_t max_len)
{
    return nvs_util_get_str_from_partition("nvs_usr", "device", "device_id",
                                           out_buf, max_len);
}

// Accumulates HTTP response body via HTTP_EVENT_ON_DATA
typedef struct { char *buf; int len; int cap; } http_buf_t;

static esp_err_t http_collect_body(esp_http_client_event_t *evt)
{
    if (evt->event_id != HTTP_EVENT_ON_DATA) return ESP_OK;
    http_buf_t *b = (http_buf_t *)evt->user_data;
    if (!b || b->len + (int)evt->data_len >= b->cap) return ESP_OK;
    memcpy(b->buf + b->len, evt->data, evt->data_len);
    b->len += evt->data_len;
    return ESP_OK;
}

static void provisioning_task(void *arg)
{
    prov_wifi_msg_t     wifi_msg;
    prov_register_msg_t reg_msg;

    while (1)
    {
        // --- Step 1: wait for Wi-Fi credentials ---
        if (xQueueReceive(wifi_queue, &wifi_msg, portMAX_DELAY) != pdPASS)
            continue;

        ESP_LOGI(TAG, "Connecting to Wi-Fi SSID=%s", wifi_msg.ssid);

        xSemaphoreTake(status_mutex, portMAX_DELAY);
        current_status = PROV_STATUS_CONNECTING;
        xSemaphoreGive(status_mutex);

        // Clear any stale event bits before attempting connection
        xEventGroupClearBits(wifi_event_group, WIFI_CONNECTED_BIT | WIFI_FAIL_BIT);

        wifi_config_t wifi_config = {0};
        strncpy((char *)wifi_config.sta.ssid,     wifi_msg.ssid,     sizeof(wifi_config.sta.ssid));
        strncpy((char *)wifi_config.sta.password,  wifi_msg.password, sizeof(wifi_config.sta.password));

        esp_wifi_disconnect();
        esp_wifi_set_mode(WIFI_MODE_STA);
        esp_wifi_set_config(ESP_IF_WIFI_STA, &wifi_config);
        esp_err_t err = esp_wifi_connect();

        bool wifi_ok = false;

        if (err == ESP_OK)
        {
            // Wait up to 10 s for IP address assignment
            EventBits_t bits = xEventGroupWaitBits(wifi_event_group,
                                                   WIFI_CONNECTED_BIT | WIFI_FAIL_BIT,
                                                   pdTRUE, pdFALSE,
                                                   pdMS_TO_TICKS(10000));
            wifi_ok = (bits & WIFI_CONNECTED_BIT) != 0;
        }
        else
        {
            ESP_LOGE(TAG, "esp_wifi_connect failed: 0x%x", err);
        }

        if (wifi_ok)
        {
            ESP_LOGI(TAG, "Wi-Fi connected");

            xSemaphoreTake(status_mutex, portMAX_DELAY);
            current_status = PROV_STATUS_WIFI_CONNECTED;
            xSemaphoreGive(status_mutex);

            printf("PROVISION:WIFI_CONNECTED\n");
            fflush(stdout);

            ble_notify_msg_t evt = { .event = BLE_EVT_WIFI_CONNECTED };
            ble_notify_send_event(&evt);
        }
        else
        {
            ESP_LOGW(TAG, "Wi-Fi connection failed");

            xSemaphoreTake(status_mutex, portMAX_DELAY);
            current_status = PROV_STATUS_FAILED;
            xSemaphoreGive(status_mutex);

            printf("PROVISION:WIFI_FAILED\n");
            fflush(stdout);

            ble_notify_msg_t evt = { .event = BLE_EVT_WIFI_FAILED };
            ble_notify_send_event(&evt);

            continue; // back to waiting for wifi credentials
        }

        // --- Step 2: wait for registration token ---
        if (xQueueReceive(register_queue, &reg_msg, portMAX_DELAY) != pdPASS)
            continue;

        ESP_LOGI(TAG, "Registering device with backend");

        xSemaphoreTake(status_mutex, portMAX_DELAY);
        current_status = PROV_STATUS_REGISTERING;
        xSemaphoreGive(status_mutex);

        ble_notify_msg_t evt = { .event = BLE_DEVICE_REGISTRATION_IN_PROGRESS };
        ble_notify_send_event(&evt);

        uint8_t mac[6] = {0};
        esp_base_mac_addr_get(mac);

        char mac_str[18];
        snprintf(mac_str, sizeof(mac_str), "%02X:%02X:%02X:%02X:%02X:%02X",
                 mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);

        char post_data[256];
        snprintf(post_data, sizeof(post_data),
                 "{\"mac\":\"%s\", \"registrationToken\":\"%s\"}", mac_str, reg_msg.token);

        char reg_body_buf[512] = {0};
        http_buf_t reg_buf = { .buf = reg_body_buf, .len = 0, .cap = sizeof(reg_body_buf) };

        esp_http_client_config_t config = {
            .url                         = REGISTER_DEVICE_URL,
            .method                      = HTTP_METHOD_POST,
            .timeout_ms                  = 5000,
            .skip_cert_common_name_check = true,
            .event_handler               = http_collect_body,
            .user_data                   = &reg_buf,
        };

        esp_http_client_handle_t client = esp_http_client_init(&config);
        esp_http_client_set_header(client, "Content-Type", "application/json");
        esp_http_client_set_post_field(client, post_data, strlen(post_data));

        esp_err_t resp = esp_http_client_perform(client);
        int http_status = (resp == ESP_OK) ? esp_http_client_get_status_code(client) : -1;
        ESP_LOGI(TAG, "POST result: esp_err=%s http_status=%d",
                 esp_err_to_name(resp), http_status);

        if (resp == ESP_OK && http_status >= 200 && http_status < 300)
        {
            reg_body_buf[reg_buf.len] = '\0';

            cJSON *json_raw = cJSON_Parse(reg_body_buf);
            if (json_raw)
            {
                cJSON *token_item = cJSON_GetObjectItem(json_raw, "apiToken");
                if (token_item && cJSON_IsString(token_item))
                {
                    esp_err_t nvs_err = nvs_util_set_str_from_partition(
                        "nvs_usr", "device", "api_key", token_item->valuestring);
                    if (nvs_err == ESP_OK) ESP_LOGI(TAG, "API key persisted to secure NVS");
                    else                   ESP_LOGE(TAG, "Failed to persist API key: 0x%x", nvs_err);
                }
                else { ESP_LOGE(TAG, "apiToken missing or not a string in response"); }

                cJSON *device_id_item = cJSON_GetObjectItem(json_raw, "deviceId");
                if (device_id_item && cJSON_IsString(device_id_item))
                {
                    esp_err_t id_err = nvs_util_set_str_from_partition(
                        "nvs_usr", "device", "device_id", device_id_item->valuestring);
                    if (id_err == ESP_OK) ESP_LOGI(TAG, "Device ID persisted to secure NVS");
                    else                  ESP_LOGE(TAG, "Failed to persist device ID: 0x%x", id_err);
                }
                else { ESP_LOGE(TAG, "deviceId missing or not a string in response"); }

                // Redact api token and log for diagnostics
                cJSON *api_tok = cJSON_GetObjectItem(json_raw, "apiToken");
                if (api_tok && cJSON_IsString(api_tok))
                    cJSON_SetValuestring(api_tok, "[REDACTED]");
                char *sanitised = cJSON_PrintUnformatted(json_raw);
                if (sanitised) { ESP_LOGI(TAG, "Registration response: %s", sanitised); free(sanitised); }
                cJSON_Delete(json_raw);
            }
            else { ESP_LOGI(TAG, "Registration response: <unparseable>"); }

            // Fetch backend configuration using the registered credentials
            char api_key_buf[65]   = {0};
            char device_id_buf[37] = {0};
            if (nvs_util_get_str_from_partition("nvs_usr", "device", "api_key",
                                                api_key_buf, sizeof(api_key_buf)) == ESP_OK &&
                nvs_util_get_str_from_partition("nvs_usr", "device", "device_id",
                                                device_id_buf, sizeof(device_id_buf)) == ESP_OK)
            {
                char cfg_body_buf[512] = {0};
                http_buf_t cfg_buf = { .buf = cfg_body_buf, .len = 0, .cap = sizeof(cfg_body_buf) };

                esp_http_client_config_t cfg_config = {
                    .url                         = DEVICE_CONFIG_URL,
                    .method                      = HTTP_METHOD_GET,
                    .timeout_ms                  = 5000,
                    .skip_cert_common_name_check = true,
                    .event_handler               = http_collect_body,
                    .user_data                   = &cfg_buf,
                };
                esp_http_client_handle_t cfg_client = esp_http_client_init(&cfg_config);
                esp_http_client_set_header(cfg_client, "X-API-KEY",   api_key_buf);
                esp_http_client_set_header(cfg_client, "X-DEVICE-ID", device_id_buf);

                esp_err_t cfg_resp   = esp_http_client_perform(cfg_client);
                int       cfg_status = (cfg_resp == ESP_OK)
                                       ? esp_http_client_get_status_code(cfg_client) : -1;

                if (cfg_resp == ESP_OK && cfg_status >= 200 && cfg_status < 300)
                {
                    cfg_body_buf[cfg_buf.len] = '\0';
                    cJSON *cfg_json = cJSON_Parse(cfg_body_buf);
                    if (cfg_json)
                    {
                        config_repo_t repo = {0};
                        config_repo_init(&repo);

                        cJSON *cfg_name = cJSON_GetObjectItem(cfg_json, "name");
                        if (cfg_name && cJSON_IsString(cfg_name))
                            config_repo_set_name(&repo, cfg_name->valuestring);

                        cJSON *cfg_rm = cJSON_GetObjectItem(cfg_json, "runMode");
                        if (cfg_rm && cJSON_IsString(cfg_rm))
                        {
                            const char *rm = cfg_rm->valuestring;
                            int rm_int = 0;
                            if      (strcmp(rm, "ALWAYS_ON")         == 0) rm_int = 1;
                            else if (strcmp(rm, "TRAINING_UPLOADER") == 0) rm_int = 2;
                            config_repo_set_run_mode(&repo, rm_int);
                        }

                        cJSON *cfg_targets = cJSON_GetObjectItem(cfg_json, "targets");
                        if (cJSON_IsArray(cfg_targets))
                            config_repo_set_targets(&repo, cfg_targets);

                        config_repo_deinit(&repo);
                        cJSON_Delete(cfg_json);
                        ESP_LOGI(TAG, "Device configuration stored to NVS");
                    }
                }
                else
                {
                    ESP_LOGE(TAG, "Failed to fetch device config: esp=%s http=%d",
                             esp_err_to_name(cfg_resp), cfg_status);
                }
                esp_http_client_cleanup(cfg_client);
            }

            xSemaphoreTake(status_mutex, portMAX_DELAY);
            current_status = PROV_STATUS_SUCCESS;
            xSemaphoreGive(status_mutex);

            printf("PROVISION:REGISTERED\n");
            fflush(stdout);

            evt.event = BLE_DEVICE_REGISTRATION_SUCCESS;
            ble_notify_send_event(&evt);
        }
        else
        {
            if (resp == ESP_OK)
                ESP_LOGE(TAG, "HTTP POST rejected: status=%d", http_status);
            else
                ESP_LOGE(TAG, "HTTP POST failed: %s", esp_err_to_name(resp));

            xSemaphoreTake(status_mutex, portMAX_DELAY);
            current_status = PROV_STATUS_FAILED;
            xSemaphoreGive(status_mutex);

            printf("PROVISION:REGISTRATION_FAILED\n");
            fflush(stdout);

            evt.event = BLE_DEVICE_REGISTRATION_FAILED;
            ble_notify_send_event(&evt);
        }

        esp_http_client_cleanup(client);
    }
}
