package com.github.games647.fastlogin.core.hooks;

import java.util.Random;

public class DefaultPasswordGenerator<P> implements PasswordGenerator<P> {

    private static final char[] PASSWORD_CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
            .toCharArray();
    private final Random random = new Random();

    @Override
    public String getRandomPassword(P player) {
        StringBuilder generatedPassword = new StringBuilder(8);
        for (int i = 1; i <= 8; i++) {
            generatedPassword.append(PASSWORD_CHARACTERS[random.nextInt(PASSWORD_CHARACTERS.length - 1)]);
        }

        return generatedPassword.toString();
    }
}
