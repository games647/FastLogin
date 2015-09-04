package com.github.games647.fastlogin.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.github.games647.fastlogin.FastLogin;
import com.github.games647.fastlogin.PlayerData;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.security.PublicKey;

import java.util.Random;
import java.util.logging.Level;

import org.bukkit.entity.Player;

public class StartPacketListener extends PacketAdapter {

    //only premium members have a uuid from there
    private static final String UUID_LINK = "https://api.mojang.com/users/profiles/minecraft/";

    private final ProtocolManager protocolManager;
    private final FastLogin fastLogin;

    private final Random random = new Random();

    public StartPacketListener(FastLogin plugin, ProtocolManager protocolManger) {
        super(params(plugin, PacketType.Login.Client.START).optionAsync());

        this.fastLogin = plugin;
        this.protocolManager = protocolManger;
    }

    /*
     * C->S : Handshake State=2
     * C->S : Login Start
     * S->C : Encryption Key Request
     * (Client Auth)
     * C->S : Encryption Key Response
     * (Server Auth, Both enable encryption)
     * S->C : Login Success (*)
     */
    @Override
    public void onPacketReceiving(PacketEvent packetEvent) {
        PacketContainer packet = packetEvent.getPacket();
        Player player = packetEvent.getPlayer();

        String username = packet.getGameProfiles().read(0).getName();
        if (isPremium(username)) {
            //do premium login process
            try {
                PacketContainer newPacket = protocolManager.createPacket(PacketType.Login.Server.ENCRYPTION_BEGIN, true);

                //constr ServerID=""
                //public key=plugin.getPublic
                newPacket.getSpecificModifier(PublicKey.class).write(0, fastLogin.getKeyPair().getPublic());
                byte[] verifyToken = new byte[4];
                random.nextBytes(verifyToken);
                newPacket.getByteArrays().write(0, verifyToken);

                String addressString = player.getAddress().toString();
                fastLogin.getSession().asMap().put(addressString, new PlayerData(verifyToken, username));

                protocolManager.sendServerPacket(player, newPacket, false);
            } catch (InvocationTargetException ex) {
                plugin.getLogger().log(Level.SEVERE, null, ex);
            }

            //cancel only if the player is premium
            packetEvent.setCancelled(true);
        }
    }

    private boolean isPremium(String playerName) {
        try {
            final HttpURLConnection connection = fastLogin.getConnection(UUID_LINK + playerName);
            final int responseCode = connection.getResponseCode();

            return responseCode == HttpURLConnection.HTTP_OK;
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, null, ex);
        }

        return false;
    }
}
