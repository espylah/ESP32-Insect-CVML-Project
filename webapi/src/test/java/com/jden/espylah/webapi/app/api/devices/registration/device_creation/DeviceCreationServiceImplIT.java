package com.jden.espylah.webapi.app.api.devices.registration.device_creation;

import com.jden.espylah.webapi.app.RunMode;
import com.jden.espylah.webapi.app.api.devices.registration.dto.CreateDeviceRequest;
import com.jden.espylah.webapi.app.api.devices.registration.dto.SpecieTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.UUID;

@SpringBootTest
class DeviceCreationServiceImplIT {

    @Autowired
    private DeviceCreationServiceImpl deviceCreationService;

    @BeforeEach
    void setUp() {
    }

    @Sql(scripts = "/h2-init.sql")
    @Test
    @WithMockUser("alice@test.com")
    public void createDeviceForAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CreateDeviceRequest createDeviceRequest = new CreateDeviceRequest("Shed", RunMode.ALWAYS_ON, 51.2, -1.54, List.of(
                SpecieTarget.builder().withSpecie("VESPA_VELUTINA_NIGRITHORAX").withThreshold(67.0).build()
        ));

        UUID deviceForAuthenticatedUser = deviceCreationService.createDeviceForAuthenticatedUser(authentication, createDeviceRequest);

    }
}