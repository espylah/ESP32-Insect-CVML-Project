# components/config_nvs_repo/CMakeLists.txt

idf_component_register(
    SRCS "config_repo.c" "nvs_util.c"
    INCLUDE_DIRS "."
    REQUIRES nvs_flash nvs_sec_provider json
)