package com.jden.espylah.webapi.app.api.devices;

import com.jden.espylah.webapi.app.RunMode;
import com.jden.espylah.webapi.app.api.devices.detail.DeviceDetail;
import com.jden.espylah.webapi.app.api.devices.detail.DeviceDetailService;
import com.jden.espylah.webapi.app.api.devices.list.DeviceFilter;
import com.jden.espylah.webapi.app.api.devices.list.DeviceListItem;
import com.jden.espylah.webapi.app.api.devices.list.DeviceListService;
import com.jden.espylah.webapi.app.api.devices.manage.DeviceManageService;
import com.jden.espylah.webapi.app.api.devices.registration.device_creation.DeviceCreationService;
import com.jden.espylah.webapi.app.api.devices.registration.dto.CreateDeviceRequest;
import com.jden.espylah.webapi.app.api.devices.registration.dto.CreateDeviceResponse;
import com.jden.espylah.webapi.app.api.devices.registration.dto.GetDeviceRegistrationCodeResponse;
import com.jden.espylah.webapi.app.device_api.device_registration.DeviceRegistrationTokenService;
import com.jden.espylah.webapi.app.device_api.device_registration.RegisterDeviceService;
import com.jden.espylah.webapi.db.models.Device;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Log4j2
@RestController
public class DeviceController {

    private final DeviceRegistrationTokenService deviceRegistrationTokenService;
    private final DeviceCreationService deviceCreationService;
    private final RegisterDeviceService deviceRegistrationService;
    private final DeviceListService deviceListService;
    private final DeviceDetailService deviceDetailService;
    private final DeviceManageService deviceManageService;

    public DeviceController(DeviceRegistrationTokenService deviceRegistrationTokenService, DeviceCreationService deviceCreationService, RegisterDeviceService deviceRegistrationService, DeviceListService deviceListService, DeviceDetailService deviceDetailService, DeviceManageService deviceManageService) {
        this.deviceRegistrationTokenService = deviceRegistrationTokenService;
        this.deviceCreationService = deviceCreationService;
        this.deviceRegistrationService = deviceRegistrationService;
        this.deviceListService = deviceListService;
        this.deviceDetailService = deviceDetailService;
        this.deviceManageService = deviceManageService;
    }

    @PostMapping("/api/devices/create")
    public CreateDeviceResponse createDevice(Authentication authentication, @RequestBody CreateDeviceRequest createDeviceRequest) {
        log.debug("Received request to create device");
        log.info("{} is creating a device : {}", authentication.getName(), createDeviceRequest);
        UUID newDevice = deviceCreationService.createDeviceForAuthenticatedUser(authentication, createDeviceRequest);
        return new CreateDeviceResponse(newDevice);
    }

    @PostMapping("/api/devices/registration-code")
    public GetDeviceRegistrationCodeResponse getRegistrationCode(Authentication authentication) {
        return deviceRegistrationTokenService.getDeviceRegistrationCode(authentication);
    }

    @PostMapping("/api/devices/{deviceId}/registration-token")
    public GetDeviceRegistrationCodeResponse getRegistrationTokenForDevice(Authentication authentication, @PathVariable UUID deviceId) {
        log.info("{}: Getting device registration code for {}", authentication.getName(), deviceId);
        return deviceRegistrationTokenService.getRegistrationTokenForDevice(authentication, deviceId);
    }

    @GetMapping("/api/devices")
    public Page<DeviceListItem> listDevices(
            Authentication authentication,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Device.State state,
            @RequestParam(required = false) RunMode runMode,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Boolean online,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return deviceListService.listDevices(authentication, new DeviceFilter(name, state, runMode, enabled, online), pageable);
    }

    @GetMapping("/api/devices/{deviceId}")
    public DeviceDetail getDevice(Authentication authentication, @PathVariable UUID deviceId) {
        return deviceDetailService.getDeviceDetail(authentication, deviceId);
    }

    @PatchMapping("/api/devices/{deviceId}/enabled")
    public ResponseEntity<Void> setEnabled(Authentication authentication, @PathVariable UUID deviceId, @RequestBody SetEnabledRequest request) {
        deviceManageService.setEnabled(authentication, deviceId, request.enabled());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/api/devices/{deviceId}")
    public ResponseEntity<Void> updateDevice(Authentication authentication, @PathVariable UUID deviceId, @RequestBody CreateDeviceRequest request) {
        deviceManageService.updateDevice(authentication, deviceId, request);
        return ResponseEntity.ok().build();
    }

    record SetEnabledRequest(boolean enabled) {
    }

}
