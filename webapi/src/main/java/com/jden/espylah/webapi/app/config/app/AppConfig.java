package com.jden.espylah.webapi.app.config.app;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Setter
public class AppConfig implements AppRegistrationConfig, AppInferenceConfig {

    @Value("${app.registration.token-expiry-seconds:600}")
    private Long registrationTokenExpirySeconds;

    @Value("${app.inference.use-default-thresholds:false}")
    private Boolean useDefaultThresholds;
}
