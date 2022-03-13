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
package com.github.games647.fastlogin.bukkit.hook;

import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.core.hooks.AuthPlugin;
import com.lenis0012.bukkit.loginsecurity.LoginSecurity;
import com.lenis0012.bukkit.loginsecurity.session.AuthService;
import com.lenis0012.bukkit.loginsecurity.session.PlayerSession;
import com.lenis0012.bukkit.loginsecurity.session.action.LoginAction;
import com.lenis0012.bukkit.loginsecurity.session.action.RegisterAction;

import org.bukkit.entity.Player;

/**
 * GitHub: https://github.com/lenis0012/LoginSecurity-2
 * <p>
 * Project page:
 * <p>
 * Bukkit: https://dev.bukkit.org/bukkit-plugins/loginsecurity/
 * <p>
 * Spigot: https://www.spigotmc.org/resources/loginsecurity.19362/
 */
public class LoginSecurityHook implements AuthPlugin<Player> {

    private final FastLoginBukkit plugin;

    public LoginSecurityHook(FastLoginBukkit plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean forceLogin(Player player) {
        PlayerSession session = LoginSecurity.getSessionManager().getPlayerSession(player);
        if (session.isAuthorized()) {
            plugin.getLog().warn(ALREADY_AUTHENTICATED, player);
            return true;
        }

        return session.isAuthorized()
                || session.performAction(new LoginAction(AuthService.PLUGIN, plugin)).isSuccess();
    }

    @Override
    public boolean isRegistered(String playerName) {
        PlayerSession session = LoginSecurity.getSessionManager().getOfflineSession(playerName);
        return session.isRegistered();
    }

    @Override
    public boolean forceRegister(Player player, String password) {
        PlayerSession session = LoginSecurity.getSessionManager().getPlayerSession(player);
        return session.performAction(new RegisterAction(AuthService.PLUGIN, plugin, password)).isSuccess();
    }
}
