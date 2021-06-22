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
package com.github.games647.fastlogin.bungee;

import com.github.games647.fastlogin.core.shared.LoginSource;

import java.net.InetSocketAddress;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.PreLoginEvent;

public class BungeeLoginSource implements LoginSource {

    private final PendingConnection connection;
    private final PreLoginEvent preLoginEvent;

    public BungeeLoginSource(PendingConnection connection, PreLoginEvent preLoginEvent) {
        this.connection = connection;
        this.preLoginEvent = preLoginEvent;
    }

    @Override
    public void enableOnlinemode() {
        connection.setOnlineMode(true);
    }

    @Override
    public void kick(String message) {
        preLoginEvent.setCancelled(true);

        if (message == null) {
            preLoginEvent.setCancelReason(new ComponentBuilder("Kicked").color(ChatColor.WHITE).create());
        } else {
            preLoginEvent.setCancelReason(TextComponent.fromLegacyText(message));
        }
    }

    @Override
    public InetSocketAddress getAddress() {
        return connection.getAddress();
    }

    public PendingConnection getConnection() {
        return connection;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + '{' +
                "connection=" + connection +
                '}';
    }
}
