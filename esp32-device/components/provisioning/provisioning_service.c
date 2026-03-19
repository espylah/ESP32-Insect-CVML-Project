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

    xTaskCreate(provisioning_task, "provisioning_task", 4096, NULL, 5, NULL);
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

        esp_http_client_config_t config = {
            .url                         = REGISTER_DEVICE_URL,
            .method                      = HTTP_METHOD_POST,
            .timeout_ms                  = 5000,
            .skip_cert_common_name_check = true,
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
            // Read and log the response body with the API key redacted
            int content_len = esp_http_client_get_content_length(client);
            int buf_size = (content_len > 0 && content_len < 512) ? content_len + 1 : 512;
            char *body = malloc(buf_size);
            if (body)
            {
                int read_len = esp_http_client_read(client, body, buf_size - 1);
                if (read_len > 0)
                {
                    body[read_len] = '\0';
                    cJSON *json = cJSON_Parse(body);
                    if (json)
                    {
                        cJSON *api_token = cJSON_GetObjectItem(json, "apiToken");
                        if (api_token && cJSON_IsString(api_token))
                            cJSON_SetValuestring(api_token, "[REDACTED]");
                        char *sanitised = cJSON_PrintUnformatted(json);
                        if (sanitised)
                        {
                            ESP_LOGI(TAG, "Registration response: %s", sanitised);
                            free(sanitised);
                        }
                        cJSON_Delete(json);
                    }
                    else
                    {
                        ESP_LOGI(TAG, "Registration response: <unparseable>");
                    }
                }
                free(body);
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
