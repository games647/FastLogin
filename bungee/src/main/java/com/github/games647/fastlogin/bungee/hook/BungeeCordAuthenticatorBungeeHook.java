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
package com.github.games647.fastlogin.bungee.hook;

import com.github.games647.fastlogin.bungee.FastLoginBungee;
import com.github.games647.fastlogin.core.hooks.AuthPlugin;

import de.xxschrandxx.bca.bungee.BungeeCordAuthenticatorBungee;
import de.xxschrandxx.bca.bungee.api.BungeeCordAuthenticatorBungeeAPI;

import java.sql.SQLException;
import java.util.logging.Level;

import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * GitHub:
 * <a href="https://github.com/xXSchrandXx/SpigotPlugins/tree/master/BungeeCordAuthenticator">...</a>
 * <p>
 * Project page:
 * <p>
 * Spigot: <a href="https://www.spigotmc.org/resources/bungeecordauthenticator.87669/">...</a>
 */
public class BungeeCordAuthenticatorBungeeHook implements AuthPlugin<ProxiedPlayer> {

    public final BungeeCordAuthenticatorBungeeAPI api;

    public BungeeCordAuthenticatorBungeeHook(FastLoginBungee plugin) {
        api = ((BungeeCordAuthenticatorBungee) plugin.getProxy().getPluginManager()
            .getPlugin("BungeeCordAuthenticatorBungee")).getAPI();
        plugin.getLog().info("BungeeCordAuthenticatorHook | Hooked successful!");
    }

    @Override
    public boolean forceLogin(ProxiedPlayer player) {
        if (api.isAuthenticated(player)) {
            return true;
        }

        try {
            api.setAuthenticated(player);
        } catch (SQLException sqlEx) {
            api.getLogger().log(Level.WARNING, "Failed to force login", sqlEx);
            return false;
        }

        return true;
    }

    @Override
    public boolean isRegistered(String playerName) {
        try {
            return api.getSQL().checkPlayerEntry(playerName);
        } catch (SQLException sqlEx) {
            api.getLogger().log(Level.WARNING, "Failed to check registration", sqlEx);
            return false;
        }
    }

    @Override
    public boolean forceRegister(ProxiedPlayer player, String password) {
        try {
            return api.createPlayerEntry(player, password);
        } catch (SQLException sqlEx) {
            api.getLogger().log(Level.WARNING, "Failed to force register", sqlEx);
            return false;
        }
    }
}
