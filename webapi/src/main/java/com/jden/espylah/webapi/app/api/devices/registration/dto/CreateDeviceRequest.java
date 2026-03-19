package com.jden.espylah.webapi.app.api.devices.registration.dto;

import com.jden.espylah.webapi.app.RunMode;

import java.util.List;

public record CreateDeviceRequest(String name, RunMode runmode, Double latitude, Double longitude, List<SpecieTarget> targetSpecies) {
}
