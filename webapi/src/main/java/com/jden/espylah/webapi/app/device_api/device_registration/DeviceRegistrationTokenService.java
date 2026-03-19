package com.jden.espylah.webapi.app.device_api.device_registration;

import com.jden.espylah.webapi.app.api.devices.registration.dto.GetDeviceRegistrationCodeResponse;
import org.springframework.security.core.Authentication;

import java.util.UUID;

public interface DeviceRegistrationTokenService {
    GetDeviceRegistrationCodeResponse getDeviceRegistrationCode(Authentication authentication);
    GetDeviceRegistrationCodeResponse getRegistrationTokenForDevice(Authentication authentication, UUID deviceId);
}
