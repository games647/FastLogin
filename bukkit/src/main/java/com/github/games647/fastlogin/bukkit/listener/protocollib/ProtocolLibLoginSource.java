package com.github.games647.fastlogin.bukkit.listener.protocollib;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.github.games647.fastlogin.bukkit.EncryptionUtil;
import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.core.shared.LoginSource;

import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.util.Random;

import org.bukkit.entity.Player;

import static com.comphenix.protocol.PacketType.Login.Server.DISCONNECT;
import static com.comphenix.protocol.PacketType.Login.Server.ENCRYPTION_BEGIN;

public class ProtocolLibLoginSource implements LoginSource {

    private final FastLoginBukkit plugin;

    private final PacketEvent packetEvent;
    private final Player player;

    private final Random random;

    private String serverId;
    private byte[] verifyToken;

    public ProtocolLibLoginSource(FastLoginBukkit plugin, PacketEvent packetEvent, Player player, Random random) {
        this.plugin = plugin;
        this.packetEvent = packetEvent;
        this.player = player;
        this.random = random;
    }

    @Override
    public void setOnlineMode() throws Exception {
        //randomized server id to make sure the request is for our server
        //this could be relevant https://www.sk89q.com/2011/09/minecraft-name-spoofing-exploit/
        serverId = Long.toString(random.nextLong(), 16);
        verifyToken = EncryptionUtil.generateVerifyToken(random);
        
        sentEncryptionRequest();
    }

    @Override
    public void kick(String message) throws InvocationTargetException {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

        PacketContainer kickPacket = protocolManager.createPacket(DISCONNECT);
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
        return packetEvent.getPlayer().getAddress();
    }

    private void sentEncryptionRequest() throws InvocationTargetException {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        /*
         * Packet Information: http://wiki.vg/Protocol#Encryption_Request
         *
         * ServerID="" (String) key=public server key verifyToken=random 4 byte array
         */
        PacketContainer newPacket = protocolManager.createPacket(ENCRYPTION_BEGIN);

        newPacket.getStrings().write(0, serverId);
        PublicKey publicKey = plugin.getServerKey().getPublic();
        newPacket.getSpecificModifier(PublicKey.class).write(0, publicKey);

        newPacket.getByteArrays().write(0, verifyToken);

        //serverId is a empty string
        protocolManager.sendServerPacket(player, newPacket);
    }

    public String getServerId() {
        return serverId;
    }

    public byte[] getVerifyToken() {
        return verifyToken;
    }
}
