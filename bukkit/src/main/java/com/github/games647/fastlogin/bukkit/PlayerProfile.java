package com.github.games647.fastlogin.bukkit;

import java.util.UUID;

public class PlayerProfile {

    private final String playerName;

    private long userId;

    private UUID uuid;
    private boolean premium;
    private String lastIp;
    private long lastLogin;

    public PlayerProfile(long userId, UUID uuid, String playerName, boolean premium
            , String lastIp, long lastLogin) {
        this.userId = userId;
        this.uuid = uuid;
        this.playerName = playerName;
        this.premium = premium;
        this.lastIp = lastIp;
        this.lastLogin = lastLogin;
    }

    public PlayerProfile(UUID uuid, String playerName, boolean premium, String lastIp) {
        this.userId = -1;

        this.uuid = uuid;
        this.playerName = playerName;
        this.premium = premium;
        this.lastIp = lastIp;
    }

    public String getPlayerName() {
        return playerName;
    }

    public synchronized long getUserId() {
        return userId;
    }

    protected synchronized void setUserId(long generatedId) {
        this.userId = generatedId;
    }

    public synchronized UUID getUuid() {
        return uuid;
    }

    public synchronized void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public synchronized boolean isPremium() {
        return premium;
    }

    public synchronized void setPremium(boolean premium) {
        this.premium = premium;
    }

    public synchronized String getLastIp() {
        return lastIp;
    }

    public synchronized void setLastIp(String lastIp) {
        this.lastIp = lastIp;
    }

    public synchronized long getLastLogin() {
        return lastLogin;
    }

    public synchronized void setLastLogin(long lastLogin) {
        this.lastLogin = lastLogin;
    }
}
