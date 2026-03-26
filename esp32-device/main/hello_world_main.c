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
#include "img_converters.h"

#include <sys/stat.h>
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
#include "blob_detect.h"
#include "cnn_inference.h"

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

#if CONFIG_TRAINING_MODE
static char s_blob_dir[40];
static int  s_blob_idx = 0;
#endif

// Draw a 2-pixel thick coloured rectangle on an RGB888 buffer (in-place)
static void draw_rect_rgb888(uint8_t *img, uint16_t iw, uint16_t ih,
                              uint16_t x, uint16_t y, uint16_t w, uint16_t h,
                              uint8_t r, uint8_t g, uint8_t b)
{
    uint16_t x2 = (x + w < iw) ? x + w : iw;
    uint16_t y2 = (y + h < ih) ? y + h : ih;
    for (int t = 0; t < 2; t++) {
        for (uint16_t px = x; px < x2; px++) {
            if (y + t < ih)    { uint32_t o = ((y + t) * iw + px) * 3;    img[o]=r; img[o+1]=g; img[o+2]=b; }
            if (y2-1-t < ih)   { uint32_t o = ((y2-1-t) * iw + px) * 3;  img[o]=r; img[o+1]=g; img[o+2]=b; }
        }
        for (uint16_t py = y; py < y2; py++) {
            if (x + t < iw)    { uint32_t o = (py * iw + x + t) * 3;     img[o]=r; img[o+1]=g; img[o+2]=b; }
            if (x2-1-t < iw)   { uint32_t o = (py * iw + x2-1-t) * 3;   img[o]=r; img[o+1]=g; img[o+2]=b; }
        }
    }
}

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
    bool is_provisioned = false;

#if CONFIG_SKIP_PROVISIONING
    ESP_LOGW("MAIN", "SKIP_PROVISIONING=y — BLE and serial provisioning bypassed");
    is_provisioned = true;
#else
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

        // Initialise provisioning service (also initialises secure NVS)
        provisioning_service_init();

        // Check if device already has an API key
        char api_key_buf[65] = {0};
        is_provisioned = (provisioning_service_load_api_key(api_key_buf, sizeof(api_key_buf)) == ESP_OK);

        if (!is_provisioned)
        {
            ble_service_init();
            ESP_LOGI("BLE", "BLE Initialized");
        }
        else
        {
            ESP_LOGI("BLE", "Already provisioned — BLE skipped");
        }
    }

    if (!is_provisioned)
    {
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
    }
#endif // CONFIG_SKIP_PROVISIONING

    // No provision command — proceed with normal sensing boot
#ifdef SAVE_TO_SD
    if (mount_sdcard() != ESP_OK)
    {
        sd_fail_mount = true;
        ESP_LOGE("SD_CARD", "Failed to Mount SD Card");
    }
#if CONFIG_TRAINING_MODE
    else {
        mkdir("/sdcard/blobs", 0777);
        snprintf(s_blob_dir, sizeof(s_blob_dir), "/sdcard/blobs/%d", boot_count);
        mkdir(s_blob_dir, 0777);
        ESP_LOGI("TRAIN", "Saving blob crops to %s", s_blob_dir);
    }
