package com.github.games647.fastlogin.core.shared;

import com.github.games647.fastlogin.core.PlayerProfile;

import java.util.UUID;

public abstract class LoginSession {

    private final String username;
    private final PlayerProfile profile;
    
    private UUID uuid;

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

    /**
     * Get the premium UUID of this player
     *
     * @return the premium UUID or null if not fetched
     */
    public synchronized UUID getUuid() {
        return uuid;
    }

    /**
     * Set the online UUID if it's fetched
     *
     * @param uuid premium UUID
     */
    public synchronized void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
}
