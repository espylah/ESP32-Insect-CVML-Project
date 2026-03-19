package com.jden.espylah.webapi.security;

import com.jden.espylah.webapi.db.repos.DeviceRepo;
import com.jden.espylah.webapi.security.authentication.AppUsernamePasswordAuthFilter;
import com.jden.espylah.webapi.security.authentication.OtpLoginAuthFilter;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.sql.DataSource;

@Log4j2
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public UserDetailsManager userDetailsManager(DataSource dataSource,PasswordEncoder passwordEncoder) {
        return new JdbcUserDetailsManager(dataSource);
    }

    @Bean
    public SecurityFilterChain deviceApiSfc(HttpSecurity http, DeviceRepo deviceRepo, PasswordEncoder passwordEncoder) throws Exception {
        return http.securityContext(s -> s.requireExplicitSave(true))
                .securityMatcher("/device-api/**")
                .exceptionHandling(httpSecurityExceptionHandlingConfigurer -> {
                    httpSecurityExceptionHandlingConfigurer.authenticationEntryPoint((request, response, authException) -> {
                        response.setStatus(401);
                        response.getWriter().write("AUTHENTICATION_REQUIRED");
                    });
                    httpSecurityExceptionHandlingConfigurer.accessDeniedHandler((request, response, authException) -> {
                        log.warn(authException.getMessage());
                        response.setStatus(403);
                        response.getWriter().write("ACCESS_DENIED");
                    });
                })
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .addFilterBefore(new DeviceAuthenticationFilter(deviceRepo, passwordEncoder), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests.requestMatchers("/device-api/register").permitAll()
                        .anyRequest().hasRole("DEVICE"))
                .build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, UserDetailsManager userDetailsManager, AppUsernamePasswordAuthFilter jsonUsernamePasswordAuthenticationFilter, OtpLoginAuthFilter otpLoginAuthFilter) {

        log.info("Security Filter Chain Initialising.");
        return http.securityContext(s -> s.requireExplicitSave(false))
                .securityMatcher("/api/**")
                .exceptionHandling(httpSecurityExceptionHandlingConfigurer -> {
                    httpSecurityExceptionHandlingConfigurer.authenticationEntryPoint((request, response, authException) -> {
                        response.setStatus(401);
                        response.getWriter().write("AUTHENTICATION_REQUIRED");
                    });
                    httpSecurityExceptionHandlingConfigurer.accessDeniedHandler((request, response, authException) -> {
                        log.warn(authException.getMessage());
                        response.setStatus(403);
                        response.getWriter().write("ACCESS_DENIED");
                    });
                })
                .cors(AbstractHttpConfigurer::disable)
                .csrf(c -> {
                    c.spa();
                    c.ignoringRequestMatchers("/api/devices/register");
                })
                .userDetailsService(userDetailsManager)
                .addFilterBefore(jsonUsernamePasswordAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(otpLoginAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(authorizeHttpRequests -> {
                    authorizeHttpRequests.requestMatchers("/", "/api/csrf", "/api/signup", "/api/login", "/api/devices/register", "/api/otp/request", "/api/otp/login").permitAll();
                    authorizeHttpRequests.anyRequest().authenticated();
                })
                .build();
    }
}
