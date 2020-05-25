package com.github.games647.fastlogin.core.shared;

import com.github.games647.fastlogin.core.StoredProfile;
import com.google.common.base.Objects;

import java.util.UUID;

public abstract class LoginSession {

    private final StoredProfile profile;

    private String requestUsername;
    private String username;
    private UUID uuid;

    protected boolean registered;

    public LoginSession(String requestUsername, boolean registered, StoredProfile profile) {
        this.requestUsername = requestUsername;
        this.username = requestUsername;

        this.registered = registered;
        this.profile = profile;
    }

    public String getRequestUsername() {
        return requestUsername;
    }

    public String getUsername() {
        return username;
    }

    public synchronized void setVerifiedUsername(String username) {
        this.username = username;
    }

    /**
     * @return This value is always false if we authenticate the player with a cracked authentication
     */
    public synchronized boolean needsRegistration() {
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

    @Override
    public synchronized String toString() {
        return Objects.toStringHelper(this)
                .add("profile", profile)
                .add("requestUsername", requestUsername)
                .add("username", username)
                .add("uuid", uuid)
                .add("registered", registered)
                .toString();
    }
}
