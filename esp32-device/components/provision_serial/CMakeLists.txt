idf_component_register(SRCS "provision_serial.c"
                        REQUIRES console_simple_init esp_common nvs_flash console freertos provisioning
                    INCLUDE_DIRS "include")
