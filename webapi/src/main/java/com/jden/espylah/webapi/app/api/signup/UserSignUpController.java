package com.jden.espylah.webapi.app.api.signup;

import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.*;

@RestController
@Log4j2
public class UserSignUpController {

    private final ISignupService signupService;

    public UserSignUpController(ISignupService signupService) {
        this.signupService = signupService;
    }

    @PostMapping("/api/signup")
    public String signUp(@RequestBody SignupRequest signupRequest) {
        log.info("Signup request received : {}", signupRequest.email);
        try {
            signupService.signup(signupRequest);
        } catch (Exception e) {
            log.debug("Signup suppressed exception for {}: {}", signupRequest.email, e.getMessage());
        }
        return "OK";
    }

    public record SignupRequest(String email, String password) {
    }
}
