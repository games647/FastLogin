package com.github.games647.fastlogin.bukkit;

import com.github.games647.craftapi.model.skin.SkinProperty;
import com.github.games647.fastlogin.core.StoredProfile;
import com.github.games647.fastlogin.core.shared.LoginSession;

import java.util.Optional;

import org.apache.commons.lang.ArrayUtils;

/**
 * Represents a client connecting to the server.
 *
 * This session is invalid if the player disconnects or the login was successful
 */
public class BukkitLoginSession extends LoginSession {

    private final String serverId;
    private final byte[] verifyToken;

    private boolean verified;

    private SkinProperty skinProperty;

    public BukkitLoginSession(String username, String serverId, byte[] verifyToken, boolean registered
            , StoredProfile profile) {
        super(username, registered, profile);

        this.serverId = serverId;
        this.verifyToken = ArrayUtils.clone(verifyToken);
    }

    //available for BungeeCord
    public BukkitLoginSession(String username, boolean registered) {
        this(username, "", ArrayUtils.EMPTY_BYTE_ARRAY, registered, null);
    }

    //cracked player
    public BukkitLoginSession(String username, StoredProfile profile) {
        this(username, "", ArrayUtils.EMPTY_BYTE_ARRAY, false, profile);
    }

    /**
     * Gets the verify token the server sent to the client.
     *
     * Empty if it's a BungeeCord connection
     *
     * @return the verify token from the server
     */
    public byte[] getVerifyToken() {
        return ArrayUtils.clone(verifyToken);
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
