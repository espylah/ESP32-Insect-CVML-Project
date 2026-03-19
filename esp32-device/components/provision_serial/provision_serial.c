#include "provision_serial.h"
#include "provisioning_service.h"
#include "console_simple_init.h"
#include "esp_log.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include <stdio.h>
#include <string.h>

static const char *TAG = "PROVISION";

static volatile bool g_provision_mode_requested = false;

// "start_provision" — signals the device to hold in provision mode
static int cmd_start_provision(int argc, char **argv)
{
    g_provision_mode_requested = true;
    printf("PROVISION:STARTED\n");
    ESP_LOGI(TAG, "Provision mode activated");
    return 0;
}

// "wifi <SSID words...> <PASSWORD>"
// SSID may contain spaces; PASSWORD must not.
static int cmd_wifi(int argc, char **argv)
{
    if (argc < 3)
    {
        printf("Usage: wifi <SSID words...> <PASSWORD>\n");
        return 1;
    }

    // Reconstruct SSID from all middle tokens; last token is password
    char ssid[64] = {0};
    for (int i = 1; i <= argc - 2; i++)
    {
        if (i > 1)
            strncat(ssid, " ", sizeof(ssid) - strlen(ssid) - 1);
        strncat(ssid, argv[i], sizeof(ssid) - strlen(ssid) - 1);
    }
    const char *password = argv[argc - 1];

    if (!provisioning_service_connect_wifi(ssid, password))
    {
        printf("PROVISION:WIFI_BUSY\n");
        ESP_LOGW(TAG, "Provisioning service busy or not ready");
        return 1;
    }

    ESP_LOGI(TAG, "Wi-Fi credentials accepted: SSID=%s", ssid);
    return 0;
}

// "register <REGISTRATION_TOKEN>"
static int cmd_register(int argc, char **argv)
{
    if (argc != 2)
    {
        printf("Usage: register <REGISTRATION_TOKEN>\n");
        return 1;
    }

    if (!provisioning_service_register(argv[1]))
    {
        printf("PROVISION:REGISTER_BUSY\n");
        ESP_LOGW(TAG, "Not ready to register — Wi-Fi must be connected first");
        return 1;
    }

    ESP_LOGI(TAG, "Registration token accepted");
    return 0;
}

static void console_repl_task(void *arg)
{
    console_cmd_start();
    vTaskDelete(NULL);
}

void provision_serial_start(void)
{
    ESP_LOGI(TAG, "Starting provision console");

    ESP_ERROR_CHECK(console_cmd_init());

    const esp_console_cmd_t start_cmd = {
        .command = "start_provision",
        .help    = "Signal device to enter provision mode",
        .hint    = NULL,
        .func    = &cmd_start_provision,
    };
    esp_console_cmd_register(&start_cmd);

    const esp_console_cmd_t wifi_cmd = {
        .command = "wifi",
        .help    = "Connect to Wi-Fi: wifi <SSID words...> <PASSWORD>",
        .hint    = NULL,
        .func    = &cmd_wifi,
    };
    esp_console_cmd_register(&wifi_cmd);

    const esp_console_cmd_t reg_cmd = {
        .command = "register",
        .help    = "Register device: register <REGISTRATION_TOKEN>",
        .hint    = NULL,
        .func    = &cmd_register,
    };
    esp_console_cmd_register(&reg_cmd);

    ESP_ERROR_CHECK(console_cmd_all_register());

    xTaskCreate(console_repl_task, "console_repl", 4096, NULL, 5, NULL);

    vTaskDelay(pdMS_TO_TICKS(100));

    printf("PROVISION:READY\r\n");
    fflush(stdout);

    ESP_LOGI(TAG, "Provision console started");
}

bool provision_serial_is_provision_mode_requested(void)
{
    return g_provision_mode_requested;
}
