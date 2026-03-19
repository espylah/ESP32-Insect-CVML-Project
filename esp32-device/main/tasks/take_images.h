#include <stdint.h>
#include "esp_camera.h"

typedef struct
{
    framesize_t grayscale;
    framesize_t small_grayscale;
    framesize_t rgb565;
    framesize_t jpeg;
} image_sizes_t;

typedef struct
{
    uint8_t *gs_lg;
    uint8_t *gs_sm;
    uint8_t *rgb565;
    uint8_t *jpeg;
} image_buffers_t;

/**
 * Capture images into pre-allocated buffers.
 * Buffers must be sized appropriately for the requested formats and sizes.
 */
esp_err_t take_images(const image_sizes_t *sizes,
                      image_buffers_t *buffers);

/**
 * Free all buffers in an image_buffers_t
 */
void free_image_buffers(image_buffers_t *b);

/**
 * Allocating buffers.
 */
esp_err_t allocate_image_buffers(const image_sizes_t *sizes,
                                 image_buffers_t *b);