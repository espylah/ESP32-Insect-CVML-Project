package com.jden.espylah.webapi.app.api;

import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
public class CsrfController {

    @GetMapping("/api/csrf")
    public String csrf(CsrfToken csrfToken, Authentication authentication) {
        log.info("{} Requesting CSRF token: {}", authentication == null ? "ANON" : authentication.getName(), csrfToken.getToken());
        return "OK";
    }
}
