package com.github.games647.fastlogin.core.shared;

public interface PasswordGenerator<T> {

    String getRandomPassword(T player);
}
