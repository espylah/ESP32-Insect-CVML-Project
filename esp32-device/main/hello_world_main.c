/*
 * SPDX-FileCopyrightText: 2010-2022 Espressif Systems (Shanghai) CO LTD
 *
 * SPDX-License-Identifier: CC0-1.0
 */

#include <stdio.h>
#include <inttypes.h>
#include "sdkconfig.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "esp_camera.h"
#include "esp_log.h"
#include "esp_log.h"
#include "esp_system.h"
#include "esp_camera.h"
#include "camera_pins.h"
#include "camera.h"
#include "esp_sleep.h"
#include "cv.h"
#include "tasks/take_images.h"
#include <string.h>
#include "esp_err.h"
#include "esp_check.h"
#include "tasks/write_sd.h"
#include "helpers.h"
#include "esp_wifi.h"

#include "esp_log.h"
#include "esp_bt.h"
#include "esp_gap_ble_api.h"
#include "esp_gatts_api.h"
#include "esp_bt_main.h"
#include "nvs_flash.h"
#include "nvs_sec_provider.h"
#include "ble_config_server.h"
#include "esp_system.h"
#include "esp_sleep.h"
#include "esp_mac.h"

#include "esp_err.h"
#include "esp_pm.h"

#include "provision_serial.h"
#include "provisioning_service.h"

#define EXAMPLE_MAX_CPU_FREQ_MHZ 80
#define EXAMPLE_MIN_CPU_FREQ_MHZ 40

static esp_pm_config_esp32_t pm_config = {
    .max_freq_mhz = EXAMPLE_MAX_CPU_FREQ_MHZ, // e.g. 80, 160, 240
    .min_freq_mhz = EXAMPLE_MIN_CPU_FREQ_MHZ, // e.g. 40
    .light_sleep_enable = false,              // --> MODEM SLEEP INSTEAD!
};

RTC_DATA_ATTR int boot_count = 0;

bool save_to_sd_enabled = SAVE_TO_SD;
bool sd_fail_mount = false;

static char path[32]; // fewer stack bytes

esp_err_t save_to_sd(char *path, uint8_t *data, size_t len)
{
    if (!sd_fail_mount && save_to_sd_enabled)
    {
        ESP_LOGI("SD", "Saving %i bytes to SD at :%s", len, path);
        return write_to_sd(path, data, len);
    }
    else if (!save_to_sd_enabled)
    {
        return ESP_OK;
    }
    else
    {
        return ESP_ERR_INVALID_STATE;
    }
}

void app_main(void)
{
    ESP_ERROR_CHECK(nvs_flash_init());

    // Init TCP/IP stack
    ESP_ERROR_CHECK(esp_netif_init());

    ESP_ERROR_CHECK(esp_netif_init());                // initialize TCP/IP stack
    ESP_ERROR_CHECK(esp_event_loop_create_default()); // event loop
    esp_netif_create_default_wifi_sta();              // default STA interface

    wifi_init_config_t cfg = WIFI_INIT_CONFIG_DEFAULT();
    ESP_ERROR_CHECK(esp_wifi_init(&cfg)); // initialize Wi-Fi driver
    ESP_ERROR_CHECK(esp_wifi_set_mode(WIFI_MODE_STA));
    ESP_ERROR_CHECK(esp_wifi_start()); // optional: start Wi-Fi task


    esp_reset_reason_t reason = esp_reset_reason();

    if (reason == ESP_RST_DEEPSLEEP)
    {
        ESP_LOGI("main", "Woke from deep sleep");
    }
    else
    {
        ESP_LOGI("main", "Cold boot / restart");

        uint8_t mac[6];
        esp_efuse_mac_get_default(mac);

        ESP_LOGI("MAC",
                 "%02X:%02X:%02X:%02X:%02X:%02X",
                 mac[0], mac[1], mac[2],
                 mac[3], mac[4], mac[5]);

        // Start provisioning service (creates queue + task)
        provisioning_service_init();

        // Start BLE GATT server
        ble_service_init();
        ESP_LOGI("BLE", "BLE Initialized");
    }

    // Start serial console — prints PROVISION:READY once ready
    provision_serial_start();

    // Wait up to 5 s for the provisioner to send "start_provision"
    const int PROVISION_WAIT_TICKS = 5000 / 100; // 5 s in 100 ms steps
    bool provision_mode = false;
    for (int i = 0; i < PROVISION_WAIT_TICKS; i++)
    {
        if (provision_serial_is_provision_mode_requested())
        {
            provision_mode = true;
            break;
        }
        vTaskDelay(pdMS_TO_TICKS(100));
    }

    if (provision_mode)
    {
        ESP_LOGI("MAIN", "Provision mode active — serial commands will drive provisioning");

        // The wifi and register serial commands feed the provisioning service
        // directly. Stay alive so the service task can complete.
        while (1)
        {
            vTaskDelay(pdMS_TO_TICKS(1000));
        }
    }

    // No provision command — proceed with normal sensing boot
#ifdef SAVE_TO_SD
    if (mount_sdcard() != ESP_OK)
    {
        sd_fail_mount = true;
        ESP_LOGE("SD_CARD", "Failed to Mount SD Card");
    }
#endif

    ESP_LOGI("PSRAM", "Free: %u bytes",
             heap_caps_get_free_size(MALLOC_CAP_SPIRAM));

    boot_count++;

    ESP_ERROR_CHECK(init_camera_x(PIXFORMAT_YUV422, FRAMESIZE_SVGA));

    for (int i = 0; i < 3; i++)
    {
        esp_camera_fb_return(esp_camera_fb_get());
    }

    camera_fb_t *rgb565 = esp_camera_fb_get();
    IF_CAM_FB_NULL(rgb565);
    LOG_CAM_FRAME_DETAILS(rgb565);

    uint8_t *out = NULL;
    size_t len = 0;

    frame2jpg(rgb565, 80, &out, &len);

    ESP_LOGI("APP", "JPEG Conversion completed : size:%i", len);

    esp_camera_fb_return(rgb565);

    sprintf(path, "/sdcard/%i.jpg", boot_count);
    ESP_ERROR_CHECK(save_to_sd(path, out, len));

    free(out);

    ESP_LOGI("APP", "Freed malloc for JPEG conversion.", len);

    multi_heap_info_t info;
    heap_caps_get_info(&info, MALLOC_CAP_SPIRAM);
    ESP_LOGI("MEM", "PSRAM total: %u, free: %u, largest block: %u", info.total_free_bytes, info.total_allocated_bytes, info.largest_free_block);

    multi_heap_info_t infoDram;
    heap_caps_get_info(&infoDram, MALLOC_CAP_8BIT);
    ESP_LOGI("MEM", "DRAM total: %u, free: %u, largest block: %u", infoDram.total_free_bytes, infoDram.total_allocated_bytes, infoDram.largest_free_block);

    ESP_LOGI("MAIN", "No provision command received — going to sleep");
    esp_deep_sleep(30000000);
}
