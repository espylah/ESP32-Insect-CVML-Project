package com.jden.espylah.webapi.app.device_api.device_registration;

import com.jden.espylah.webapi.app.api.devices.registration.dto.DeviceRegistrationResponse;
import com.jden.espylah.webapi.app.api.devices.registration.dto.RegisterDeviceRequest;

public interface RegisterDeviceService {
    DeviceRegistrationResponse registerDevice(RegisterDeviceRequest request);
}
