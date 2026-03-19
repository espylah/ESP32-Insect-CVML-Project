package com.jden.espylah.webapi.app.device_api;

import com.jden.espylah.webapi.app.api.devices.registration.dto.DeviceRegistrationResponse;
import com.jden.espylah.webapi.app.api.devices.registration.dto.RegisterDeviceRequest;
import com.jden.espylah.webapi.app.api.exceptions.AppExceptionRuntime;
import com.jden.espylah.webapi.app.device_api.device_config.DeviceConfigService;
import com.jden.espylah.webapi.app.device_api.device_registration.RegisterDeviceService;
import com.jden.espylah.webapi.app.device_api.dtos.DeviceConfigResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@Log4j2
public class DeviceApiController {

    private final RegisterDeviceService registerDeviceService;
    private final DeviceConfigService deviceConfigService;

    public DeviceApiController(RegisterDeviceService registerDeviceService, DeviceConfigService deviceConfigService) {
        this.registerDeviceService = registerDeviceService;
        this.deviceConfigService = deviceConfigService;
    }

    @PostMapping("/device-api/register")
    public DeviceRegistrationResponse registerDevice(@RequestBody RegisterDeviceRequest registerDeviceRequest) {
        log.debug("Received request to register device");
        log.info("Registering a device : {}", registerDeviceRequest);
        return registerDeviceService.registerDevice(registerDeviceRequest);
    }

    @GetMapping("/device-api/configuration")
    public DeviceConfigResponse getDeviceConfigurationDataForAuthenticatedDevice(Authentication authentication) {
        log.debug("Received request to get device configuration data");
        log.info("Getting device configuration data for authenticated device : {}", authentication.getName());
        return deviceConfigService.getDeviceConfigurationDataForAuthenticatedDevice(authentication);
    }

    @ExceptionHandler
    public ResponseEntity<String> handleException(AppExceptionRuntime e) {
        switch (e.getErrorCode()) {
            case REGISTRATION_TOKEN_EXPIRED, REGISTRATION_TOKEN_ALREADY_USED -> {
                log.info("Exception 4xx : {}", e.toString());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getErrorCode().name());
            }
            default -> {
                log.error("UNEXPECTED APPRUNTIME ERRORCODE:{}->{}", e.getErrorCode(), e.toString());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(HttpStatus.INTERNAL_SERVER_ERROR.name());
            }
        }
    }
}
