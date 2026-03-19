package com.jden.espylah.webapi.app.device_api.device_registration;

import com.jden.espylah.webapi.app.api.devices.registration.dto.GetDeviceRegistrationCodeResponse;
import com.jden.espylah.webapi.app.config.app.AppRegistrationConfig;
import com.jden.espylah.webapi.app.utils.RandomUtil;
import com.jden.espylah.webapi.app.utils.TimeUtil;
import com.jden.espylah.webapi.db.models.Device;
import com.jden.espylah.webapi.db.models.DeviceRegistrationToken;
import com.jden.espylah.webapi.db.repos.DeviceRegistrationTokenRepo;
import com.jden.espylah.webapi.db.repos.DeviceRepo;
import com.jden.espylah.webapi.db.repos.UserRepo;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Log4j2
@Component
public class DeviceRegistrationTokenServiceImpl implements DeviceRegistrationTokenService {

    private final DeviceRegistrationTokenRepo registrationTokenRepo;
    private final RandomUtil randomUtil;
    private final TimeUtil timeUtil;
    private final UserRepo userRepo;
    private final DeviceRepo deviceRepo;
    private final AppRegistrationConfig registrationConfig;

    public DeviceRegistrationTokenServiceImpl(DeviceRegistrationTokenRepo registrationTokenRepo, RandomUtil randomUtil, TimeUtil timeUtil, UserRepo userRepo, DeviceRepo deviceRepo, AppRegistrationConfig registrationConfig) {
        this.registrationTokenRepo = registrationTokenRepo;
        this.randomUtil = randomUtil;
        this.timeUtil = timeUtil;
        this.userRepo = userRepo;
        this.deviceRepo = deviceRepo;
        this.registrationConfig = registrationConfig;
    }

    @Override
    public GetDeviceRegistrationCodeResponse getDeviceRegistrationCode(Authentication authentication) {
        DeviceRegistrationToken deviceRegistrationToken = new DeviceRegistrationToken();
        deviceRegistrationToken.setToken(randomUtil.generateRandomAlphaNumericString(16));
        deviceRegistrationToken.setExpiresAt(timeUtil.now().plus(30, ChronoUnit.MINUTES));
        deviceRegistrationToken.setUsed(false);
        deviceRegistrationToken.setUser(userRepo.getReferenceById(authentication.getName()));
        deviceRegistrationToken = registrationTokenRepo.save(deviceRegistrationToken);
        log.info("Device registration token generated: {}", deviceRegistrationToken);
        return new GetDeviceRegistrationCodeResponse(deviceRegistrationToken.getToken(), deviceRegistrationToken.getExpiresAt().getEpochSecond());
    }

    @Override
    public GetDeviceRegistrationCodeResponse getRegistrationTokenForDevice(Authentication authentication, UUID deviceId) {
        Device device = deviceRepo.findById(deviceId)
                .filter(d -> d.getUser().getUsername().equals(authentication.getName()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (device.getState() == Device.State.PROVISIONED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Device already provisioned");
        }

        DeviceRegistrationToken token = new DeviceRegistrationToken();
        token.setToken(randomUtil.generateRandomAlphaNumericString(32));
        token.setDevice(device);
        token.setUser(userRepo.getReferenceById(authentication.getName()));
        token.setExpiresAt(timeUtil.now().plus(registrationConfig.getRegistrationTokenExpirySeconds(), ChronoUnit.SECONDS));
        token.setUsed(false);
        token = registrationTokenRepo.save(token);

        log.info("Registration token generated for device {}", deviceId);
        return new GetDeviceRegistrationCodeResponse(token.getToken(), token.getExpiresAt().getEpochSecond());
    }
}

