package com.jden.espylah.webapi.app.api.devices.registration.dto;

import java.util.UUID;

public record RegisterDeviceResponse(UUID deviceId,String apiToken) {
}
