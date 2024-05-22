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
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.github.games647.fastlogin.bukkit.auth.protocollib.packet.ClientPublicKey;
import com.github.games647.fastlogin.core.shared.LoginSource;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Random;

import static com.comphenix.protocol.PacketType.Login.Server.DISCONNECT;
import static com.comphenix.protocol.PacketType.Login.Server.ENCRYPTION_BEGIN;

class ProtocolLibLoginSource implements LoginSource {

    private final Player player;

    private final Random random;

    private final ClientPublicKey clientKey;
    private final PublicKey publicKey;

    private byte[] verifyToken;

    ProtocolLibLoginSource(Player player, Random random, PublicKey serverPublicKey, ClientPublicKey clientKey) {
        this.player = player;
        this.random = random;
        this.publicKey = serverPublicKey;
        this.clientKey = clientKey;
    }

    @Override
    public void enableOnlinemode() {
        verifyToken = EncryptionUtil.generateVerifyToken(random);

        /*
         * Packet Information: https://wiki.vg/Protocol#Encryption_Request
         *
         * ServerID="" (String) key=public server key verifyToken=random 4 byte array
         */
        PacketContainer newPacket = new PacketContainer(ENCRYPTION_BEGIN);

        newPacket.getStrings().write(0, "");
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
        // shouldAuthenticate, but why does this field even exist?
        newPacket.getBooleans().writeSafely(0, true);

        //serverId is an empty string
        ProtocolLibrary.getProtocolManager().sendServerPacket(player, newPacket);
    }

    @Override
    public void kick(String message) {
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

    public ClientPublicKey getClientKey() {
        return clientKey;
    }

    public byte[] getVerifyToken() {
        return verifyToken.clone();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + '{'
            + "player=" + player
            + ", random=" + random
            + ", verifyToken=" + Arrays.toString(verifyToken)
            + '}';
    }
}
