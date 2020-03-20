package com.github.games647.fastlogin.core.shared;

import com.github.games647.fastlogin.core.StoredProfile;

import java.util.UUID;

public abstract class LoginSession {

    private final String username;
    private final StoredProfile profile;
    
    private UUID uuid;

    protected boolean registered;

    protected boolean confirmationLogin;

    public LoginSession(String username, boolean registered, StoredProfile profile) {
        this.username = username;
        this.registered = registered;
        this.profile = profile;
    }

    public LoginSession(String username, StoredProfile profile) {
        this(username, true, profile);
        this.confirmationLogin = true;
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

    public StoredProfile getProfile() {
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

    public synchronized boolean isConfirmationPending() {
        return confirmationLogin;
    }

    @Override
    public synchronized String toString() {
        return this.getClass().getSimpleName() + '{' +
                "username='" + username + '\'' +
                ", profile=" + profile +
                ", uuid=" + uuid +
                ", registered=" + registered +
                '}';
    }
}
