#include "../camera.h"
#include "../cv.h"
#include "take_images.h"
#include "esp_heap_caps.h"
#include <string.h>
#include "esp_err.h"
#include "esp_check.h"

static void framesize_to_resolution(framesize_t fs, int *w, int *h)
{
    switch (fs)
    {
    case FRAMESIZE_QQVGA:
        *w = 160;
        *h = 120;
        break;
    case FRAMESIZE_QVGA:
        *w = 320;
        *h = 240;
        break;
    case FRAMESIZE_VGA:
        *w = 640;
        *h = 480;
        break;
    case FRAMESIZE_SVGA:
        *w = 800;
        *h = 600;
        break;
    case FRAMESIZE_XGA:
        *w = 1024;
        *h = 768;
        break;
    case FRAMESIZE_SXGA:
        *w = 1280;
        *h = 1024;
        break;
    case FRAMESIZE_UXGA:
        *w = 1600;
        *h = 1200;
        break;
    default:
        *w = 0;
        *h = 0;
        break;
    }
}

size_t image_buffer_size(pixformat_t fmt, framesize_t fs)
{
    int w, h;
    framesize_to_resolution(fs, &w, &h);

    switch (fmt)
    {
    case PIXFORMAT_GRAYSCALE:
        return w * h;
    case PIXFORMAT_RGB565:
        return w * h * 2;
    case PIXFORMAT_JPEG:
        return w * h * 5; // worst-case allocation
    default:
        return 0;
    }
}

/**
 * Create buffers based on the pixformat and framesize.
 * assign pointer out_buf via heaps_caps_malloc.
 */
esp_err_t create_image_buffer(pixformat_t pixformat,
                              framesize_t framesize,
                              uint8_t **out_buf,
                              uint32_t caps)
{
    if (!out_buf)
    {
        return ESP_ERR_INVALID_ARG;
    }

    size_t size = image_buffer_size(pixformat, framesize);
    if (size == 0)
    {
        return ESP_ERR_INVALID_ARG;
    }

    uint8_t *buf = heap_caps_malloc(size, caps);
    if (!buf)
    {
        return ESP_ERR_NO_MEM;
    }

    *out_buf = buf;
    return ESP_OK;
}

esp_err_t allocate_image_buffers(const image_sizes_t *sizes,
                                 image_buffers_t *b)
{
    ESP_LOGI("IMG_BUFF_PSRAM", "Pre-Alloc Free: %u bytes",
             heap_caps_get_free_size(MALLOC_CAP_SPIRAM));

    memset(b, 0, sizeof(*b));

    ESP_RETURN_ON_ERROR(
        create_image_buffer(PIXFORMAT_GRAYSCALE, sizes->grayscale,
                            &b->gs_lg, MALLOC_CAP_SPIRAM),
        "IMG_BUF", "gs_lg alloc failed");

    ESP_RETURN_ON_ERROR(
        create_image_buffer(PIXFORMAT_GRAYSCALE, sizes->small_grayscale,
                            &b->gs_sm, MALLOC_CAP_SPIRAM),
        "IMG_BUF", "gs_sm alloc failed");

    ESP_RETURN_ON_ERROR(
        create_image_buffer(PIXFORMAT_RGB565, sizes->jpeg,
                            &b->rgb565, MALLOC_CAP_SPIRAM),
        "IMG_BUF", "rgb565 alloc failed");

    ESP_RETURN_ON_ERROR(
        create_image_buffer(PIXFORMAT_JPEG, sizes->jpeg,
                            &b->jpeg, MALLOC_CAP_SPIRAM),
        "IMG_BUF", "jpeg alloc failed");

    ESP_LOGI("IMG_BUFF", "Image Buffers Allocated!");

    ESP_LOGI("IMG_BUFF_PSRAM", "Post Alloc Free: %u bytes",
             heap_caps_get_free_size(MALLOC_CAP_SPIRAM));

    return ESP_OK;
}

void free_image_buffers(image_buffers_t *b)
{
    ESP_LOGI("IMG_BUFF_PSRAM", "Pre-Free Free: %u bytes",
             heap_caps_get_free_size(MALLOC_CAP_SPIRAM));

    free(b->gs_lg);
    free(b->gs_sm);
    free(b->rgb565);
    free(b->jpeg);
    memset(b, 0, sizeof(*b));

    ESP_LOGI("IMG_BUFF_PSRAM", "Image Buffers Freed!");

    ESP_LOGI("PSRAM", "Post-Free Free: %u bytes",
             heap_caps_get_free_size(MALLOC_CAP_SPIRAM));
}