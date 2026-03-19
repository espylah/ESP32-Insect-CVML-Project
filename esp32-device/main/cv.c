#include "cv.h"
#include "esp_log.h"

void downscale_grayscale(uint8_t *in,
                         uint16_t sw, uint16_t sh,
                         uint16_t tw, uint16_t th,
                         uint8_t *out)
{
    if (!in || !out)
        return;

    uint16_t x_step = sw / tw;
    uint16_t y_step = sh / th;

    for (uint16_t ty = 0; ty < th; ty++)
    {
        for (uint16_t tx = 0; tx < tw; tx++)
        {

            uint32_t sum = 0;
            uint32_t count = 0;

            uint16_t sx0 = tx * x_step;
            uint16_t sy0 = ty * y_step;

            for (uint16_t y = 0; y < y_step; y++)
            {
                uint32_t row = (sy0 + y) * sw;
                for (uint16_t x = 0; x < x_step; x++)
                {
                    sum += in[row + sx0 + x];
                    count++;
                }
            }

            out[ty * tw + tx] = sum / count;
        }
    }
}

static const char ASCII_LUT_10[10] = {
    ' ', '.', ':', '-', '=', '+', '*', '#', '%', '@'};

static inline char gray_to_ascii(uint8_t v)
{
    return ASCII_LUT_10[(v * 10) >> 8]; // 0–255 → 0–9
}

void print_ascii_grayscale_10(const uint8_t *img,
                              uint16_t w,
                              uint16_t h,
                              const char *tag)
{
    if (!img)
        return;

    for (uint16_t y = 0; y < h; y++)
    {

        char line[65]; // 64 + NUL

        for (uint16_t x = 0; x < w; x++)
        {
            uint8_t v = img[y * w + x];
            line[x] = gray_to_ascii(v);
        }

        line[w] = '\0';
        ESP_LOGI(tag, "%s", line);
    }
}

void log_average_rgb565_corrected(const uint8_t *buf, int width, int height)
{
    uint32_t r_sum = 0, g_sum = 0, b_sum = 0;
    int pixels = width * height;

    // Step 1: extract raw RGB565
    for (int i = 0; i < pixels * 2; i += 2)
    {
        uint16_t px = buf[i] | (buf[i + 1] << 8);
        uint8_t r5 = (px >> 11) & 0x1F;
        uint8_t g6 = (px >> 5) & 0x3F;
        uint8_t b5 = px & 0x1F;

        r_sum += r5;
        g_sum += g6;
        b_sum += b5;
    }

    float r_avg = (float)r_sum / pixels;
    float g_avg = (float)g_sum / pixels;
    float b_avg = (float)b_sum / pixels;

    // Step 2: convert to 8-bit RGB
    float R = r_avg * 255.0f / 31.0f;
    float G = g_avg * 255.0f / 63.0f;
    float B = b_avg * 255.0f / 31.0f;

    // Step 3: apply calibrated colour correction matrix
    // These coefficients were tuned to OV3660 raw RGB565 measurements
    float Rc = 1.05f * R - 0.10f * G - 0.05f * B;
    float Gc = -0.15f * R + 0.90f * G + 0.10f * B;
    float Bc = -0.05f * R + 0.10f * G + 1.05f * B;

    // Step 4: clamp to 0–255
    Rc = (Rc < 0) ? 0 : (Rc > 255) ? 255
                                   : Rc;
    Gc = (Gc < 0) ? 0 : (Gc > 255) ? 255
                                   : Gc;
    Bc = (Bc < 0) ? 0 : (Bc > 255) ? 255
                                   : Bc;

    uint8_t r8 = (uint8_t)Rc;
    uint8_t g8 = (uint8_t)Gc;
    uint8_t b8 = (uint8_t)Bc;

    // Step 5: log visually intuitive percentages
    float sum = r8 + g8 + b8;
    float r_percent = 100.0f * r8 / sum;
    float g_percent = 100.0f * g8 / sum;
    float b_percent = 100.0f * b8 / sum;

    ESP_LOGI("RGB", "Calibrated RGB8: %u,%u,%u | R%%=%.1f G%%=%.1f B%%=%.1f",
             r8, g8, b8, r_percent, g_percent, b_percent);
}
