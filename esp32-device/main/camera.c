#include "camera_pins.h"
#include "esp_camera.h"
#include "esp_log.h"
#include "esp_heap_caps.h"
#include "camera_pins.h"
#include "esp_system.h"



esp_err_t init_camera_x(pixformat_t pixformat, framesize_t framesize_t)
{
    camera_config_t config;
    config.ledc_channel = LEDC_CHANNEL_0;
    config.ledc_timer = LEDC_TIMER_0;
    config.pin_d0 = Y2_GPIO_NUM;
    config.pin_d1 = Y3_GPIO_NUM;
    config.pin_d2 = Y4_GPIO_NUM;
    config.pin_d3 = Y5_GPIO_NUM;
    config.pin_d4 = Y6_GPIO_NUM;
    config.pin_d5 = Y7_GPIO_NUM;
    config.pin_d6 = Y8_GPIO_NUM;
    config.pin_d7 = Y9_GPIO_NUM;
    config.pin_xclk = XCLK_GPIO_NUM;
    config.pin_pclk = PCLK_GPIO_NUM;
    config.pin_vsync = VSYNC_GPIO_NUM;
    config.pin_href = HREF_GPIO_NUM;
    config.pin_sccb_sda = SIOD_GPIO_NUM;
    config.pin_sccb_scl = SIOC_GPIO_NUM;
    config.pin_pwdn = PWDN_GPIO_NUM;
    config.pin_reset = RESET_GPIO_NUM;
    config.xclk_freq_hz = 20000000;
    config.frame_size = framesize_t;
    config.pixel_format = pixformat;
    config.grab_mode = CAMERA_GRAB_WHEN_EMPTY;
    config.fb_location = CAMERA_FB_IN_PSRAM;
    config.jpeg_quality = 15;
    config.fb_count = 1;

    // camera init
    esp_err_t err = esp_camera_reconfigure(&config);
    if (err != ESP_OK)
    {
        ESP_LOGE("CAM", "Camera init failed with error 0x%x", err);
        return err;
    }

    sensor_t *s = esp_camera_sensor_get();
    camera_sensor_info_t *si = esp_camera_sensor_get_info(&s->id);


    ESP_LOGI("CAM", "ESP-CAMERA configured for %i:%i", pixformat, framesize_t);
    ESP_LOGI("CAM", "Model:%s ModelEnum:%i JPEGSupport:%i ", si->name, si->model, si->support_jpeg);


    return ESP_OK;
}

void yuv422_to_gray(const uint8_t *yuv,
                    uint8_t *gray,
                    int width,
                    int height)
{
    int pixels = width * height;
    int gi = 0;

    for (int i = 0; i < pixels * 2; i += 4) {
        gray[gi++] = yuv[i];     // Y0
        gray[gi++] = yuv[i + 2]; // Y1
    }
}

void gray_to_yuv422(const uint8_t *gray,
                    uint8_t *yuv,
                    int width,
                    int height)
{
    int pixels = width * height;
    int gi = 0;

    for (int i = 0; i < pixels * 2; i += 4) {
        uint8_t y0 = gray[gi++];
        uint8_t y1 = gray[gi++];

        yuv[i]     = y0;   // Y0
        yuv[i + 1] = 128;  // U
        yuv[i + 2] = y1;   // Y1
        yuv[i + 3] = 128;  // V
    }
}
