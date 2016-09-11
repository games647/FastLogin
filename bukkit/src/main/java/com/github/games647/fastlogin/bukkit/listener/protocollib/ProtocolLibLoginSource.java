package com.github.games647.fastlogin.bukkit.listener.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.core.shared.LoginSource;
import java.lang.reflect.InvocationTargetException;

import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.util.Random;

import org.bukkit.entity.Player;

public class ProtocolLibLoginSource implements LoginSource {

    private static final int VERIFY_TOKEN_LENGTH = 4;

    private final FastLoginBukkit plugin;

    private final PacketEvent packetEvent;
    private final Player player;

    private final Random random;

    private String serverId;
    private byte[] verifyToken = new byte[VERIFY_TOKEN_LENGTH];

    public ProtocolLibLoginSource(FastLoginBukkit plugin, PacketEvent packetEvent, Player player, Random random) {
        this.plugin = plugin;
        this.packetEvent = packetEvent;
        this.player = player;
        this.random = random;
    }

    @Override
    public void setOnlineMode() throws Exception {
        //randomized server id to make sure the request is for our server
        //this could be relevant http://www.sk89q.com/2011/09/minecraft-name-spoofing-exploit/
        serverId = Long.toString(random.nextLong(), 16);

        //generate a random token which should be the same when we receive it from the client
        random.nextBytes(verifyToken);
        
        sentEncryptionRequest();
    }

    @Override
    public void kick(String message) throws Exception {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

        PacketContainer kickPacket = protocolManager.createPacket(PacketType.Login.Server.DISCONNECT);
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
        /**
         * Packet Information: http://wiki.vg/Protocol#Encryption_Request
         *
         * ServerID="" (String) key=public server key verifyToken=random 4 byte array
         */
        PacketContainer newPacket = protocolManager.createPacket(PacketType.Login.Server.ENCRYPTION_BEGIN);

        newPacket.getStrings().write(0, serverId);
        newPacket.getSpecificModifier(PublicKey.class).write(0, plugin.getServerKey().getPublic());

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
