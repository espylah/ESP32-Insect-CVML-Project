package com.jden.espylah.webapi.app.api;

import com.jden.espylah.webapi.app.RunMode;
import com.jden.espylah.webapi.app.api.devices.registration.dto.*;
import com.jden.espylah.webapi.app.api.exceptions.AppExceptionRuntime;
import com.jden.espylah.webapi.app.config.app.AppRegistrationConfig;
import com.jden.espylah.webapi.app.utils.TimeUtil;
import com.jden.espylah.webapi.db.models.Device;
import com.jden.espylah.webapi.db.models.DeviceRegistrationToken;
import com.jden.espylah.webapi.db.repos.DeviceRegistrationTokenRepo;
import com.jden.espylah.webapi.db.repos.DeviceRepo;
import jakarta.servlet.http.HttpSession;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Log4j2
@SpringBootTest
public class DeviceCreateGetTokenAndRegisterE2EIT extends BaseAppTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private DeviceRepo deviceRepo;

    @Autowired
    private DeviceRegistrationTokenRepo deviceRegistrationTokenRepo;

    @MockitoBean
    private TimeUtil timeUtil;

    @Autowired
    private AppRegistrationConfig appRegistrationConfig;

    @BeforeEach
    public void setup() {
        when(timeUtil.now()).thenReturn(Instant.parse("2026-03-15T19:33:00Z"));
    }

    public DeviceCreateGetTokenAndRegisterE2EIT(WebApplicationContext webApplicationContext) {
        super(webApplicationContext);
    }

    @Test
    @Sql(scripts = "/h2-init.sql")
    public void createDeviceGetTokenAndRegister() throws Exception {

        // Step 1: Login as alice
        ObjectNode loginNode = objectMapper.createObjectNode();
        loginNode.put("email", "alice@test.com");
        loginNode.put("password", "password");

        ResultActions loginResult = getMockMvc().perform(post("/api/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginNode)))
                .andExpect(status().is2xxSuccessful());

        HttpSession alicesSession = loginResult.andReturn().getRequest().getSession();

        // Step 2: Create device
        CreateDeviceRequest createDeviceRequest = new CreateDeviceRequest(
                "Alices Sensor", RunMode.DEFAULT, null, null,
                List.of(new SpecieTarget("APIS_MELLIFERA", 0.80))
        );

        MvcResult createResult = getMockMvc().perform(post("/api/devices/create")
                        .with(csrf())
                        .session((MockHttpSession) alicesSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDeviceRequest)))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        CreateDeviceResponse createDeviceResponse = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), CreateDeviceResponse.class);
        UUID deviceId = createDeviceResponse.deviceId();
        Assertions.assertNotNull(deviceId);

        // Step 3: Get registration token via new endpoint
        MvcResult tokenResult = getMockMvc().perform(post("/api/devices/{deviceId}/registration-token", deviceId)
                        .with(csrf())
                        .session((MockHttpSession) alicesSession))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        GetDeviceRegistrationCodeResponse tokenResponse = objectMapper.readValue(
                tokenResult.getResponse().getContentAsString(), GetDeviceRegistrationCodeResponse.class);

        String regCode = tokenResponse.code();
        Assertions.assertNotNull(regCode);
        Assertions.assertEquals(32, regCode.length());
        Assertions.assertEquals(
                Instant.parse("2026-03-15T19:33:00Z").getEpochSecond() + appRegistrationConfig.getRegistrationTokenExpirySeconds(),
                tokenResponse.expiresAt()
        );

        // Step 4: Register the device using the token
        RegisterDeviceRequest registerDeviceRequest = new RegisterDeviceRequest("B2:3C:4D:5E:6F:70", regCode);

        MvcResult registerResult = getMockMvc().perform(post("/device-api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDeviceRequest)))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        RegisterDeviceResponse registerDeviceResponse = objectMapper.readValue(
                registerResult.getResponse().getContentAsString(), RegisterDeviceResponse.class);

        Assertions.assertEquals(deviceId, registerDeviceResponse.deviceId());

        Device device = deviceRepo.findById(deviceId).orElseThrow();
        Assertions.assertEquals(Device.State.PROVISIONED, device.getState());

        List<DeviceRegistrationToken> tokens = deviceRegistrationTokenRepo.findByDevice(device);
        DeviceRegistrationToken usedToken = tokens.stream()
                .filter(t -> t.getToken().equals(regCode))
                .findFirst()
                .orElseThrow();
        Assertions.assertTrue(usedToken.getUsed());

        // Step 5: Token is single-use
        getMockMvc().perform(post("/device-api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDeviceRequest)))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string(AppExceptionRuntime.ErrorCode.REGISTRATION_TOKEN_ALREADY_USED.name()));

        // Step 6: Provisioned device can authenticate against the device API
        getMockMvc().perform(get("/device-api/configuration")
                        .header("X-API-KEY", registerDeviceResponse.apiToken())
                        .header("X-DEVICE-ID", registerDeviceResponse.deviceId().toString()))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @Sql(scripts = "/h2-init.sql")
    public void registrationTokenExpires() throws Exception {

        // Login as alice
        ObjectNode loginNode = objectMapper.createObjectNode();
        loginNode.put("email", "alice@test.com");
        loginNode.put("password", "password");

        ResultActions loginResult = getMockMvc().perform(post("/api/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginNode)))
                .andExpect(status().is2xxSuccessful());

        HttpSession alicesSession = loginResult.andReturn().getRequest().getSession();

        // Create device
        CreateDeviceRequest createDeviceRequest = new CreateDeviceRequest(
                "Expiry Test Sensor", RunMode.DEFAULT, null, null,
                List.of(new SpecieTarget("APIS_MELLIFERA", 0.80))
        );

        MvcResult createResult = getMockMvc().perform(post("/api/devices/create")
                        .with(csrf())
                        .session((MockHttpSession) alicesSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDeviceRequest)))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        UUID deviceId = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), CreateDeviceResponse.class).deviceId();

        // Get registration token at fixed time T
        MvcResult tokenResult = getMockMvc().perform(post("/api/devices/{deviceId}/registration-token", deviceId)
                        .with(csrf())
                        .session((MockHttpSession) alicesSession))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        String regCode = objectMapper.readValue(
                tokenResult.getResponse().getContentAsString(), GetDeviceRegistrationCodeResponse.class).code();

        // Advance time past expiry
        when(timeUtil.now()).thenReturn(
                Instant.parse("2026-03-15T19:33:00Z")
                        .plus(appRegistrationConfig.getRegistrationTokenExpirySeconds() + 1, ChronoUnit.SECONDS)
        );

        // Attempt registration with expired token — expect rejection
        getMockMvc().perform(post("/device-api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterDeviceRequest("C3:4D:5E:6F:70:81", regCode))))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string(AppExceptionRuntime.ErrorCode.REGISTRATION_TOKEN_EXPIRED.name()));
    }
}
