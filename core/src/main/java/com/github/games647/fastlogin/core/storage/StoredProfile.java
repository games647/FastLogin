/*
 * SPDX-License-Identifier: MIT
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2024 games647 and contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.github.games647.fastlogin.core.storage;

import com.github.games647.craftapi.model.Profile;
import com.github.games647.fastlogin.core.shared.FloodgateState;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

public class StoredProfile extends Profile {

    private long rowId;
    private final ReentrantLock saveLock = new ReentrantLock();

    private boolean premium;
    private FloodgateState floodgate;
    private String lastIp;
    private Instant lastLogin;

    public StoredProfile(long rowId, UUID uuid, String playerName, boolean premium, FloodgateState floodgate,
                         String lastIp, Instant lastLogin) {
        super(uuid, playerName);

        this.rowId = rowId;
        this.premium = premium;
        this.floodgate = floodgate;
        this.lastIp = lastIp;
        this.lastLogin = lastLogin;
    }

    public StoredProfile(UUID uuid, String playerName, boolean premium, FloodgateState isFloodgate, String lastIp) {
        this(-1, uuid, playerName, premium, isFloodgate, lastIp, Instant.now());
    }

    @Deprecated
    public StoredProfile(UUID uuid, String playerName, boolean premium, String lastIp) {
        this(-1, uuid, playerName, premium, FloodgateState.FALSE, lastIp, Instant.now());
    }

    public ReentrantLock getSaveLock() {
        return saveLock;
    }

    public synchronized boolean isExistingPlayer() {
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

    /**
     * @return whether the online mode should be enabled for this player
     * @deprecated {@link #isOnlinemodePreferred()} is recommended, because the name represents more its
     * meaning
     */
    @Deprecated
    public synchronized boolean isPremium() {
        return premium;
    }

    /**
     * Return the online mode preference for this player.
     * <p>
     * <b>
     * Note: {@code false} doesn't represent that the player is offline. It could also mean that the player is premium,
     * but didn't activated the premium login mode yet.
     * </b>
     *
     * @return whether the online mode should be enabled for this player
     */
    public synchronized boolean isOnlinemodePreferred() {
        return premium;
    }

    /**
     * @param premium whether online mode is preferred
     * @deprecated {@link #setOnlinemodePreferred(boolean)} is recommended, because of the better method name
     */
    @Deprecated
    public synchronized void setPremium(boolean premium) {
        this.premium = premium;
    }

    public synchronized void setOnlinemodePreferred(boolean premium) {
        this.premium = premium;
    }

    public synchronized FloodgateState getFloodgate() {
        return floodgate;
    }

    public synchronized boolean isFloodgateMigrated() {
        return floodgate != FloodgateState.NOT_MIGRATED;
    }

    public synchronized void setFloodgate(FloodgateState floodgate) {
        this.floodgate = floodgate;
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
    public synchronized boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof StoredProfile)) {
            return false;
        }

        StoredProfile that = (StoredProfile) o;
        if (!super.equals(o)) {
            return false;
        }

        return rowId == that.rowId && premium == that.premium
                && Objects.equals(lastIp, that.lastIp) && lastLogin.equals(that.lastLogin);
    }

    @Override
    public synchronized int hashCode() {
        return Objects.hash(super.hashCode(), rowId, premium, lastIp, lastLogin);
    }

    @Override
    public synchronized String toString() {
        return this.getClass().getSimpleName() + '{'
                + "rowId=" + rowId
                + ", premium=" + premium
                + ", floodgate=" + floodgate
                + ", lastIp='" + lastIp + '\''
                + ", lastLogin=" + lastLogin
                + "} " + super.toString();
    }
}
