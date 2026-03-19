package com.jden.espylah.webapi.app.api.devices.detail;

import org.springframework.security.core.Authentication;

import java.util.UUID;

public interface DeviceDetailService {
    DeviceDetail getDeviceDetail(Authentication authentication, UUID deviceId);
}
