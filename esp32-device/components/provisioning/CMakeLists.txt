idf_component_register(
    SRCS "provisioning_service.c"
    INCLUDE_DIRS "."
    REQUIRES ble_config esp_common config_repo json esp_http_client esp_wifi esp_netif
)