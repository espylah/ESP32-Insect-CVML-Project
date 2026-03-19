package com.jden.espylah.webapi.security.authentication;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.stream.Collectors;

@Log4j2
public class OtpLoginAuthFilter extends AbstractAuthenticationProcessingFilter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public OtpLoginAuthFilter(AuthenticationManager authenticationManager) {
        super(PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/api/otp/login"), authenticationManager);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException, IOException {
        String body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        JsonNode tokenNode = objectMapper.readTree(body).get("token");
        if (tokenNode == null) {
            throw new BadCredentialsException("Missing token field");
        }
        String token = tokenNode.asString();
        log.debug("OTP login attempt");
        return getAuthenticationManager().authenticate(new OtpAuthenticationToken(token));
    }
}