package com.jden.espylah.webapi.app.api.devices.registration.dto;

import com.jden.espylah.webapi.app.RunMode;
import java.util.UUID;

public record DeviceRegistrationResponse(UUID deviceId, RunMode runMode, String apiToken) {
}
