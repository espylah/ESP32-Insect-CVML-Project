#include "esp_err.h"
#include "esp_camera.h"

esp_err_t init_camera_x(pixformat_t pixformat, framesize_t framesize_t);

void yuv422_to_gray(const uint8_t *yuv,
                    uint8_t *gray,
                    int width,
                    int height);

void gray_to_yuv422(const uint8_t *gray,
                    uint8_t *yuv,
                    int width,
                    int height);