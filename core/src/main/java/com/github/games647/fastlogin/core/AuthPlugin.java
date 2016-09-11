package com.github.games647.fastlogin.core;

public interface AuthPlugin<T> {

    boolean forceLogin(T player);

    boolean forceRegister(T player, String password);

    boolean isRegistered(String playerName) throws Exception;
}
