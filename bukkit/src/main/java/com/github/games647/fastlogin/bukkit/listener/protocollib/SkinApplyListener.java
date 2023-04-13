/*
 * SPDX-License-Identifier: MIT
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2023 games647 and contributors
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
package com.github.games647.fastlogin.bukkit.listener.protocollib;

import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import com.github.games647.craftapi.model.skin.Textures;
import com.github.games647.fastlogin.bukkit.BukkitLoginSession;
import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;

public class SkinApplyListener implements Listener {

    private final FastLoginBukkit plugin;

    public SkinApplyListener(FastLoginBukkit plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    //run this on the loginEvent to let skins plugins see the skin like in normal Minecraft behaviour
    public void onPlayerLogin(PlayerLoginEvent loginEvent) {
        if (loginEvent.getResult() != Result.ALLOWED) {
            return;
        }

        Player player = loginEvent.getPlayer();

        //go through every session, because player.getAddress is null
        //loginEvent.getAddress is just a InetAddress not InetSocketAddress, so not unique enough
        for (BukkitLoginSession session : plugin.getLoginSessions().values()) {
            if (session.getUsername().equals(player.getName())) {
                session.getSkin().ifPresent(skin -> applySkin(player, skin.getValue(), skin.getSignature()));
                break;
            }
        }
    }

    private void applySkin(Player player, String skinData, String signature) {
        WrappedGameProfile gameProfile = WrappedGameProfile.fromPlayer(player);

        WrappedSignedProperty skin = WrappedSignedProperty.fromValues(Textures.KEY, skinData, signature);
        gameProfile.getProperties().put(Textures.KEY, skin);
    }
}
