package com.jden.espylah.webapi.app.device_api.device_registration;

import com.jden.espylah.webapi.app.api.exceptions.RegistrationTokenExpiredException;
import com.jden.espylah.webapi.app.api.exceptions.RegistrationTokenUsedException;
import com.jden.espylah.webapi.app.api.devices.registration.dto.DeviceRegistrationResponse;
import com.jden.espylah.webapi.app.api.devices.registration.dto.RegisterDeviceRequest;
import com.jden.espylah.webapi.app.utils.RandomUtil;
import com.jden.espylah.webapi.app.utils.TimeUtil;
import com.jden.espylah.webapi.db.models.Device;
import com.jden.espylah.webapi.db.models.DeviceRegistrationToken;
import com.jden.espylah.webapi.db.repos.DeviceRegistrationTokenRepo;
import com.jden.espylah.webapi.db.repos.DeviceRepo;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;

@Log4j2
@Component
public class RegisterDeviceServiceImpl implements RegisterDeviceService {

    private final DeviceRepo deviceRepo;
    private final TimeUtil timeUtil;
    private final RandomUtil randomUtil;
    private final DeviceRegistrationTokenRepo deviceRegistrationTokenRepo;
    private final PasswordEncoder passwordEncoder;

    public RegisterDeviceServiceImpl(DeviceRepo deviceRepo, TimeUtil timeUtil, RandomUtil randomUtil, DeviceRegistrationTokenRepo deviceRegistrationTokenRepo, PasswordEncoder passwordEncoder) {
        this.deviceRepo = deviceRepo;
        this.timeUtil = timeUtil;
        this.randomUtil = randomUtil;
        this.deviceRegistrationTokenRepo = deviceRegistrationTokenRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public DeviceRegistrationResponse registerDevice(RegisterDeviceRequest request) {

        DeviceRegistrationToken deviceRegistrationToken = deviceRegistrationTokenRepo.findByTokenFetchDevice(request.registrationToken());
        if (deviceRegistrationToken == null || deviceRegistrationToken.getExpiresAt().isBefore(timeUtil.now())) {
            throw new RegistrationTokenExpiredException();
        }
        if (deviceRegistrationToken.getUsed()) {
            throw new RegistrationTokenUsedException();
        }

        String apiKey = randomUtil.generateRandomAlphaNumericString(64);

        Device device = deviceRegistrationToken.getDevice();
        device.setState(Device.State.PROVISIONED);
        device.setDeviceMac(Long.parseLong(request.mac().replace(":", ""), 16));
        device.setApiToken(passwordEncoder.encode(apiKey));
        device.setDeviceMacStr(request.mac());
        device = deviceRepo.save(device);


        deviceRegistrationToken.setUsed(true);

        deviceRegistrationTokenRepo.save(deviceRegistrationToken);

        byte[] bytes = ByteBuffer.allocate(16)
                .putLong(device.getId().getMostSignificantBits())
                .putLong(device.getId().getLeastSignificantBits())
                .array();

        log.info("Device {}:[{}]  registered successfully", device.getId(),device.getDeviceMacStr());
        return new DeviceRegistrationResponse(bytes, device.getRunMode(), apiKey);
    }


}
