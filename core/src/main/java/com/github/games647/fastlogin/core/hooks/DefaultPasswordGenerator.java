package com.github.games647.fastlogin.core.hooks;

import java.security.SecureRandom;
import java.util.Random;
import java.util.stream.IntStream;

public class DefaultPasswordGenerator<P> implements PasswordGenerator<P> {

    private static final int PASSWORD_LENGTH = 8;
    private static final char[] PASSWORD_CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
            .toCharArray();

    private final Random random = new SecureRandom();

    @Override
    public String getRandomPassword(P player) {
        StringBuilder generatedPassword = new StringBuilder(8);
        IntStream.rangeClosed(1, PASSWORD_LENGTH)
                .map(i -> random.nextInt(PASSWORD_CHARACTERS.length - 1))
                .mapToObj(pos -> PASSWORD_CHARACTERS[pos])
                .forEach(generatedPassword::append);

        return generatedPassword.toString();
    }
}
