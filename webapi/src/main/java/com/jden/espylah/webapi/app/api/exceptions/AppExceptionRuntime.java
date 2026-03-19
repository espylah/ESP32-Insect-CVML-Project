package com.jden.espylah.webapi.app.api.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public abstract class AppExceptionRuntime extends RuntimeException {

    private final ErrorCode errorCode;
    private String errorMessage = "";

    public AppExceptionRuntime(ErrorCode errorCode) {
        super("An error occurred while trying to process the request.");
        this.errorMessage = "An error occurred while trying to process the request.";
        this.errorCode = errorCode;
    }

    public AppExceptionRuntime(ErrorCode errorCode, String errorMessage) {
        super(errorMessage);
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
    }

    public enum ErrorCode {
        DEVICE_NOT_FOUND,
        REGISTRATION_TOKEN_EXPIRED,
        REGISTRATION_TOKEN_ALREADY_USED
    }
}
