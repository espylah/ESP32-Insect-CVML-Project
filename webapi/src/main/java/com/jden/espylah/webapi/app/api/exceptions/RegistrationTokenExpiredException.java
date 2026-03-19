package com.jden.espylah.webapi.app.api.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RegistrationTokenExpiredException extends AppExceptionRuntime {
    public RegistrationTokenExpiredException() {
        super(ErrorCode.REGISTRATION_TOKEN_EXPIRED);
    }
}
