package com.jden.espylah.webapi.security.authentication;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

@Configuration
public class LoginFilterConfig {

    Logger logger = LoggerFactory.getLogger(AppUsernamePasswordAuthFilter.class);

    @Primary
    @Bean
    public AppUsernamePasswordAuthFilter appJsonUsernamePasswordAuthenticationFilter(AuthenticationConfiguration authenticationConfiguration) {
        AppUsernamePasswordAuthFilter f = new AppUsernamePasswordAuthFilter();
        f.setFilterProcessesUrl("/api/login");
        f.setAuthenticationSuccessHandler((request, response, authentication) -> {
            logger.info("{} Successfully authenticated", authentication == null ? "ANON" : authentication.getName());
            response.getWriter().write("OK");
            response.setStatus(200);
        });
        f.setAuthenticationFailureHandler((request, response, exception) -> {

            logger.warn("Authentication failure: {}", exception.getMessage());

            String code;
            String message;

            switch (exception) {
                case BadCredentialsException _ -> {
                    code = "BAD_CREDENTIALS";
                    message = "Invalid username or password";
                }
                case LockedException _ -> {
                    code = "ACCOUNT_LOCKED";
                    message = "Account is locked";
                }
                case AuthenticationServiceException _ -> {
                    code = "AUTHENTICATION_SERVICE_ERROR";
                    message = "Authentication service failure";
                }
                default -> {
                    code = "AUTHENTICATION_FAILED";
                    message = "Authentication failed";
                }
            }

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");

            String json = """
        {
            "error": "%s",
            "message": "%s"
        }
        """.formatted(code, message);

            response.getWriter().write(json);
        });

        f.setAuthenticationManager(authenticationConfiguration.getAuthenticationManager());

        return f;
    }

}
