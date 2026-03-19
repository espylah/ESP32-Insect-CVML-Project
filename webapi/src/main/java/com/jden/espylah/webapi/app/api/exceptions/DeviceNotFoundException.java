package com.jden.espylah.webapi.app.api.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;

import static com.jden.espylah.webapi.app.api.exceptions.AppExceptionRuntime.ErrorCode.DEVICE_NOT_FOUND;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceNotFoundException extends AppExceptionRuntime {
    public DeviceNotFoundException() {
        super(DEVICE_NOT_FOUND);
    }
}
