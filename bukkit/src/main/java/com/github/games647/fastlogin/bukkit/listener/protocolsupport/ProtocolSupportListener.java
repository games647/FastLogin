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

import com.github.games647.craftapi.UUIDAdapter;
import com.github.games647.fastlogin.bukkit.BukkitLoginSession;
import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.bukkit.event.BukkitFastLoginPreLoginEvent;
import com.github.games647.fastlogin.core.StoredProfile;
import com.github.games647.fastlogin.core.antibot.AntiBotService;
import com.github.games647.fastlogin.core.antibot.AntiBotService.Action;
import com.github.games647.fastlogin.core.shared.JoinManagement;
import com.github.games647.fastlogin.core.shared.event.FastLoginPreLoginEvent;

import java.net.InetSocketAddress;
import java.util.Optional;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import protocolsupport.api.events.ConnectionCloseEvent;
import protocolsupport.api.events.PlayerLoginStartEvent;
import protocolsupport.api.events.PlayerProfileCompleteEvent;

public class ProtocolSupportListener extends JoinManagement<Player, CommandSender, ProtocolLoginSource>
        implements Listener {

    private final FastLoginBukkit plugin;
    private final AntiBotService antiBotService;

    public ProtocolSupportListener(FastLoginBukkit plugin, AntiBotService antiBotService) {
        super(plugin.getCore(), plugin.getCore().getAuthPluginHook(), plugin.getBedrockService());

        this.plugin = plugin;
        this.antiBotService = antiBotService;
    }

    @EventHandler
    public void onLoginStart(PlayerLoginStartEvent loginStartEvent) {
        if (loginStartEvent.isLoginDenied() || plugin.getCore().getAuthPluginHook() == null) {
            return;
        }

        String username = loginStartEvent.getConnection().getProfile().getName();
        InetSocketAddress address = loginStartEvent.getConnection().getRawAddress();
        plugin.getLog().info("Incoming login request for {} from {}", username, address);

        Action action = antiBotService.onIncomingConnection(address, username);
        switch (action) {
            case Ignore:
                // just ignore
                return;
            case Block:
                String message = plugin.getCore().getMessage("kick-antibot");
                loginStartEvent.denyLogin(message);
                break;
            case Continue:
            default:
                //remove old data every time on a new login in order to keep the session only for one person
                plugin.removeSession(address);

                ProtocolLoginSource source = new ProtocolLoginSource(loginStartEvent);
                super.onLogin(username, source);
                break;
        }
    }

    @EventHandler
    public void onConnectionClosed(ConnectionCloseEvent closeEvent) {
        InetSocketAddress address = closeEvent.getConnection().getRawAddress();
        plugin.removeSession(address);
    }

    @EventHandler
    public void onPropertiesResolve(PlayerProfileCompleteEvent profileCompleteEvent) {
        InetSocketAddress address = profileCompleteEvent.getConnection().getRawAddress();

        BukkitLoginSession session = plugin.getSession(address);

        if (session != null && profileCompleteEvent.getConnection().getProfile().isOnlineMode()) {
            session.setVerified(true);

            if (!plugin.getConfig().getBoolean("premiumUuid")) {
                String username = Optional.ofNullable(profileCompleteEvent.getForcedName())
                        .orElse(profileCompleteEvent.getConnection().getProfile().getName());
                profileCompleteEvent.setForcedUUID(UUIDAdapter.generateOfflineId(username));
            }
        }
    }

    @Override
    public FastLoginPreLoginEvent callFastLoginPreLoginEvent(String username, ProtocolLoginSource source,
                                                             StoredProfile profile) {
        BukkitFastLoginPreLoginEvent event = new BukkitFastLoginPreLoginEvent(username, source, profile);
        plugin.getServer().getPluginManager().callEvent(event);
        return event;
    }

    @Override
    public void requestPremiumLogin(ProtocolLoginSource source, StoredProfile profile, String username,
                                    boolean registered) {
        source.enableOnlinemode();

        String ip = source.getAddress().getAddress().getHostAddress();
        plugin.getCore().getPendingLogin().put(ip + username, new Object());

        BukkitLoginSession playerSession = new BukkitLoginSession(username, registered, profile);
        plugin.putSession(source.getAddress(), playerSession);
    }

    @Override
    public void startCrackedSession(ProtocolLoginSource source, StoredProfile profile, String username) {
        BukkitLoginSession loginSession = new BukkitLoginSession(username, profile);
        plugin.putSession(source.getAddress(), loginSession);
    }
}
