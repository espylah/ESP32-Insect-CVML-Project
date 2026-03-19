package com.jden.espylah.webapi.app.api.exceptions;

public class RegistrationTokenUsedException extends AppExceptionRuntime {
    public RegistrationTokenUsedException() {
        super(ErrorCode.REGISTRATION_TOKEN_ALREADY_USED);
    }
}
