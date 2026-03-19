package com.jden.espylah.webapi.app.api.devices.manage;

import com.jden.espylah.webapi.app.api.devices.registration.dto.CreateDeviceRequest;
import org.springframework.security.core.Authentication;

import java.util.UUID;

public interface DeviceManageService {
    boolean setEnabled(Authentication authentication, UUID deviceId, boolean enabled);
    void updateDevice(Authentication authentication, UUID deviceId, CreateDeviceRequest request);
}
