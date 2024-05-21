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
package com.github.games647.fastlogin.velocity.listener;

import com.github.games647.craftapi.UUIDAdapter;
import com.github.games647.fastlogin.core.antibot.AntiBotService;
import com.github.games647.fastlogin.core.antibot.AntiBotService.Action;
import com.github.games647.fastlogin.core.hooks.bedrock.FloodgateService;
import com.github.games647.fastlogin.core.shared.LoginSession;
import com.github.games647.fastlogin.core.storage.StoredProfile;
import com.github.games647.fastlogin.velocity.FastLoginVelocity;
import com.github.games647.fastlogin.velocity.VelocityLoginSession;
import com.github.games647.fastlogin.velocity.task.AsyncPremiumCheck;
import com.github.games647.fastlogin.velocity.task.FloodgateAuthTask;
import com.github.games647.fastlogin.velocity.task.ForceLoginTask;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent.PreLoginComponentResult;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.util.GameProfile.Property;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class ConnectListener {

    private final FastLoginVelocity plugin;
    private final AntiBotService antiBotService;

    public ConnectListener(FastLoginVelocity plugin, AntiBotService antiBotService) {
        this.plugin = plugin;
        this.antiBotService = antiBotService;
    }

    @Subscribe
    public EventTask onPreLogin(PreLoginEvent preLoginEvent) {
        if (!preLoginEvent.getResult().isAllowed()) {
            return null;
        }

        InboundConnection connection = preLoginEvent.getConnection();
        String username = preLoginEvent.getUsername();
        InetSocketAddress address = connection.getRemoteAddress();
        plugin.getLog().info("Incoming login request for {} from {}", username, address);

        Action action = antiBotService.onIncomingConnection(address, username);
        switch (action) {
            case Ignore:
                // just ignore
                return null;
            case Block:
                String message = plugin.getCore().getMessage("kick-antibot");
                TextComponent messageParsed = LegacyComponentSerializer.legacyAmpersand().deserialize(message);

                PreLoginComponentResult reason = PreLoginComponentResult.denied(messageParsed);
                preLoginEvent.setResult(reason);
                return null;
            case Continue:
            default:
                return EventTask.async(
                        new AsyncPremiumCheck(plugin, connection, username, preLoginEvent)
                );
        }
    }

    @Subscribe
    public void onGameProfileRequest(GameProfileRequestEvent event) {
        if (event.isOnlineMode()) {
            LoginSession session = plugin.getSession().get(event.getConnection().getRemoteAddress());
            if (session == null) {
                plugin.getLog().warn("No active login session found for player {}", event.getUsername());
                return;
            }

            UUID verifiedUUID = event.getGameProfile().getId();
            String verifiedUsername = event.getUsername();
            session.setUuid(verifiedUUID);
            session.setVerifiedUsername(verifiedUsername);

            StoredProfile playerProfile = session.getProfile();
            playerProfile.setId(verifiedUUID);
            if (!plugin.getCore().getConfig().get("premiumUuid", true)) {
                UUID offlineUUID = UUIDAdapter.generateOfflineId(playerProfile.getName());
                event.setGameProfile(event.getGameProfile().withId(offlineUUID));
                plugin.getLog().info("Overridden UUID from {} to {} (based of {}) on {}",
                        verifiedUUID, offlineUUID, verifiedUsername, event.getConnection());
            }

            if (!plugin.getCore().getConfig().get("forwardSkin", true)) {
                List<Property> newProp = removeSkin(event.getGameProfile().getProperties());
                event.setGameProfile(event.getGameProfile().withProperties(newProp));
            }
        }
    }

    private List<Property> removeSkin(Collection<Property> oldProperties) {
        List<Property> newProperties = new ArrayList<>(oldProperties.size());
        for (Property property : oldProperties) {
            if (!"textures".equals(property.getName())) {
                newProperties.add(property);
            }
        }

        return newProperties;
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent serverConnectedEvent) {
        Player player = serverConnectedEvent.getPlayer();
        RegisteredServer server = serverConnectedEvent.getServer();

        FloodgateService floodgateService = plugin.getFloodgateService();
        if (floodgateService != null) {
            FloodgatePlayer floodgatePlayer = floodgateService.getBedrockPlayer(player.getUniqueId());
            if (floodgatePlayer != null) {
                plugin.getLog().info("Running floodgate handling for {}", player);
                Runnable floodgateAuthTask = new FloodgateAuthTask(plugin.getCore(), player, floodgatePlayer, server);
                plugin.getScheduler().runAsync(floodgateAuthTask);
                return;
            }
        }

        VelocityLoginSession session = plugin.getSession().get(player.getRemoteAddress());
        if (session == null) {
            plugin.getLog().info("No active login session found on server connect for {}", player);
            return;
        }

        // delay sending force command, because Paper will process the login event asynchronously
        // In this case it means that the force command (plugin message) is already received and processed while
        // player is still in the login phase and reported to be offline.
        Runnable loginTask = new ForceLoginTask(plugin.getCore(), player, server, session);

        // Delay at least one second, otherwise the login command can be missed
        plugin.getScheduler().runAsyncDelayed(loginTask, Duration.ofSeconds(1));
    }
}
