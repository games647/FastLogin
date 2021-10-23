/*
 * SPDX-License-Identifier: MIT
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2021 <Your name and contributors>
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
package com.github.games647.fastlogin.core.hooks;

import java.util.UUID;

import com.github.games647.fastlogin.core.shared.FastLoginCore;

import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;

public class GeyserService {

    private final GeyserConnector geyser;
    private final FastLoginCore<?, ?, ?> core;

    public GeyserService(GeyserConnector geyser, FastLoginCore<?, ?, ?> core) {
        this.geyser = geyser;
        this.core = core;
    }

    /**
     * The Geyser API does not support querying players by name, so this function
     * iterates over every online Geyser Player and checks if the requested
     * username can be found
     * 
     * @param username the name of the player
     * @return GeyserSession if found, null otherwise
     */
    public GeyserSession getGeyserPlayer(String username) {
        for (GeyserSession gSess : geyser.getSessionManager().getSessions().values()) {
            if (gSess.getName().equals(username)) {
                return gSess;
            }
        }

        return null;
    }

    public GeyserSession getGeyserPlayer(UUID uuid) {
        return geyser.getPlayerByUuid(uuid);
    }

    public boolean isGeyserPlayer(UUID uuid) {
        return getGeyserPlayer(uuid) != null;
    }

    public boolean isGeyserConnection(String username) {
        return getGeyserPlayer(username) != null;
    }
}
