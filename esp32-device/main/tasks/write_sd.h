#include "stdint.h"
#include "esp_err.h"

esp_err_t write_to_sd(char* path,uint8_t* data,size_t data_len);

esp_err_t mount_sdcard(void);