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

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.utility.MinecraftVersion;
import com.comphenix.protocol.wrappers.BukkitConverters;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedProfilePublicKey.WrappedProfileKeyData;
import com.github.games647.fastlogin.bukkit.BukkitLoginSession;
import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.bukkit.listener.protocollib.packet.ClientPublicKey;
import com.github.games647.fastlogin.core.antibot.AntiBotService;
import com.github.games647.fastlogin.core.antibot.AntiBotService.Action;
import com.mojang.datafixers.util.Either;

import java.net.InetSocketAddress;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import lombok.var;
import org.bukkit.entity.Player;

import static com.comphenix.protocol.PacketType.Login.Client.ENCRYPTION_BEGIN;
import static com.comphenix.protocol.PacketType.Login.Client.START;

public class ProtocolLibListener extends PacketAdapter {

    public static final String SOURCE_META_KEY = "source";

    private final FastLoginBukkit plugin;

    //just create a new once on plugin enable. This used for verify token generation
    private final SecureRandom random = new SecureRandom();
    private final KeyPair keyPair = EncryptionUtil.generateKeyPair();
    private final AntiBotService antiBotService;

    private final boolean verifyClientKeys;

    private PacketContainer lastStartPacket;

    public ProtocolLibListener(FastLoginBukkit plugin, AntiBotService antiBotService, boolean verifyClientKeys) {
        //run async in order to not block the server, because we are making api calls to Mojang
        super(params()
                .plugin(plugin)
                .types(START, ENCRYPTION_BEGIN));

        this.plugin = plugin;
        this.antiBotService = antiBotService;
        this.verifyClientKeys = verifyClientKeys;
    }

    public static void register(FastLoginBukkit plugin, AntiBotService antiBotService, boolean verifyClientKeys) {
        // they will be created with a static builder, because otherwise it will throw a NoClassDefFoundError
        // TODO: make synchronous processing, but do web or database requests async
        ProtocolLibrary.getProtocolManager()
                .addPacketListener(new ProtocolLibListener(plugin, antiBotService, verifyClientKeys));
    }

    @Override
    public void onPacketReceiving(PacketEvent packetEvent) {
        PacketContainer packet = packetEvent.getPacket();
        plugin.getLog().info("New packet {} from {}; Cancellation: {}, Meta: {}",
                packetEvent.getPacketType(), packetEvent.getPlayer(), packetEvent.isCancelled(),
                packet.getMeta(SOURCE_META_KEY)
        );

        if (packetEvent.getPacketType() == START) {
            plugin.getLog().info("Start-packet equality (Last/New): {}/{}, {}",
                    lastStartPacket.hashCode(), packet.hashCode(), Objects.equals(lastStartPacket, packet)
            );
            lastStartPacket = packet;
        }

        if (packetEvent.isCancelled()
                || plugin.getCore().getAuthPluginHook() == null
                || !plugin.isServerFullyStarted()) {
            return;
        }

        if (isFastLoginPacket(packetEvent)) {
            // this is our own packet
            return;
        }

        Player sender = packetEvent.getPlayer();
        PacketType packetType = packetEvent.getPacketType();
        if (packetType == START) {
            // PacketContainer packet = packet;

            InetSocketAddress address = sender.getAddress();
            String username = getUsername(packet);

            Action action = antiBotService.onIncomingConnection(address, username);
            switch (action) {
                case Ignore:
                    // just ignore
                    return;
                case Block:
                    String message = plugin.getCore().getMessage("kick-antibot");
                    sender.kickPlayer(message);
                    break;
                case Continue:
                default:
                    //player.getName() won't work at this state
                    onLoginStart(packetEvent, sender, username);
                    break;
            }
        } else {
            onEncryptionBegin(packetEvent, sender);
        }
    }

    private boolean isFastLoginPacket(PacketEvent packetEvent) {
        return packetEvent.getPacket().getMeta(SOURCE_META_KEY)
                .map(val -> val.equals(plugin.getName()))
                .orElse(false);
    }

    private void onEncryptionBegin(PacketEvent packetEvent, Player sender) {
        byte[] sharedSecret = packetEvent.getPacket().getByteArrays().read(0);

        BukkitLoginSession session = plugin.getSession(sender.getAddress());
        if (session == null) {
            plugin.getLog().warn("Profile {} tried to send encryption response at invalid state", sender.getAddress());
            sender.kickPlayer(plugin.getCore().getMessage("invalid-request"));
        } else {
            byte[] expectedVerifyToken = session.getVerifyToken();
            if (verifyNonce(sender, packetEvent.getPacket(), session.getClientPublicKey(), expectedVerifyToken)) {
                // packetEvent.getAsyncMarker().incrementProcessingDelay();

                Runnable verifyTask = new VerifyResponseTask(
                        plugin, packetEvent, sender, session, sharedSecret, keyPair
                );
                verifyTask.run();
                // plugin.getScheduler().runAsync(verifyTask);
            } else {
                sender.kickPlayer(plugin.getCore().getMessage("invalid-verify-token"));
            }
        }
    }

