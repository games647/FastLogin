package com.github.games647.fastlogin.core.hooks;

public interface PasswordGenerator<P> {

    String getRandomPassword(P player);
}