#endif
#endif

    ESP_LOGI("PSRAM", "Free: %u bytes",
             heap_caps_get_free_size(MALLOC_CAP_SPIRAM));

    boot_count++;

    ESP_ERROR_CHECK(init_camera_x(PIXFORMAT_RGB565, FRAMESIZE_320X320));

    for (int i = 0; i < 3; i++)
    {
        esp_camera_fb_return(esp_camera_fb_get());
    }

    ESP_ERROR_CHECK(cnn_inference_init());

    static const blob_cfg_t blob_cfg = BLOB_CFG_DEFAULT;

    while (1)
    {
        camera_fb_t *fb = esp_camera_fb_get();
        IF_CAM_FB_NULL(fb);
        LOG_CAM_FRAME_DETAILS(fb);

        uint16_t img_w = fb->width, img_h = fb->height;

        // Convert RGB565 → RGB888 in PSRAM for blob detection and annotation
        uint8_t *rgb888 = heap_caps_malloc((size_t)img_w * img_h * 3, MALLOC_CAP_SPIRAM);
        bool converted = rgb888 && fmt2rgb888(fb->buf, fb->len, PIXFORMAT_RGB565, rgb888);

        // Raw frame no longer needed
        esp_camera_fb_return(fb);

        if (converted)
        {
            blob_t blobs[BLOB_MAX];
            int n = blob_detect_rgb888(rgb888, img_w, img_h, &blob_cfg, blobs, BLOB_MAX);
            ESP_LOGI("APP", "Blobs found: %d", n);

            for (int i = 0; i < n; i++) {
                ESP_LOGI("APP", "  blob[%d] x=%u y=%u w=%u h=%u",
                         i, blobs[i].x, blobs[i].y, blobs[i].w, blobs[i].h);
                draw_rect_rgb888(rgb888, img_w, img_h,
                                 blobs[i].x, blobs[i].y,
                                 blobs[i].w, blobs[i].h,
                                 255, 0, 0);

#if CONFIG_TRAINING_MODE
                {
                    uint16_t bw = blobs[i].w, bh = blobs[i].h;
                    uint8_t *blob_buf = heap_caps_malloc((size_t)bw * bh * 3, MALLOC_CAP_SPIRAM);
                    if (blob_buf) {
                        for (uint16_t row = 0; row < bh; row++) {
                            uint32_t src = ((uint32_t)(blobs[i].y + row) * img_w + blobs[i].x) * 3;
                            memcpy(blob_buf + (size_t)row * bw * 3, rgb888 + src, (size_t)bw * 3);
                        }
                        uint8_t *blob_jpg = NULL;
                        size_t   blob_jpg_len = 0;
                        if (fmt2jpg(blob_buf, (size_t)bw * bh * 3, bw, bh,
                                    PIXFORMAT_RGB888, 85, &blob_jpg, &blob_jpg_len)) {
                            char blob_path[64];
                            snprintf(blob_path, sizeof(blob_path), "%s/b%04d.jpg",
                                     s_blob_dir, s_blob_idx++);
                            save_to_sd(blob_path, blob_jpg, blob_jpg_len);
                            free(blob_jpg);
                        }
                        free(blob_buf);
                    } else {
                        ESP_LOGW("TRAIN", "blob[%d]: no memory for crop", i);
                    }
                }
#endif

                // Replicate training SquarePad: center blob in a white square,
                // then nearest-neighbour scale to CNN input size.
                uint16_t bw = blobs[i].w, bh = blobs[i].h;
                uint16_t sq = (bw > bh) ? bw : bh;
                uint16_t x_off = (sq - bw) / 2;
                uint16_t y_off = (sq - bh) / 2;

                uint8_t *sq_buf = heap_caps_malloc((size_t)sq * sq * 3, MALLOC_CAP_SPIRAM);
                if (!sq_buf) {
                    ESP_LOGE("CNN", "blob[%d]: no memory for square buf", i);
                    continue;
                }
                memset(sq_buf, 255, (size_t)sq * sq * 3); // white fill

                for (uint16_t row = 0; row < bh; row++) {
                    uint32_t src = ((uint32_t)(blobs[i].y + row) * img_w + blobs[i].x) * 3;
                    uint32_t dst = ((uint32_t)(y_off + row) * sq + x_off) * 3;
                    memcpy(sq_buf + dst, rgb888 + src, (size_t)bw * 3);
                }

                uint8_t *patch = heap_caps_malloc(CNN_INPUT_W * CNN_INPUT_H * 3, MALLOC_CAP_SPIRAM);
                if (!patch) {
                    free(sq_buf);
                    ESP_LOGE("CNN", "blob[%d]: no memory for patch", i);
                    continue;
                }
                for (int py = 0; py < CNN_INPUT_H; py++) {
                    for (int px = 0; px < CNN_INPUT_W; px++) {
                        uint32_t so = ((uint32_t)(py * sq / CNN_INPUT_H) * sq +
                                                  (px * sq / CNN_INPUT_W)) * 3;
                        uint32_t do_ = ((uint32_t)py * CNN_INPUT_W + px) * 3;
                        patch[do_]     = sq_buf[so];
                        patch[do_ + 1] = sq_buf[so + 1];
                        patch[do_ + 2] = sq_buf[so + 2];
                    }
                }
                free(sq_buf);

                cnn_detection_t detections[4];
                size_t det_count = 0;
                esp_err_t cnn_ret = cnn_inference_run(patch, NULL, detections, 4, &det_count);
                free(patch);

                if (cnn_ret == ESP_OK) {
                    for (size_t d = 0; d < det_count; d++) {
                        ESP_LOGI("CNN", "blob[%d]: %s (%.2f)",
                                 i, detections[d].specie, detections[d].confidence);
                    }
                    if (det_count == 0) {
                        ESP_LOGI("CNN", "blob[%d]: no class above threshold", i);
                    }
                } else {
                    ESP_LOGE("CNN", "blob[%d]: inference failed (%s)", i, esp_err_to_name(cnn_ret));
                }
            }
        }
        else
        {
            ESP_LOGE("APP", "RGB565→RGB888 conversion failed");
        }

        // Encode annotated BGR888 → JPEG for SD storage
        uint8_t *jpg = NULL;
        size_t jpg_len = 0;
        if (rgb888) {
            fmt2jpg(rgb888, (size_t)img_w * img_h * 3, img_w, img_h,
                    PIXFORMAT_RGB888, 80, &jpg, &jpg_len);
        }
        free(rgb888);

        if (jpg) {
            sprintf(path, "/sdcard/%i.jpg", boot_count);
            ESP_ERROR_CHECK(save_to_sd(path, jpg, jpg_len));
            free(jpg);
        }

        multi_heap_info_t info;
        heap_caps_get_info(&info, MALLOC_CAP_SPIRAM);
        ESP_LOGI("MEM", "PSRAM total: %u, free: %u, largest block: %u", info.total_free_bytes, info.total_allocated_bytes, info.largest_free_block);

        multi_heap_info_t infoDram;
        heap_caps_get_info(&infoDram, MALLOC_CAP_8BIT);
        ESP_LOGI("MEM", "DRAM total: %u, free: %u, largest block: %u", infoDram.total_free_bytes, infoDram.total_allocated_bytes, infoDram.largest_free_block);

        ESP_LOGI("MAIN", "No provision command received — going to sleep");
        // esp_deep_sleep(30000000);
        // esp_deep_sleep(1000);
        vTaskDelay(pdMS_TO_TICKS(1000));
        boot_count++;
    }
}
