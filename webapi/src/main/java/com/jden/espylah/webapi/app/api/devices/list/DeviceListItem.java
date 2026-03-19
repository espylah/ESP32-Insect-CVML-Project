package com.jden.espylah.webapi.app.api.devices.list;

import com.jden.espylah.webapi.app.RunMode;
import com.jden.espylah.webapi.db.models.Device;

import java.time.Instant;
import java.util.UUID;

public record DeviceListItem(
        UUID id,
        String name,
        RunMode runMode,
        Device.State state,
        boolean enabled,
        Instant createdAt,
        Instant lastSeenAt
) {
}
