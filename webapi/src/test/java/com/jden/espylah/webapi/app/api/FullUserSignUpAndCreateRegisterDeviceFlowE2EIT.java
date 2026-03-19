package com.jden.espylah.webapi.app.api;

import com.jden.espylah.webapi.app.RunMode;
import com.jden.espylah.webapi.app.api.devices.registration.dto.CreateDeviceRequest;
import com.jden.espylah.webapi.app.api.devices.registration.dto.CreateDeviceResponse;
import com.jden.espylah.webapi.app.api.devices.registration.dto.SpecieTarget;
import com.jden.espylah.webapi.app.api.devices.registration.dto.RegisterDeviceRequest;
import com.jden.espylah.webapi.app.api.devices.registration.dto.RegisterDeviceResponse;
import com.jden.espylah.webapi.app.api.exceptions.AppExceptionRuntime;
import com.jden.espylah.webapi.app.api.signup.UserSignUpController;
import com.jden.espylah.webapi.app.config.app.AppRegistrationConfig;
import com.jden.espylah.webapi.app.device_api.dtos.DeviceConfigResponse;
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
import org.springframework.http.HttpHeaders;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Log4j2
@SpringBootTest
public class FullUserSignUpAndCreateRegisterDeviceFlowE2EIT extends BaseAppTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private DeviceRepo deviceRepo;

    @Autowired
    private DeviceRegistrationTokenRepo deviceRegistrationTokenRepo;

    @MockitoBean
    private TimeUtil timeUtil;

    @Autowired
    private AppRegistrationConfig appRegistrationConfigImpl;

    @BeforeEach
    public void setup() {
        when(timeUtil.now()).thenReturn(Instant.parse("2026-03-15T19:33:00Z"));
    }

    public FullUserSignUpAndCreateRegisterDeviceFlowE2EIT(WebApplicationContext webApplicationContext) {
        super(webApplicationContext);
    }


    @Test
    @Sql(scripts = "/h2-init.sql")
    public void signUpAndProvisionDevice() throws Exception {

        UserSignUpController.SignupRequest signupRequest = new UserSignUpController.SignupRequest("bob@test.com", "password");
        String json = objectMapper.writeValueAsString(signupRequest);

        getMockMvc().perform(post("/api/signup")
                        .with(csrf())
                        .content(json)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string("OK"));


        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("email", "bob@test.com");
        objectNode.put("password", "password");

        getMockMvc().perform(post("/api/login")
                        .with(csrf())
                        .content(objectMapper.writeValueAsString(objectNode))
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                )
                .andExpect(status().is2xxSuccessful());

        ResultActions resultActions = getMockMvc().perform(post("/api/login")
                        .with(csrf())
                        .content(objectMapper.writeValueAsString(objectNode))
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                )
                .andExpect(status().is2xxSuccessful());

        HttpSession bobsSession = resultActions.andReturn().getRequest().getSession();

        CreateDeviceRequest createDeviceRequest = new CreateDeviceRequest("Bobs Device", RunMode.ALWAYS_ON, 20.01, -51.4, List.of(
                new SpecieTarget("VESPA_VELUTINA_NIGRITHORAX", 0.71),
                new SpecieTarget("VESPA_CABRO", 0.75)
        ));

        MvcResult mvcResult = getMockMvc().perform(post("/api/devices/create")
                        .with(csrf())
                        .session((MockHttpSession) bobsSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDeviceRequest)
                        ))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        CreateDeviceResponse createDeviceResponse = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), CreateDeviceResponse.class);
        UUID uuid = createDeviceResponse.deviceId();

        Assertions.assertTrue(deviceRepo.existsById(uuid));

        Device device = deviceRepo.findByIdAndFetchTree(uuid).orElseThrow();
        log.info(device);

        Assertions.assertEquals("Bobs Device", device.getName());
        Assertions.assertEquals(RunMode.ALWAYS_ON, device.getRunMode());
        Assertions.assertEquals(uuid, device.getId());
        Assertions.assertTrue(device.getSpeciesTargets().stream().map(it -> it.getTargetSpecies().getSpecies())
                .toList().containsAll(List.of("VESPA_VELUTINA_NIGRITHORAX", "VESPA_CABRO")));

        Assertions.assertEquals(0.71, device.getSpeciesTargets().stream()
                .filter(it -> it.getTargetSpecies().getSpecies().equals("VESPA_VELUTINA_NIGRITHORAX"))
                .findAny()
                .orElseThrow().getThresholdDetect());

        Assertions.assertEquals(0.75, device.getSpeciesTargets().stream()
                .filter(it -> it.getTargetSpecies().getSpecies().equals("VESPA_CABRO"))
                .findAny().orElseThrow().getThresholdDetect());

        List<DeviceRegistrationToken> byDevice = deviceRegistrationTokenRepo.findByDevice(device);
        DeviceRegistrationToken deviceRegistrationToken = byDevice.getFirst();
        Assertions.assertNotNull(deviceRegistrationToken);
        Assertions.assertNotNull(deviceRegistrationToken.getToken());
        Assertions.assertEquals(deviceRegistrationToken.getExpiresAt(), Instant.parse("2026-03-15T19:33:00Z").plus(appRegistrationConfigImpl.getRegistrationTokenExpirySeconds(), ChronoUnit.SECONDS));
        Assertions.assertEquals(false, deviceRegistrationToken.getUsed());

        String regToken = deviceRegistrationToken.getToken();

        RegisterDeviceRequest registerDeviceRequest = new RegisterDeviceRequest("A1:5F:1C:9B:B3:D6", regToken);
        MvcResult deviceRegisteredResult = getMockMvc().perform(post("/device-api/register")
                        .content(objectMapper.writeValueAsString(registerDeviceRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        String contentAsString = deviceRegisteredResult.getResponse().getContentAsString();
        RegisterDeviceResponse registerDeviceResponse = objectMapper.readValue(contentAsString, RegisterDeviceResponse.class);
        log.info(registerDeviceResponse);


        MvcResult deviceRegisteredResultTwiceUsed = getMockMvc().perform(post("/device-api/register")
                        .content(objectMapper.writeValueAsString(registerDeviceRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string(AppExceptionRuntime.ErrorCode.REGISTRATION_TOKEN_ALREADY_USED.name()))
                .andReturn();

        String response = deviceRegisteredResultTwiceUsed.getResponse().getContentAsString();
        log.info("Controller Response: {}", response);


        MvcResult deviceGetConfig = getMockMvc().perform(get("/device-api/configuration")
                        .header("X-API-KEY",registerDeviceResponse.apiToken())
                        .header("X-DEVICE-ID",registerDeviceResponse.deviceId().toString())
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        DeviceConfigResponse deviceConfigResponse = objectMapper.readValue(deviceGetConfig.getResponse().getContentAsString(), DeviceConfigResponse.class);
        log.info(deviceConfigResponse);


    }
}
