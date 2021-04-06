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
import org.bukkit.entity.Player;
import red.mohist.sodionauth.bukkit.implementation.BukkitPlayer;
import red.mohist.sodionauth.core.SodionAuthApi;
import red.mohist.sodionauth.core.exception.AuthenticatedException;

/**
 * GitHub: https://github.com/Mohist-Community/SodionAuth
 * <p>
 * Project page: https://gitea.e-loli.com/SodionAuth/SodionAuth
 * <p>
 * Bukkit: Unknown
 * <p>
 * Spigot: https://www.spigotmc.org/resources/sodionauth.76944/
 */
public class SodionAuthHook implements AuthPlugin<Player> {

    private final FastLoginBukkit plugin;

    public SodionAuthHook(FastLoginBukkit plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean forceLogin(Player player) {
        try {
            SodionAuthApi.login(new BukkitPlayer(player));
        } catch (AuthenticatedException e) {
            plugin.getLog().warn(ALREADY_AUTHENTICATED, player);
            return false;
        }
        return true;
    }

    @Override
    public boolean forceRegister(Player player, String password) {
        try{
            return SodionAuthApi.register(new BukkitPlayer(player), password);
        } catch (UnsupportedOperationException e){
            plugin.getLog().warn("Currently SodionAuth is not accepting forceRegister, " +
                    "It may be caused by unsupported AuthBackend");
            return false;
        }
    }

    @Override
    public boolean isRegistered(String playerName) {
        return SodionAuthApi.isRegistered(playerName);
    }
}
