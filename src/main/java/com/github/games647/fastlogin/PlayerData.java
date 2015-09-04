package com.github.games647.fastlogin;

public class PlayerData {

    private final byte[] verifyToken;
    private final String username;

    public PlayerData(byte[] verifyToken, String username) {
        this.username = username;
        this.verifyToken = verifyToken;
    }

    public byte[] getVerifyToken() {
        return verifyToken;
    }

    public String getUsername() {
        return username;
    }
}
