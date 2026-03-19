package com.jden.espylah.webapi.app.device_api.device_config;

import com.jden.espylah.webapi.app.api.devices.registration.dto.SpecieTarget;
import com.jden.espylah.webapi.app.api.exceptions.AppExceptionRuntime;
import com.jden.espylah.webapi.app.device_api.dtos.DeviceConfigResponse;
import com.jden.espylah.webapi.db.models.Device;
import com.jden.espylah.webapi.db.repos.DeviceRepo;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Log4j2
@Component
public class DeviceConfigServiceImpl implements DeviceConfigService {

    private final DeviceRepo deviceRepo;

    public DeviceConfigServiceImpl(DeviceRepo deviceRepo) {
        this.deviceRepo = deviceRepo;
    }

    @Override
    public DeviceConfigResponse getDeviceConfigurationDataForAuthenticatedDevice(Authentication authentication) {
        Device device = deviceRepo.findByIdAndFetchTree(UUID.fromString(authentication.getName()))
                .orElseThrow(() -> new AppExceptionRuntime(AppExceptionRuntime.ErrorCode.DEVICE_NOT_FOUND, "Device not found.") {
                });

        return new DeviceConfigResponse(device.getName(), device.getId(), device.getRunMode(),
                device.getSpeciesTargets().stream()
                        .map(it -> SpecieTarget
                                .builder()
                                .withSpecie(it.getTargetSpecies().getSpecies())
                                .withThreshold(it.getThresholdDetect())
                                .build())
                        .toList());

    }
}
