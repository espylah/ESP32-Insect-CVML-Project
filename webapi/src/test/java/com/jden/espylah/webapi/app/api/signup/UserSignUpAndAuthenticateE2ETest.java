package com.jden.espylah.webapi.app.api.signup;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.Objects;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class UserSignUpAndAuthenticateE2ETest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private UserDetailsManager userDetailsManager;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    public void signupAndLogin() throws Exception {

        MockMvc mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        UserSignUpController.SignupRequest signupRequest = new UserSignUpController.SignupRequest("bob@test.com", "password");
        String json = objectMapper.writeValueAsString(signupRequest);

        mockMvc
                .perform(post("/api/signup")
                        .with(csrf())
                        .content(json)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string("OK"));

        boolean b = userDetailsManager.userExists("bob@test.com");

        UserDetails userDetails = userDetailsManager.loadUserByUsername("bob@test.com");

        Assertions.assertTrue(userDetails.getAuthorities().stream().anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_USER")));
        Assertions.assertEquals("bob@test.com", userDetails.getUsername());
        Assertions.assertTrue(userDetails.isEnabled());


        Assertions.assertTrue(passwordEncoder.matches(signupRequest.password(), userDetails.getPassword()));


        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("email", userDetails.getUsername());
        objectNode.put("password", "password");

        mockMvc.perform(post("/api/login")
                        .with(csrf())
                        .content(objectMapper.writeValueAsString(objectNode))
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                ).andDo(print())
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(post("/api/login")
                        .with(csrf())
                        .content(objectMapper.writeValueAsString(objectNode))
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                ).andDo(print())
                .andExpect(status().is2xxSuccessful());

    }

    @Test
    @Sql(scripts = "/h2-init.sql")
    public void otpRequestAndLogin() throws Exception {

        MockMvc mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        ObjectNode otpRequest = objectMapper.createObjectNode();
        otpRequest.put("email", "alice@test.com");

        String otpResponseJson = mockMvc.perform(post("/api/otp/request")
                        .with(csrf())
                        .content(objectMapper.writeValueAsString(otpRequest))
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(otpResponseJson).get("token").asString();

        ObjectNode loginRequest = objectMapper.createObjectNode();
        loginRequest.put("token", token);

        mockMvc.perform(post("/api/otp/login")
                        .with(csrf())
                        .content(objectMapper.writeValueAsString(loginRequest))
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string("OK"));
    }
}