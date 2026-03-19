#pragma once
#include <stdint.h>
#include <stdbool.h>

#define PROV_QUEUE_LEN 2

typedef enum
{
    PROV_STATUS_IDLE = 0,
    PROV_STATUS_CONNECTING,
    PROV_STATUS_WIFI_CONNECTED,
    PROV_STATUS_REGISTERING,
    PROV_STATUS_SUCCESS,
    PROV_STATUS_FAILED
} prov_status_t;

// Initialize the provisioning service and start the background task.
void provisioning_service_init(void);

// Step 1: connect to Wi-Fi. Returns false if the service is busy or not initialised.
bool provisioning_service_connect_wifi(const char *ssid, const char *password);

// Step 2: register the device with the backend using the provisioning token.
// Must be called after PROV_STATUS_WIFI_CONNECTED is reached.
// Returns false if the service is not ready.
bool provisioning_service_register(const char *token);

// Get the current provisioning status.
prov_status_t provisioning_service_get_status(void);

// Reset status to IDLE to allow a fresh attempt.
void provisioning_service_reset(void);
