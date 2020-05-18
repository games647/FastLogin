package com.github.games647.fastlogin.core;

import com.github.games647.craftapi.model.Profile;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

public class StoredProfile extends Profile {

    private long rowId;
    private final ReentrantLock saveLock = new ReentrantLock();

    private boolean premium;
    private String lastIp;
    private Instant lastLogin;

    public StoredProfile(long rowId, UUID uuid, String playerName, boolean premium, String lastIp, Instant lastLogin) {
        super(uuid, playerName);

        this.rowId = rowId;
        this.premium = premium;
        this.lastIp = lastIp;
        this.lastLogin = lastLogin;
    }

    public StoredProfile(UUID uuid, String playerName, boolean premium, String lastIp) {
        this(-1, uuid, playerName, premium, lastIp, Instant.now());
    }

    public ReentrantLock getSaveLock() {
        return saveLock;
    }

    public synchronized boolean isSaved() {
        return rowId >= 0;
    }

    public synchronized void setPlayerName(String playerName) {
        this.name = playerName;
    }

    public synchronized long getRowId() {
        return rowId;
    }

    public synchronized void setRowId(long generatedId) {
        this.rowId = generatedId;
    }

    // can be null
    public synchronized UUID getId() {
        return id;
    }

    public synchronized Optional<UUID> getOptId() {
        return Optional.ofNullable(id);
    }

    public synchronized void setId(UUID uniqueId) {
        this.id = uniqueId;
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

    public synchronized Instant getLastLogin() {
        return lastLogin;
    }

    public synchronized void setLastLogin(Instant lastLogin) {
        this.lastLogin = lastLogin;
    }

    @Override
    public synchronized String toString() {
        return this.getClass().getSimpleName() + '{' +
                "rowId=" + rowId +
                ", premium=" + premium +
                ", lastIp='" + lastIp + '\'' +
                ", lastLogin=" + lastLogin +
                "} " + super.toString();
    }
}
