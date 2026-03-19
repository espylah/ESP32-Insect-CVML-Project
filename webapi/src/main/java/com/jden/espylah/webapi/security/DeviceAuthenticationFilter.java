package com.jden.espylah.webapi.security;


import com.jden.espylah.webapi.db.models.Device;
import com.jden.espylah.webapi.db.repos.DeviceRepo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Log4j2
public class DeviceAuthenticationFilter extends OncePerRequestFilter {

    private final DeviceRepo deviceRepo;
    private final PasswordEncoder passwordEncoder;

    public DeviceAuthenticationFilter(DeviceRepo deviceRepo, PasswordEncoder passwordEncoder) {
        this.deviceRepo = deviceRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.debug("Entering DeviceAuthenticationFilter");

        String deviceId = request.getHeader("X-DEVICE-ID");
        String apiKey = request.getHeader("X-API-KEY");

        if (deviceId != null && apiKey != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            log.debug("DeviceAuthenticationFilter: deviceId={}, apiKey={}", deviceId, "[REDACTED]");
            final UUID parsedDeviceId;
            try {
                parsedDeviceId = UUID.fromString(deviceId);
            } catch (IllegalArgumentException e) {
                throw new BadCredentialsException("Invalid device ID.");
            }
            Device device = deviceRepo.findById(parsedDeviceId).orElseThrow(() -> new BadCredentialsException("Device not found."));
            boolean apiMatch = passwordEncoder.matches(apiKey, device.getApiToken());
            if (!apiMatch) {
                throw new BadCredentialsException("Invalid API Token.");
            }
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(device.getId(), null, List.of(new SimpleGrantedAuthority("ROLE_DEVICE"))));
            log.debug("Authentication successful for Device: {}", deviceId);
        }

        filterChain.doFilter(request, response);
    }

}
