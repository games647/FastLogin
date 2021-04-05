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
package com.github.games647.fastlogin.bukkit.hook;

import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.core.hooks.AuthPlugin;

import io.github.lucaseasedup.logit.CancelledState;
import io.github.lucaseasedup.logit.LogItCore;
import io.github.lucaseasedup.logit.account.Account;
import io.github.lucaseasedup.logit.session.SessionManager;

import java.time.Instant;

import org.bukkit.entity.Player;

/**
 * GitHub: https://github.com/XziomekX/LogIt
 * <p>
 * Project page:
 * <p>
 * Bukkit: Unknown
 * <p>
 * Spigot: Unknown
 */
public class LogItHook implements AuthPlugin<Player> {

    private final FastLoginBukkit plugin;

    public LogItHook(FastLoginBukkit plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean forceLogin(Player player) {
        SessionManager sessionManager = LogItCore.getInstance().getSessionManager();
        if (sessionManager.isSessionAlive(player)) {
            plugin.getLog().warn(ALREADY_AUTHENTICATED, player);
            return false;
        }

        return sessionManager.startSession(player) == CancelledState.NOT_CANCELLED;
    }

    @Override
    public boolean isRegistered(String playerName) {
        return LogItCore.getInstance().getAccountManager().isRegistered(playerName);
    }

    @Override
    public boolean forceRegister(Player player, String password) {
        Account account = new Account(player.getName());
        account.changePassword(password);

        Instant now = Instant.now();
        account.setLastActiveDate(now.getEpochSecond());
        account.setRegistrationDate(now.getEpochSecond());
        return LogItCore.getInstance().getAccountManager().insertAccount(account) == CancelledState.NOT_CANCELLED;
    }
}