    private boolean verifyNonce(Player sender, PacketContainer packet,
                                ClientPublicKey clientPublicKey, byte[] expectedToken) {
        try {
            if (MinecraftVersion.atOrAbove(new MinecraftVersion(1, 19, 0))) {
                Either<byte[], ?> either = packet.getSpecificModifier(Either.class).read(0);
                if (clientPublicKey == null) {
                    Optional<byte[]> left = either.left();
                    if (!left.isPresent()) {
                        plugin.getLog().error("No verify token sent if requested without player signed key {}", sender);
                        return false;
                    }

                    return EncryptionUtil.verifyNonce(expectedToken, keyPair.getPrivate(), left.get());
                } else {
                    Optional<?> optSignatureData = either.right();
                    if (!optSignatureData.isPresent()) {
                        plugin.getLog().error("No signature given to sent player signing key {}", sender);
                        return false;
                    }

                    Object signatureData = optSignatureData.get();
                    long salt = FuzzyReflection.getFieldValue(signatureData, Long.TYPE, true);
                    byte[] signature = FuzzyReflection.getFieldValue(signatureData, byte[].class, true);

                    PublicKey publicKey = clientPublicKey.key();
                    return EncryptionUtil.verifySignedNonce(expectedToken, publicKey, salt, signature);
                }
            } else {
                byte[] nonce = packet.getByteArrays().read(1);
                return EncryptionUtil.verifyNonce(expectedToken, keyPair.getPrivate(), nonce);
            }
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException | NoSuchPaddingException
                 | IllegalBlockSizeException | BadPaddingException signatureEx) {
            plugin.getLog().error("Invalid signature from player {}", sender, signatureEx);
            return false;
        }
    }

    private void onLoginStart(PacketEvent packetEvent, Player player, String username) {
        //this includes ip:port. Should be unique for an incoming login request with a timeout of 2 minutes
        String sessionKey = player.getAddress().toString();

        //remove old data every time on a new login in order to keep the session only for one person
        plugin.removeSession(player.getAddress());

        if (packetEvent.getPacket().getMeta("original_name").isPresent()) {
            //username has been injected by ManualNameChange.java
            username = (String) packetEvent.getPacket().getMeta("original_name").get();
        }

        PacketContainer packet = packetEvent.getPacket();
        var profileKey = packet.getOptionals(BukkitConverters.getWrappedPublicKeyDataConverter())
                .optionRead(0);

        var clientKey = profileKey.flatMap(opt -> opt).flatMap(this::verifyPublicKey);
        if (verifyClientKeys && !clientKey.isPresent()) {
            // missing or incorrect
            // expired always not allowed
            player.kickPlayer(plugin.getCore().getMessage("invalid-public-key"));
            plugin.getLog().warn("Invalid public key from player {}", username);
            return;
        }

        plugin.getLog().trace("GameProfile {} with {} connecting", sessionKey, username);

        // packetEvent.getAsyncMarker().incrementProcessingDelay();
        Runnable nameCheckTask = new NameCheckTask(
                plugin, random, player, packetEvent, username, clientKey.orElse(null), keyPair.getPublic()
        );
        // plugin.getScheduler().runAsync(nameCheckTask);
        nameCheckTask.run();
    }

    private Optional<ClientPublicKey> verifyPublicKey(WrappedProfileKeyData profileKey) {
        Instant expires = profileKey.getExpireTime();
        PublicKey key = profileKey.getKey();
        byte[] signature = profileKey.getSignature();
        ClientPublicKey clientKey = ClientPublicKey.of(expires, key, signature);
        try {
            if (EncryptionUtil.verifyClientKey(clientKey, Instant.now())) {
                return Optional.of(clientKey);
            }
        } catch (SignatureException | InvalidKeyException | NoSuchAlgorithmException ex) {
            return Optional.empty();
        }

        return Optional.empty();
    }

    private String getUsername(PacketContainer packet) {
        WrappedGameProfile profile = packet.getGameProfiles().readSafely(0);
        if (profile == null) {
            return packet.getStrings().read(0);
        }

        //player.getName() won't work at this state
        return profile.getName();
    }
}
