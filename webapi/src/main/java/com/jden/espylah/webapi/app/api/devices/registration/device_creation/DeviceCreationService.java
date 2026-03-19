package com.jden.espylah.webapi.app.api.devices.registration.device_creation;

import com.jden.espylah.webapi.app.api.devices.registration.dto.CreateDeviceRequest;
import org.springframework.security.core.Authentication;

import java.util.UUID;

public interface DeviceCreationService {
    UUID createDeviceForAuthenticatedUser(Authentication authentication, CreateDeviceRequest createDeviceRequest);
}
