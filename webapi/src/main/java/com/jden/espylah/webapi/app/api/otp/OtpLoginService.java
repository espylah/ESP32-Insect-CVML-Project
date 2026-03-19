package com.jden.espylah.webapi.app.api.otp;

public interface OtpLoginService {
    OtpRequestResponse requestToken(String email);
}