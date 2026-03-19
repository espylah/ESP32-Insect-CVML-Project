package com.jden.espylah.webapi.app.api.devices.detail;

import com.jden.espylah.webapi.app.api.devices.registration.dto.SpecieTarget;
import com.jden.espylah.webapi.db.models.Device;
import com.jden.espylah.webapi.db.repos.DeviceRepo;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Component
public class DeviceDetailServiceImpl implements DeviceDetailService {

    private final DeviceRepo deviceRepo;

    public DeviceDetailServiceImpl(DeviceRepo deviceRepo) {
        this.deviceRepo = deviceRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public DeviceDetail getDeviceDetail(Authentication authentication, UUID deviceId) {
        Device device = deviceRepo.findByIdAndFetchTree(deviceId)
                .filter(d -> d.getUser().getUsername().equals(authentication.getName()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        List<SpecieTarget> targets = device.getSpeciesTargets().stream()
                .map(st -> new SpecieTarget(st.getTargetSpecies().getSpecies(), st.getThresholdDetect()))
                .toList();

        return new DeviceDetail(
                device.getId(),
                device.getName(),
                device.getRunMode(),
                device.getState(),
                device.isEnabled(),
                device.getCreatedAt(),
                device.getLastSeenAt(),
                targets
        );
    }
}
