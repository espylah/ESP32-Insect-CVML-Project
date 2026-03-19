package com.jden.espylah.webapi.app.utils;

import org.springframework.stereotype.Component;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

@Component
public class RandomUtilImpl implements RandomUtil {

    private final SecureRandom secureRandom = SecureRandom.getInstanceStrong();
    private static final String ALPHA_NUMERIC_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    public RandomUtilImpl() throws NoSuchAlgorithmException {
    }

    @Override
    public String generateRandomAlphaNumericString(int length) {

        byte[] randomBytes = new byte[length];

        if (length == 0) {
            throw new IllegalArgumentException("Length must be greater than 0");
        }

        char[] chars = ALPHA_NUMERIC_CHARACTERS.toCharArray();
        int charSetSize = chars.length;
        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int randomIndex = secureRandom.nextInt(charSetSize);
            sb.append(chars[randomIndex]);
        }

        return sb.toString();
    }
}
