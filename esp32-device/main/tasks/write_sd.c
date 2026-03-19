
#include "sdmmc_cmd.h"
#include "driver/sdmmc_host.h"
#include <stdio.h>
#include <inttypes.h>
#include "esp_log.h"
#include "esp_vfs_fat.h"

esp_err_t mount_sdcard(void)
{
    sdmmc_host_t host = SDMMC_HOST_DEFAULT();
    sdmmc_slot_config_t slot_config = SDMMC_SLOT_CONFIG_DEFAULT();

    // Override default pins with board pins:
    slot_config.width = 1; // 1‑bit mode
    slot_config.clk = 39;  // SD_CLK pin
    slot_config.cmd = 38;  // SD_CMD pin
    slot_config.d0 = 40;   // SD_D0 pin

    esp_vfs_fat_sdmmc_mount_config_t mount_config = {
        .format_if_mount_failed = false,
        .max_files = 5};

    sdmmc_card_t *card;
    esp_err_t ret = esp_vfs_fat_sdmmc_mount("/sdcard", &host, &slot_config, &mount_config, &card);
    if (ret != ESP_OK)
    {
        ESP_LOGE("SD", "Failed to mount card (%s)", esp_err_to_name(ret));
        return ret;
    }

    ESP_LOGI("SD", "SD card mounted successfully");
    return ESP_OK;
}

esp_err_t write_to_sd(char *path, uint8_t *data, size_t data_len)
{
    FILE *f = fopen(path, "wb");

    ESP_LOGI("SD", "Filename :%s", path);

    size_t remaining = data_len;
    uint8_t *ptr = data;
    const size_t chunk_size = 32 * 1024; // 32 KB per write

    while (remaining)
    {
        size_t w = remaining > chunk_size ? chunk_size : remaining;
        size_t written = fwrite(ptr, 1, w, f);
        if (written != w)
        {
            ESP_LOGE("SD", "Write error");
            return ESP_ERR_INVALID_SIZE;
            break;
        }
        ptr += w;
        remaining -= w;
    }

    ESP_LOGI("SD", "Saving to memory card.");
    if (0 != fclose(f))
    {
        return ESP_ERR_INVALID_STATE;
    }
    else
    {
        return ESP_OK;
    }
}
