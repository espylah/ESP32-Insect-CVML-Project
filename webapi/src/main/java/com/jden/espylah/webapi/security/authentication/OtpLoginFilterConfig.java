package com.jden.espylah.webapi.security.authentication;

import com.jden.espylah.webapi.app.utils.TimeUtil;
import com.jden.espylah.webapi.db.repos.LoginOneTimeTokenRepo;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.provisioning.UserDetailsManager;

@Configuration
public class OtpLoginFilterConfig {

    Logger logger = LoggerFactory.getLogger(OtpLoginAuthFilter.class);

    @Bean
    public OtpLoginAuthFilter otpLoginAuthFilter(LoginOneTimeTokenRepo tokenRepo, UserDetailsManager userDetailsManager, TimeUtil timeUtil) {
        var provider = new OtpAuthenticationProvider(tokenRepo, userDetailsManager, timeUtil);
        var filter = new OtpLoginAuthFilter(new ProviderManager(provider));

        filter.setAuthenticationSuccessHandler((request, response, authentication) -> {
            logger.info("{} OTP authenticated successfully", authentication.getName());
            response.getWriter().write("OK");
            response.setStatus(200);
        });

        filter.setAuthenticationFailureHandler((request, response, exception) -> {
            logger.warn("OTP authentication failure: {}", exception.getMessage());
            String code = exception instanceof BadCredentialsException ? "BAD_TOKEN" : "AUTHENTICATION_FAILED";
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("""
                    {"error": "%s", "message": "%s"}
                    """.formatted(code, exception.getMessage()));
        });

        return filter;
    }
}