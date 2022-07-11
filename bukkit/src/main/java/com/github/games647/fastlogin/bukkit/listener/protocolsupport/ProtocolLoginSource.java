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
package com.github.games647.fastlogin.bukkit.listener.protocolsupport;

import com.github.games647.fastlogin.core.shared.LoginSource;

import java.net.InetSocketAddress;

import protocolsupport.api.events.PlayerLoginStartEvent;

public class ProtocolLoginSource implements LoginSource {

    private final PlayerLoginStartEvent loginStartEvent;

    public ProtocolLoginSource(PlayerLoginStartEvent loginStartEvent) {
        this.loginStartEvent = loginStartEvent;
    }

    @Override
    public void enableOnlinemode() {
        loginStartEvent.setOnlineMode(true);
    }

    @Override
    public void kick(String message) {
        loginStartEvent.denyLogin(message);
    }

    @Override
    public InetSocketAddress getAddress() {
        return loginStartEvent.getAddress();
    }

    public PlayerLoginStartEvent getLoginStartEvent() {
        return loginStartEvent;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + '{'
            + "loginStartEvent=" + loginStartEvent
            + '}';
    }
}
