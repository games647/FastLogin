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
package com.github.games647.fastlogin.bungee.task;

import com.github.games647.fastlogin.bungee.BungeeLoginSession;
import com.github.games647.fastlogin.bungee.BungeeLoginSource;
import com.github.games647.fastlogin.bungee.FastLoginBungee;
import com.github.games647.fastlogin.bungee.event.BungeeFastLoginPreLoginEvent;
import com.github.games647.fastlogin.core.StoredProfile;
import com.github.games647.fastlogin.core.shared.JoinManagement;
import com.github.games647.fastlogin.core.shared.event.FastLoginPreLoginEvent;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PreLoginEvent;

public class AsyncPremiumCheck extends JoinManagement<ProxiedPlayer, CommandSender, BungeeLoginSource>
        implements Runnable {

    private final FastLoginBungee plugin;
    private final PreLoginEvent preLoginEvent;

    private final String username;
    private final PendingConnection connection;

    public AsyncPremiumCheck(FastLoginBungee plugin, PreLoginEvent preLoginEvent, PendingConnection connection,
                             String username) {
        super(plugin.getCore(), plugin.getCore().getAuthPluginHook(), plugin.getFloodgateService());

        this.plugin = plugin;
        this.preLoginEvent = preLoginEvent;
        this.connection = connection;
        this.username = username;
    }

    @Override
    public void run() {
        plugin.getSession().remove(connection);

        try {
            super.onLogin(username, new BungeeLoginSource(connection, preLoginEvent));
        } finally {
            preLoginEvent.completeIntent(plugin);
        }
    }

    @Override
    public FastLoginPreLoginEvent callFastLoginPreLoginEvent(String username, BungeeLoginSource source,
                                                             StoredProfile profile) {
        return plugin.getProxy().getPluginManager()
                .callEvent(new BungeeFastLoginPreLoginEvent(username, source, profile));
    }

    @Override
    public void requestPremiumLogin(BungeeLoginSource source, StoredProfile profile,
                                    String username, boolean registered) {
        source.enableOnlinemode();
        plugin.getSession().put(source.getConnection(), new BungeeLoginSession(username, registered, profile));

        String ip = source.getAddress().getAddress().getHostAddress();
        plugin.getCore().getPendingLogin().put(ip + username, new Object());
    }

    @Override
    public void startCrackedSession(BungeeLoginSource source, StoredProfile profile, String username) {
        plugin.getSession().put(source.getConnection(), new BungeeLoginSession(username, false, profile));
    }
}
