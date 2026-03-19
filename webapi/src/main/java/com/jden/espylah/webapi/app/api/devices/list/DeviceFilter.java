package com.jden.espylah.webapi.app.api.devices.list;

import com.jden.espylah.webapi.app.RunMode;
import com.jden.espylah.webapi.db.models.Device;

public record DeviceFilter(
        String name,
        Device.State state,
        RunMode runMode,
        Boolean enabled,
        Boolean online
) {
}
