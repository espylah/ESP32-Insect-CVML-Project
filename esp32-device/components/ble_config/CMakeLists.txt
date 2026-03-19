idf_component_register(
    SRCS "ble_notify.c" "ble_config.c" "ble_config_utils.c" "ble_config_server.c"
    INCLUDE_DIRS "."
    REQUIRES bt esp_common config_repo json provisioning
)