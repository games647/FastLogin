package com.github.games647.fastlogin.core.hooks;

@FunctionalInterface
public interface PasswordGenerator<P> {

    String getRandomPassword(P player);
}
