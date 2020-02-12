package com.github.games647.fastlogin.bukkit.listener.protocollib;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.github.games647.fastlogin.bukkit.EncryptionUtil;
import com.github.games647.fastlogin.core.shared.LoginSource;

import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Random;

import org.bukkit.entity.Player;

import static com.comphenix.protocol.PacketType.Login.Server.DISCONNECT;
import static com.comphenix.protocol.PacketType.Login.Server.ENCRYPTION_BEGIN;

public class ProtocolLibLoginSource implements LoginSource {

    private final PacketEvent packetEvent;
    private final Player player;

    private final Random random;
    private final PublicKey publicKey;

    private final String serverId = "";
    private byte[] verifyToken;

    public ProtocolLibLoginSource(PacketEvent packetEvent, Player player, Random random, PublicKey publicKey) {
        this.packetEvent = packetEvent;
        this.player = player;
        this.random = random;
        this.publicKey = publicKey;
    }

    @Override
    public void setOnlineMode() throws Exception {
        verifyToken = EncryptionUtil.generateVerifyToken(random);

        /*
         * Packet Information: https://wiki.vg/Protocol#Encryption_Request
         *
         * ServerID="" (String) key=public server key verifyToken=random 4 byte array
         */
        PacketContainer newPacket = new PacketContainer(ENCRYPTION_BEGIN);

        newPacket.getStrings().write(0, serverId);
        newPacket.getSpecificModifier(PublicKey.class).write(0, publicKey);

        newPacket.getByteArrays().write(0, verifyToken);

        //serverId is a empty string
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
        return packetEvent.getPlayer().getAddress();
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
                "packetEvent=" + packetEvent +
                ", player=" + player +
                ", random=" + random +
                ", serverId='" + serverId + '\'' +
                ", verifyToken=" + Arrays.toString(verifyToken) +
                '}';
    }
}
