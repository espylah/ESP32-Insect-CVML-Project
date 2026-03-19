package com.jden.espylah.webapi.app.api.otp;

public record OtpRequestResponse(String token, long expiresAt) {
}