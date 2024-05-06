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
package com.github.games647.fastlogin.core.shared;

import com.github.games647.fastlogin.core.storage.StoredProfile;

import java.util.StringJoiner;
import java.util.UUID;

public abstract class LoginSession {

    private final StoredProfile profile;

    private final String requestUsername;
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

    public synchronized String getUsername() {
        return username;
    }

    public synchronized void setVerifiedUsername(String username) {
        this.username = username;
    }

    /**
     * Check if user needs registration once login is successful
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
    public String toString() {
        return new StringJoiner(", ", LoginSession.class.getSimpleName() + '[', "]")
                .add("profile=" + profile)
                .add("requestUsername='" + requestUsername + '\'')
                .add("username='" + username + '\'')
                .add("uuid=" + uuid)
                .add("registered=" + registered)
                .toString();
    }
}
