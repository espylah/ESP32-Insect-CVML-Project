package com.jden.espylah.webapi.app.api.devices.manage;

import com.jden.espylah.webapi.app.api.devices.registration.dto.CreateDeviceRequest;
import com.jden.espylah.webapi.app.api.devices.registration.dto.SpecieTarget;
import com.jden.espylah.webapi.db.models.Device;
import com.jden.espylah.webapi.db.models.SpeciesTarget;
import com.jden.espylah.webapi.db.repos.DeviceRepo;
import com.jden.espylah.webapi.db.repos.SpeciesRepo;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Component
public class DeviceManageServiceImpl implements DeviceManageService {

    private final DeviceRepo deviceRepo;
    private final SpeciesRepo speciesRepo;

    public DeviceManageServiceImpl(DeviceRepo deviceRepo, SpeciesRepo speciesRepo) {
        this.deviceRepo = deviceRepo;
        this.speciesRepo = speciesRepo;
    }

    private Device findOwned(Authentication authentication, UUID deviceId) {
        return deviceRepo.findById(deviceId)
                .filter(d -> d.getUser().getUsername().equals(authentication.getName()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Override
    @Transactional
    public boolean setEnabled(Authentication authentication, UUID deviceId, boolean enabled) {
        Device device = findOwned(authentication, deviceId);
        device.setEnabled(enabled);
        deviceRepo.save(device);
        log.info("Device {} enabled={}", deviceId, enabled);
        return device.isEnabled();
    }

    @Override
    @Transactional
    public void unprovision(Authentication authentication, UUID deviceId) {
        Device device = findOwned(authentication, deviceId);
        if (device.getState() != Device.State.PROVISIONED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Device is not provisioned");
        }
        device.setState(Device.State.UNPROVISIONED);
        device.setApiToken(null);
        device.setDeviceMac(null);
        device.setDeviceMacStr(null);
        deviceRepo.save(device);
        log.info("Device {} unprovisioned", deviceId);
    }

    @Override
    @Transactional
    public void updateDevice(Authentication authentication, UUID deviceId, CreateDeviceRequest request) {
        Device device = deviceRepo.findByIdAndFetchTree(deviceId)
                .filter(d -> d.getUser().getUsername().equals(authentication.getName()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        device.setName(request.name());
        device.setRunMode(request.runmode());

        Map<String, Double> requestedMap = request.targetSpecies().stream()
                .collect(Collectors.toMap(SpecieTarget::specie, SpecieTarget::threshold));

        // Remove targets no longer requested
        device.getSpeciesTargets().removeIf(st -> !requestedMap.containsKey(st.getId().getSpecies()));

        // Update thresholds on retained targets
        for (SpeciesTarget st : device.getSpeciesTargets()) {
            st.setThresholdDetect(requestedMap.get(st.getId().getSpecies()));
        }

        // Add genuinely new targets
        Set<String> existing = device.getSpeciesTargets().stream()
                .map(st -> st.getId().getSpecies())
                .collect(Collectors.toSet());
        for (SpecieTarget req : request.targetSpecies()) {
            if (!existing.contains(req.specie())) {
                SpeciesTarget st = new SpeciesTarget();
                st.setDevice(device);
                st.setTargetSpecies(speciesRepo.getReferenceById(req.specie()));
                st.setThresholdDetect(req.threshold());
                device.getSpeciesTargets().add(st);
            }
        }

        deviceRepo.save(device);
        log.info("Device {} updated", deviceId);
    }
}
