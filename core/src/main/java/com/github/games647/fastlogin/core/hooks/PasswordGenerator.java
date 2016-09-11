package com.github.games647.fastlogin.core.hooks;

public interface PasswordGenerator<T> {

    String getRandomPassword(T player);
}
