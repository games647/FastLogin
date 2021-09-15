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
package com.github.games647.fastlogin.velocity.listener;

import com.github.games647.fastlogin.core.RateLimiter;
import com.github.games647.fastlogin.core.StoredProfile;
import com.github.games647.fastlogin.core.shared.LoginSession;
import com.github.games647.fastlogin.velocity.FastLoginVelocity;
import com.github.games647.fastlogin.velocity.VelocityLoginSession;
import com.github.games647.fastlogin.velocity.task.AsyncPremiumCheck;
import com.github.games647.fastlogin.velocity.task.ForceLoginTask;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.util.UUID;

public class ConnectListener {

    private final FastLoginVelocity plugin;
    private final RateLimiter rateLimiter;

    public ConnectListener(FastLoginVelocity plugin, RateLimiter rateLimiter) {
        this.plugin = plugin;
        this.rateLimiter = rateLimiter;
    }

    @Subscribe()
    public void onPreLogin(PreLoginEvent preLoginEvent, Continuation continuation) {
        if (!preLoginEvent.getResult().isAllowed()) {
            return;
        }
        InboundConnection connection = preLoginEvent.getConnection();
        if (!rateLimiter.tryAcquire()) {
            plugin.getLog().warn("Simple Anti-Bot join limit - Ignoring {}", connection);
            return;
        }

        String username = preLoginEvent.getUsername();
        plugin.getLog().info("Incoming login request for {} from {}", username, connection.getRemoteAddress());


        Runnable asyncPremiumCheck = new AsyncPremiumCheck(plugin, connection, username, continuation, preLoginEvent);
        plugin.getScheduler().runAsync(asyncPremiumCheck);
    }

    @Subscribe(order = PostOrder.LATE)
    public void onLogin(LoginEvent loginEvent) {
        //use the login event instead of the post login event in order to send the login success packet to the client
        //with the offline uuid this makes it possible to set the skin then
        Player connection = loginEvent.getPlayer();
        if (connection.isOnlineMode()) {
            LoginSession session = plugin.getSession().get(connection.getRemoteAddress());

            UUID verifiedUUID = connection.getUniqueId();
            String verifiedUsername = connection.getUsername();
            session.setUuid(verifiedUUID);
            session.setVerifiedUsername(verifiedUsername);

            StoredProfile playerProfile = session.getProfile();
            playerProfile.setId(verifiedUUID);

            // bungeecord will do this automatically so override it on disabled option
//            if (uniqueIdSetter != null) {
//                InitialHandler initialHandler = (InitialHandler) connection;
//
//                if (!plugin.getCore().getConfig().get("premiumUuid", true)) {
//                    setOfflineId(initialHandler, verifiedUsername);
//                }
//
//                if (!plugin.getCore().getConfig().get("forwardSkin", true)) {
//                    // this is null on offline mode
//                    LoginResult loginProfile = initialHandler.getLoginProfile();
//                    loginProfile.setProperties(emptyProperties);
//                }
//            }
        }
    }

//    private void setOfflineId(InitialHandler connection, String username) {
//        try {
//            final UUID oldPremiumId = connection.getUniqueId();
//            final UUID offlineUUID = UUIDAdapter.generateOfflineId(username);
//
//            // BungeeCord only allows setting the UUID in PreLogin events and before requesting online mode
//            // However if online mode is requested, it will override previous values
//            // So we have to do it with reflection
//            uniqueIdSetter.invokeExact(connection, offlineUUID);
//
//            String format = "Overridden UUID from {} to {} (based of {}) on {}";
//            plugin.getLog().info(format, oldPremiumId, offlineUUID, username, connection);
//        } catch (Exception ex) {
//            plugin.getLog().error("Failed to set offline uuid of {}", username, ex);
//        } catch (Throwable throwable) {
//            // throw remaining exceptions like outofmemory that we shouldn't handle ourself
//            Throwables.throwIfUnchecked(throwable);
//        }
//    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent serverConnectedEvent) {
        Player player = serverConnectedEvent.getPlayer();
        RegisteredServer server = serverConnectedEvent.getServer();

        VelocityLoginSession session = plugin.getSession().get(player.getRemoteAddress());
        if (session == null) {
            return;
        }

        // delay sending force command, because Paper will process the login event asynchronously
        // In this case it means that the force command (plugin message) is already received and processed while
        // player is still in the login phase and reported to be offline.
        Runnable loginTask = new ForceLoginTask(plugin.getCore(), player, server, session);
        plugin.getScheduler().runAsync(loginTask);
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent disconnectEvent) {
        Player player = disconnectEvent.getPlayer();
        assert plugin.getSession().remove(player.getRemoteAddress()) != null;
        plugin.getCore().getPendingConfirms().remove(player.getUniqueId());
    }
}
