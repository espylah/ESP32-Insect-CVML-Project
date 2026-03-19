package com.jden.espylah.webapi.app.api.devices.registration.dto;

import lombok.Builder;

@Builder(setterPrefix = "with")
public record SpecieTarget(String specie,Double threshold) {
}
