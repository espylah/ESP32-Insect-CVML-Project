package com.jden.espylah.webapi.app.api.devices.detail;

import com.jden.espylah.webapi.app.RunMode;
import com.jden.espylah.webapi.app.api.devices.registration.dto.SpecieTarget;
import com.jden.espylah.webapi.db.models.Device;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DeviceDetail(
        UUID id,
        String name,
        RunMode runMode,
        Device.State state,
        boolean enabled,
        Instant createdAt,
        Instant lastSeenAt,
        List<SpecieTarget> targetSpecies
) {}
