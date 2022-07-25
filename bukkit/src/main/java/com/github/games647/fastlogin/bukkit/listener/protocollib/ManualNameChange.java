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
package com.github.games647.fastlogin.bukkit.listener.protocollib;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.core.hooks.bedrock.FloodgateService;

import java.util.UUID;

import org.geysermc.floodgate.api.FloodgateApi;

import static com.comphenix.protocol.PacketType.Login.Client.START;

/**
 * Manually inject Floodgate player name prefixes.
 * <br>
 * This is used as a workaround, because Floodgate fails to inject
 * the prefixes when it's used together with ProtocolLib and FastLogin.
 * <br>
 * For more information visit: <a href="https://github.com/games647/FastLogin/issues/493">...</a>
 */
public class ManualNameChange extends PacketAdapter {

    private final FloodgateService floodgate;

    public ManualNameChange(FastLoginBukkit plugin, FloodgateService floodgate) {
        super(params()
                .plugin(plugin)
                .types(START));

        this.plugin = plugin;
        this.floodgate = floodgate;
    }

    public static void register(FastLoginBukkit plugin, FloodgateService floodgate) {
        // they will be created with a static builder, because otherwise it will throw a NoClassDefFoundError
        ProtocolLibrary.getProtocolManager()
                .getAsynchronousManager()
                .registerAsyncHandler(new ManualNameChange(plugin, floodgate))
                .start();
    }

    @Override
    public void onPacketReceiving(PacketEvent packetEvent) {
        PacketContainer packet = packetEvent.getPacket();
        WrappedGameProfile originalProfile = packet.getGameProfiles().read(0);

        if (floodgate.getBedrockPlayer(originalProfile.getName()) == null) {
            //not a Floodgate player, no need to add a prefix
            return;
        }

        packet.setMeta("original_name", originalProfile.getName());
        String prefixedName = FloodgateApi.getInstance().getPlayerPrefix() + originalProfile.getName();
        setUsername(packet, prefixedName);
    }
    
    private void setUsername(PacketContainer packet, String name) {
        if (packet.getGameProfiles().size() > 0) {
            WrappedGameProfile updatedProfile = new WrappedGameProfile(UUID.randomUUID(), name);
            packet.getGameProfiles().write(0, updatedProfile);
        } else {
            packet.getStrings().write(0, name);
        }
    }
}
