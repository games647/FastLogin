/*
 * SPDX-License-Identifier: MIT
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2022 games647 and contributors
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
package com.github.games647.fastlogin.bungee.task;

import com.github.games647.fastlogin.bungee.BungeeLoginSession;
import com.github.games647.fastlogin.bungee.FastLoginBungee;
import com.github.games647.fastlogin.core.shared.FastLoginCore;
import com.github.games647.fastlogin.core.shared.FloodgateManagement;

import java.net.InetSocketAddress;
import java.util.UUID;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;

import org.geysermc.floodgate.api.player.FloodgatePlayer;

public class FloodgateAuthTask
        extends FloodgateManagement<ProxiedPlayer, CommandSender, BungeeLoginSession, FastLoginBungee> {

    private final Server server;

    public FloodgateAuthTask(FastLoginCore<ProxiedPlayer, CommandSender, FastLoginBungee> core, ProxiedPlayer player,
            FloodgatePlayer floodgatePlayer, Server server) {
        super(core, player, floodgatePlayer);
        this.server = server;
    }

    @Override
    protected void startLogin() {
        BungeeLoginSession session = new BungeeLoginSession(player.getName(), isRegistered, profile);
        core.getPlugin().getSession().put(player.getPendingConnection(), session);

        // run login task
        Runnable forceLoginTask = new ForceLoginTask(core.getPlugin().getCore(), player, server, session,
                isAutoAuthAllowed(autoLoginFloodgate));
        core.getPlugin().getScheduler().runAsync(forceLoginTask);
    }

    @Override
    protected String getName(ProxiedPlayer player) {
        return player.getName();
    }

    @Override
    protected UUID getUUID(ProxiedPlayer player) {
        return player.getUniqueId();
    }

    @Override
    protected InetSocketAddress getAddress(ProxiedPlayer player) {
        return player.getAddress();
    }

}
