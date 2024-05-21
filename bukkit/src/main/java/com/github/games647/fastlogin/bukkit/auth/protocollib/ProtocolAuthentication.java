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
package com.github.games647.fastlogin.bukkit.auth.protocollib;

import com.comphenix.protocol.ProtocolLibrary;
import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.bukkit.auth.LocalAuthentication;
import com.github.games647.fastlogin.core.antibot.AntiBotService;
import com.github.games647.fastlogin.core.shared.FastLoginCore;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.PluginManager;

public class ProtocolAuthentication extends LocalAuthentication implements Listener {

    public ProtocolAuthentication(FastLoginBukkit plugin) {
        super(plugin);
    }

    @Override
    public boolean isAvailable() {
        return plugin.getServer().getPluginManager().isPluginEnabled("ProtocolLib");
    }

    @Override
    public void init(PluginManager pluginManager) {
        pluginManager.registerEvents(this, plugin);

        FastLoginCore<Player, CommandSender, FastLoginBukkit> core = plugin.getCore();
        AntiBotService antiBotService = core.getAntiBotService();
        ProtocolLibListener.register(plugin, antiBotService,  core.getConfig().getBoolean("verifyClientKeys"));
    }

    @Override
    public void stop() {
        ProtocolLibrary.getProtocolManager().getAsynchronousManager().unregisterAsyncHandlers(plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerLoginEvent loginEvent) {
        if (loginEvent.getResult() == PlayerLoginEvent.Result.ALLOWED && !plugin.isInitialized()) {
            loginEvent.disallow(PlayerLoginEvent.Result.KICK_OTHER, plugin.getCore().getMessage("not-started"));
        }
    }
}
