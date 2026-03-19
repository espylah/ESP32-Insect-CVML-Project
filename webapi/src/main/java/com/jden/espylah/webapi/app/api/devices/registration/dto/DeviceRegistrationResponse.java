package com.jden.espylah.webapi.app.api.devices.registration.dto;

import com.jden.espylah.webapi.app.RunMode;

public record DeviceRegistrationResponse(byte[] deviceId, RunMode runMode,String apiToken) {
}
