package com.github.games647.fastlogin.core;

public class LoginSession {

    private final String username;
    private final boolean registered;
    private final PlayerProfile profile;

    public LoginSession(String username, boolean registered, PlayerProfile profile) {
        this.username = username;
        this.registered = registered;
        this.profile = profile;
    }

    public String getUsername() {
        return username;
    }

    public boolean needsRegistration() {
        return !registered;
    }

    public PlayerProfile getProfile() {
        return profile;
    }
}
