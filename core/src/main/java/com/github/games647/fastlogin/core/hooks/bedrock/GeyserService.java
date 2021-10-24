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
package com.github.games647.fastlogin.core.hooks.bedrock;

import java.util.UUID;

import com.github.games647.fastlogin.core.shared.FastLoginCore;
import com.github.games647.fastlogin.core.shared.LoginSource;

import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;

public class GeyserService extends BedrockService<GeyserSession> {

    private final GeyserConnector geyser;
    private final FastLoginCore<?, ?, ?> core;

    public GeyserService(GeyserConnector geyser, FastLoginCore<?, ?, ?> core) {
        super(core);
        this.geyser = geyser;
        this.core = core;
    }

    @Override
    public void checkNameConflict(String username, LoginSource source) {
        //TODO: Replace stub with Geyser specific code      
        if ("false".equals(allowConflict)) {
                super.checkNameConflict(username, source);
        } else {
            core.getPlugin().getLog().info("Skipping name conflict checking for player {}", username);
        }
    }

    @Override
    public GeyserSession getBedrockPlayer(String username) {
        for (GeyserSession gSess : geyser.getSessionManager().getSessions().values()) {
            if (gSess.getName().equals(username)) {
                return gSess;
            }
        }

        return null;
    }

    @Override
    public GeyserSession getBedrockPlayer(UUID uuid) {
        return geyser.getPlayerByUuid(uuid);
    }
}
