#include <stdint.h>

void downscale_grayscale(uint8_t *in,
                         uint16_t sw, uint16_t sh,
                         uint16_t tw, uint16_t th,
                         uint8_t *out);

void print_ascii_grayscale_10(const uint8_t *img,
                              uint16_t w,
                              uint16_t h,
                              const char *tag);

void log_average_rgb565_corrected(const uint8_t *buf, int width, int height);