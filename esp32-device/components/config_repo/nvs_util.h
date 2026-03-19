#pragma once

#include "esp_err.h"
#include "nvs.h"

// Initialize default NVS partition
esp_err_t nvs_util_init_default(void);

// Initialize secure NVS partition (must exist in partition table)
esp_err_t nvs_util_init_secure(const char *partition_name);

// Store string in a namespace in either default or secure NVS
esp_err_t nvs_util_set_str_from_partition(const char *partition_name, const char *namespace_name,
                                          const char *key, const char *value);

// Read string from a namespace in either default or secure NVS
esp_err_t nvs_util_get_str_from_partition(const char *partition_name, const char *namespace_name,
                                          const char *key, char *out_value, size_t max_len);

esp_err_t nvs_util_set_u32(const char *partition_name,
                      const char *namespace_name,
                      const char *key,
                      uint32_t value);

esp_err_t nvs_util_get_u32(const char *partition_name,
                      const char *namespace_name,
                      const char *key,
                      uint32_t *value);