package com.jden.espylah.webapi.app.device_api.device_config;

import com.jden.espylah.webapi.app.device_api.dtos.DeviceConfigResponse;
import org.springframework.security.core.Authentication;

public interface DeviceConfigService {
    DeviceConfigResponse getDeviceConfigurationDataForAuthenticatedDevice(Authentication authentication);
}
