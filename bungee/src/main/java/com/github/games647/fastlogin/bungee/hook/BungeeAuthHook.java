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
package com.github.games647.fastlogin.bungee.hook;

import com.github.games647.fastlogin.bungee.FastLoginBungee;
import com.github.games647.fastlogin.core.hooks.AuthPlugin;
import me.vik1395.BungeeAuth.Main;
import me.vik1395.BungeeAuthAPI.RequestHandler;
import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * GitHub:
 * <a href="https://github.com/vik1395/BungeeAuth-Minecraft">...</a>
 * <p>
 * Project page:
 * <p>
 * Spigot:
 * <a href="https://www.spigotmc.org/resources/bungeeauth.493/">...</a>
 */
public class BungeeAuthHook implements AuthPlugin<ProxiedPlayer> {

    private final RequestHandler requestHandler = new RequestHandler();

    public BungeeAuthHook(FastLoginBungee plugin) {
    }

    @Override
    public boolean forceLogin(ProxiedPlayer player) {
        String playerName = player.getName();
        return Main.plonline.contains(playerName) || requestHandler.forceLogin(playerName);
    }

    @Override
    public boolean isRegistered(String playerName) {
        return requestHandler.isRegistered(playerName);
    }

    @Override
    public boolean forceRegister(ProxiedPlayer player, String password) {
        return requestHandler.forceRegister(player, password);
    }
}
