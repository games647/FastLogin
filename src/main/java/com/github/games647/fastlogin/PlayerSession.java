package com.github.games647.fastlogin;

/**
 * Represents a client connecting to the server.
 *
 * This session is invalid if the player disconnects or the login was successful
 */
public class PlayerSession {

    private final byte[] verifyToken;
    private final String username;
    private boolean verified;

    public PlayerSession(byte[] verifyToken, String username) {
        this.username = username;
        this.verifyToken = verifyToken;
    }

    /**
     * Gets the verify token the server sent to the client.
     *
     * @return the verify token from the server
     */
    public byte[] getVerifyToken() {
        return verifyToken;
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
