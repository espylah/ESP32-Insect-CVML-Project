#include "esp_log.h"

#define SAVE_TO_SD true;

#define IF_CAM_FB_NULL(fb)                                \
    do                                                    \
    {                                                     \
        if (!(fb))                                        \
        {                                                 \
            ESP_LOGE("CAM", "FAILURE TO CAPTURE IMAGE!"); \
        }                                                 \
    } while (0)

#define LOG_CAM_FRAME_DETAILS(fb)                                                                      \
    do                                                                                                 \
    {                                                                                                  \
        ESP_LOGI("CAM", "GOT FRAME : %i %ix%i FORMAT:%i", fb->len, fb->width, fb->height, fb->format); \
    } while (0)
