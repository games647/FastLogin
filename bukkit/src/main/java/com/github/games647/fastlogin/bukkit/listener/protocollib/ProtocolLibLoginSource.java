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
package com.github.games647.fastlogin.bukkit.listener.protocollib;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.github.games647.fastlogin.core.shared.LoginSource;

import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Random;

import org.bukkit.entity.Player;

import static com.comphenix.protocol.PacketType.Login.Server.DISCONNECT;
import static com.comphenix.protocol.PacketType.Login.Server.ENCRYPTION_BEGIN;

class ProtocolLibLoginSource implements LoginSource {

    private final Player player;

    private final Random random;
    private final PublicKey publicKey;

    private final String serverId = "";
    private byte[] verifyToken;

    public ProtocolLibLoginSource(Player player, Random random, PublicKey publicKey) {
        this.player = player;
        this.random = random;
        this.publicKey = publicKey;
    }

    @Override
    public void enableOnlinemode() throws Exception {
        verifyToken = EncryptionUtil.generateVerifyToken(random);

        /*
         * Packet Information: https://wiki.vg/Protocol#Encryption_Request
         *
         * ServerID="" (String) key=public server key verifyToken=random 4 byte array
         */
        PacketContainer newPacket = new PacketContainer(ENCRYPTION_BEGIN);

        newPacket.getStrings().write(0, serverId);
        StructureModifier<PublicKey> keyModifier = newPacket.getSpecificModifier(PublicKey.class);
        int verifyField = 0;
        if (keyModifier.getFields().isEmpty()) {
            // Since 1.16.4 this is now a byte field
            newPacket.getByteArrays().write(0, publicKey.getEncoded());
            verifyField++;
        } else {
            keyModifier.write(0, publicKey);
        }

        newPacket.getByteArrays().write(verifyField, verifyToken);

        //serverId is an empty string
        ProtocolLibrary.getProtocolManager().sendServerPacket(player, newPacket);
    }

    @Override
    public void kick(String message) throws InvocationTargetException {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

        PacketContainer kickPacket = new PacketContainer(DISCONNECT);
        kickPacket.getChatComponents().write(0, WrappedChatComponent.fromText(message));

        try {
            //send kick packet at login state
            //the normal event.getPlayer.kickPlayer(String) method does only work at play state
            protocolManager.sendServerPacket(player, kickPacket);
        } finally {
            //tell the server that we want to close the connection
            player.kickPlayer("Disconnect");
        }
    }

    @Override
    public InetSocketAddress getAddress() {
        return player.getAddress();
    }

    public String getServerId() {
        return serverId;
    }

    public byte[] getVerifyToken() {
        return verifyToken.clone();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + '{' +
                "player=" + player +
                ", random=" + random +
                ", serverId='" + serverId + '\'' +
                ", verifyToken=" + Arrays.toString(verifyToken) +
                '}';
    }
}
