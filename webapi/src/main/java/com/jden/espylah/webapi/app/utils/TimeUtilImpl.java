package com.jden.espylah.webapi.app.utils;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Log4j2
@Component
public class TimeUtilImpl implements TimeUtil {
    @Override
    public Instant now() {
        return Instant.now();
    }
}
