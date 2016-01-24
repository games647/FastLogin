package com.github.games647.fastlogin.bukkit;

import com.comphenix.protocol.wrappers.WrappedSignedProperty;

import org.apache.commons.lang.ArrayUtils;

/**
 * Represents a client connecting to the server.
 *
 * This session is invalid if the player disconnects or the login was successful
 */
public class PlayerSession {

    private final String username;
    private final String serverId;
    private final byte[] verifyToken;

    private WrappedSignedProperty skinProperty;
    private boolean verified;

    public PlayerSession(String username, String serverId, byte[] verifyToken) {
        this.username = username;
        this.serverId = serverId;
        this.verifyToken = ArrayUtils.clone(verifyToken);
    }

    /**
     * Gets the random generated server id. This makes sure the request
     * sent from the client is just for this server.
     *
     * See this for details
     * http://www.sk89q.com/2011/09/minecraft-name-spoofing-exploit/
     *
     * @return random generated server id
     */
    public String getServerId() {
        return serverId;
    }

    /**
     * Gets the verify token the server sent to the client.
     *
     * @return the verify token from the server
     */
    public byte[] getVerifyToken() {
        return ArrayUtils.clone(verifyToken);
    }

    /**
     * Gets the username the player sent to the server
     *
     * @return the client sent username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Gets the premium skin of this player
     *
     * @return skin property or null if the player has no skin or is a cracked account
     */
    public synchronized WrappedSignedProperty getSkin() {
        return this.skinProperty;
    }

    /**
     * Sets the premium skin property which was retrieved by the session server
     *
     * @param skinProperty premium skin property
     */
    public synchronized void setSkin(WrappedSignedProperty skinProperty) {
        this.skinProperty = skinProperty;
    }

    /**
     * Sets whether the player has a premium (paid account) account
     * and valid session
     *
     * @param verified whether the player has valid session
     */
    public synchronized void setVerified(boolean verified) {
        this.verified = verified;
    }

    /**
     * Get whether the player has a premium (paid account) account
     * and valid session
     *
     * @return whether the player has a valid session
     */
    public synchronized boolean isVerified() {
        return verified;
    }
}
