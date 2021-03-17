package com.github.games647.fastlogin.bukkit.auth;

import com.github.games647.craftapi.model.skin.SkinProperty;
import com.github.games647.fastlogin.core.storage.StoredProfile;
import com.github.games647.fastlogin.core.auth.LoginSession;

import java.util.Optional;

/**
 * Represents a client connecting to the server.
 *
 * This session is invalid if the player disconnects or the login was successful
 */
public class BukkitLoginSession extends LoginSession {

    private static final byte[] EMPTY_ARRAY = {};

    private final String serverId;
    private final byte[] verifyToken;

    private boolean verified;

    private SkinProperty skinProperty;

    public BukkitLoginSession(String username, String serverId, byte[] verifyToken, boolean registered
            , StoredProfile profile) {
        super(username, registered, profile);

        this.serverId = serverId;
        this.verifyToken = verifyToken.clone();
    }

    // available for proxies
    public BukkitLoginSession(String username, boolean registered) {
        this(username, "", EMPTY_ARRAY, registered, null);
    }

    //cracked player
    public BukkitLoginSession(String username, StoredProfile profile) {
        this(username, "", EMPTY_ARRAY, false, profile);
    }

    //ProtocolSupport
    public BukkitLoginSession(String username, boolean registered, StoredProfile profile) {
        this(username, "", EMPTY_ARRAY, registered, profile);
    }

    /**
     * Gets the verify token the server sent to the client.
     *
     * Empty if it's a proxy connection
     *
     * @return the verify token from the server
     */
    public synchronized byte[] getVerifyToken() {
        return verifyToken.clone();
    }

    /**
     * @return premium skin if available
     */
    public synchronized Optional<SkinProperty> getSkin() {
        return Optional.ofNullable(skinProperty);
    }

    /**
     * Sets the premium skin property which was retrieved by the session server
     * @param skinProperty premium skin
     */
    public synchronized void setSkinProperty(SkinProperty skinProperty) {
        this.skinProperty = skinProperty;
    }

    /**
     * Sets whether the player has a premium (paid account) account and valid session
     *
     * @param verified whether the player has valid session
     */
    public synchronized void setVerified(boolean verified) {
        this.verified = verified;
    }

    /**
     * Get whether the player has a premium (paid account) account and valid session
     *
     * @return whether the player has a valid session
     */
    public synchronized boolean isVerified() {
        return verified;
    }
}
