package com.jden.espylah.webapi.app.api.devices.registration.device_creation;

import com.jden.espylah.webapi.app.api.devices.registration.dto.CreateDeviceRequest;
import com.jden.espylah.webapi.app.api.devices.registration.dto.SpecieTarget;
import com.jden.espylah.webapi.app.config.app.AppRegistrationConfig;
import com.jden.espylah.webapi.app.utils.RandomUtil;
import com.jden.espylah.webapi.app.utils.TimeUtil;
import com.jden.espylah.webapi.db.models.Device;
import com.jden.espylah.webapi.db.models.DeviceRegistrationToken;
import com.jden.espylah.webapi.db.models.SpeciesTarget;
import com.jden.espylah.webapi.db.repos.DeviceRegistrationTokenRepo;
import com.jden.espylah.webapi.db.repos.DeviceRepo;
import com.jden.espylah.webapi.db.repos.SpeciesRepo;
import com.jden.espylah.webapi.db.repos.UserRepo;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.jden.espylah.webapi.db.models.Device.State.UNPROVISIONED;

@Log4j2
@Component
public class DeviceCreationServiceImpl implements DeviceCreationService {

    private final SpeciesRepo speciesRepo;
    private final UserRepo userRepo;
    private final DeviceRepo deviceRepo;
    private final RandomUtil randomUtil;
    private final DeviceRegistrationTokenRepo deviceRegistrationTokenRepo;
    private final TimeUtil timeUtil;
    private final AppRegistrationConfig registrationConfiguration;


    public DeviceCreationServiceImpl(SpeciesRepo speciesRepo, UserRepo userRepo, DeviceRepo deviceRepo, RandomUtil randomUtil, DeviceRegistrationTokenRepo deviceRegistrationTokenRepo, TimeUtil timeUtil, AppRegistrationConfig registrationConfiguration) {
        this.speciesRepo = speciesRepo;
        this.userRepo = userRepo;
        this.deviceRepo = deviceRepo;
        this.randomUtil = randomUtil;
        this.deviceRegistrationTokenRepo = deviceRegistrationTokenRepo;
        this.timeUtil = timeUtil;
        this.registrationConfiguration = registrationConfiguration;
    }

    /**
     *
     * @param authentication      The authentication passed via Spring Security.
     * @param createDeviceRequest The request details to create a device.
     * @return The created device UUID.
     */
    @Override
    @Transactional
    public UUID createDeviceForAuthenticatedUser(Authentication authentication, CreateDeviceRequest createDeviceRequest) {
        Device device = new Device();

        device.setName(createDeviceRequest.name());
        device.setRunMode(createDeviceRequest.runmode());
        device.setUser(userRepo.getReferenceById(authentication.getName()));
        device.setEnabled(true);
        device.setState(UNPROVISIONED);

        // must happen here
        Set<SpeciesTarget> targets = new HashSet<>();
        for (SpecieTarget specieTarget : createDeviceRequest.targetSpecies()) {
            SpeciesTarget st = new SpeciesTarget();
            st.setDevice(device); // must happen here
            st.setTargetSpecies(speciesRepo.getReferenceById(specieTarget.specie()));
            st.setThresholdDetect(specieTarget.threshold());
            targets.add(st);
        }

        device.setSpeciesTargets(targets);

        device = deviceRepo.save(device);

        log.info("Device created with id: {}", device.getId());

        DeviceRegistrationToken deviceRegistrationToken = new DeviceRegistrationToken();
        deviceRegistrationToken.setToken(randomUtil.generateRandomAlphaNumericString(32));
        deviceRegistrationToken.setDevice(device);
        deviceRegistrationToken.setUser(userRepo.getReferenceById(authentication.getName()));
        deviceRegistrationToken.setExpiresAt(timeUtil.now().plus(registrationConfiguration.getRegistrationTokenExpirySeconds(), ChronoUnit.SECONDS));
        deviceRegistrationToken.setUsed(false);
        deviceRegistrationToken = deviceRegistrationTokenRepo.save(deviceRegistrationToken);
        return device.getId();
    }
}
