package com.jden.espylah.webapi.app.api.otp;

import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Log4j2
public class OtpLoginController {

    private final OtpLoginService otpLoginService;

    public OtpLoginController(OtpLoginService otpLoginService) {
        this.otpLoginService = otpLoginService;
    }

    @PostMapping("/api/otp/request")
    public OtpRequestResponse requestOtp(@RequestBody OtpRequest request) {
        log.info("OTP request received for: {}", request.email());
        return otpLoginService.requestToken(request.email());
    }

    public record OtpRequest(String email) {
    }
}