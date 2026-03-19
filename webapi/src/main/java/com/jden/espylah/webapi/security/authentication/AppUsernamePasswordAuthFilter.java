package com.jden.espylah.webapi.security.authentication;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.stream.Collectors;

@Log4j2
public class AppUsernamePasswordAuthFilter extends UsernamePasswordAuthenticationFilter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String CACHED_REQUEST_BODY = "CACHED_REQUEST_BODY";

    @Override
    protected String obtainUsername(HttpServletRequest request) {
        return extractFieldCached(request, "email");
    }

    @Override
    protected String obtainPassword(HttpServletRequest request) {
        return extractFieldCached(request, "password");
    }


    private String extractFieldCached(HttpServletRequest request, String field) {
        try {
            String body = (String) request.getAttribute(CACHED_REQUEST_BODY);
            if (body == null) {
                body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
                request.setAttribute(CACHED_REQUEST_BODY, body);
            }
            return objectMapper.readTree(body).get(field).asString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
