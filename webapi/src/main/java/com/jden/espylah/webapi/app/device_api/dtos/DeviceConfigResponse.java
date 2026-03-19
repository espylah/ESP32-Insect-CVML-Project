package com.jden.espylah.webapi.app.device_api.dtos;

import com.jden.espylah.webapi.app.RunMode;
import com.jden.espylah.webapi.app.api.devices.registration.dto.SpecieTarget;

import java.util.List;
import java.util.UUID;

public record DeviceConfigResponse(String name, UUID id, RunMode runMode, List<SpecieTarget> targets) {
}
