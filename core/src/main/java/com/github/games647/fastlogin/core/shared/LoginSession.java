package com.github.games647.fastlogin.core.shared;

import com.github.games647.fastlogin.core.PlayerProfile;

public abstract class LoginSession {

    private final String username;
    private final PlayerProfile profile;
    protected boolean registered;

    public LoginSession(String username, boolean registered, PlayerProfile profile) {
        this.username = username;
        this.registered = registered;
        this.profile = profile;
    }

    public String getUsername() {
        return username;
    }

    /**
     * This value is always false if we authenticate the player with a cracked authentication
     *
     * @return
     */
    public boolean needsRegistration() {
        return !registered;
    }

    public PlayerProfile getProfile() {
        return profile;
    }
}
