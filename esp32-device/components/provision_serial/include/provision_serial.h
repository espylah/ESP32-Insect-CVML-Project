#pragma once

#ifdef __cplusplus
extern "C" {
#endif

#include <stdbool.h>

// Start the serial console and register provisioning commands.
// Prints PROVISION:READY once the console is up.
void provision_serial_start(void);

// Returns true once the "start_provision" command has been received.
bool provision_serial_is_provision_mode_requested(void);

#ifdef __cplusplus
}
#endif
