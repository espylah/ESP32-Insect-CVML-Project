package com.jden.espylah.webapi.app.api.devices.list;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

public interface DeviceListService {
    Page<DeviceListItem> listDevices(Authentication authentication, DeviceFilter filter, Pageable pageable);
}
