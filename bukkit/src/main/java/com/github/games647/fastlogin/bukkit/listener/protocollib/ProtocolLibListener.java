package com.github.games647.fastlogin.bukkit.listener.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.github.games647.fastlogin.bukkit.EncryptionUtil;
import com.github.games647.fastlogin.bukkit.FastLoginBukkit;

import java.security.KeyPair;
import java.security.SecureRandom;

import org.bukkit.entity.Player;

import static com.comphenix.protocol.PacketType.Login.Client.ENCRYPTION_BEGIN;
import static com.comphenix.protocol.PacketType.Login.Client.START;

public class ProtocolLibListener extends PacketAdapter {

    private static final int WORKER_THREADS = 3;

    private final FastLoginBukkit plugin;

    //just create a new once on plugin enable. This used for verify token generation
    private final SecureRandom random = new SecureRandom();
    private final KeyPair keyPair = EncryptionUtil.generateKeyPair();

    public ProtocolLibListener(FastLoginBukkit plugin) {
        //run async in order to not block the server, because we are making api calls to Mojang
        super(params()
                .plugin(plugin)
                .types(START, ENCRYPTION_BEGIN)
                .optionAsync());

        this.plugin = plugin;
    }

    public static void register(FastLoginBukkit plugin) {
        //they will be created with a static builder, because otherwise it will throw a NoClassDefFoundError
        ProtocolLibrary.getProtocolManager()
                .getAsynchronousManager()
                .registerAsyncHandler(new ProtocolLibListener(plugin))
                .start(WORKER_THREADS);
    }

    @Override
    public void onPacketReceiving(PacketEvent packetEvent) {
        if (packetEvent.isCancelled()
                || plugin.getCore().getAuthPluginHook()== null
                || !plugin.isServerFullyStarted()) {
            return;
        }

        Player sender = packetEvent.getPlayer();
        PacketType packetType = packetEvent.getPacketType();
        if (packetType == START) {
            onLogin(packetEvent, sender);
        } else {
            onEncryptionBegin(packetEvent, sender);
        }
    }

    private void onEncryptionBegin(PacketEvent packetEvent, Player sender) {
        byte[] sharedSecret = packetEvent.getPacket().getByteArrays().read(0);

        packetEvent.getAsyncMarker().incrementProcessingDelay();
        Runnable verifyTask = new VerifyResponseTask(plugin, packetEvent, sender, sharedSecret, keyPair);
        plugin.getCore().getAsyncScheduler().runAsync(verifyTask);
    }

    private void onLogin(PacketEvent packetEvent, Player player) {
        //this includes ip:port. Should be unique for an incoming login request with a timeout of 2 minutes
        String sessionKey = player.getAddress().toString();

        //remove old data every time on a new login in order to keep the session only for one person
        plugin.getLoginSessions().remove(sessionKey);

        //player.getName() won't work at this state
        PacketContainer packet = packetEvent.getPacket();

        String username = packet.getGameProfiles().read(0).getName();
        plugin.getLog().trace("GameProfile {} with {} connecting", sessionKey, username);

        packetEvent.getAsyncMarker().incrementProcessingDelay();
        Runnable nameCheckTask = new NameCheckTask(plugin, packetEvent, random, player, username, keyPair.getPublic());
        plugin.getCore().getAsyncScheduler().runAsync(nameCheckTask);
    }
}
