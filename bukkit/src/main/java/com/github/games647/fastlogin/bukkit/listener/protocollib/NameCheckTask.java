package com.github.games647.fastlogin.bukkit.listener.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.github.games647.fastlogin.bukkit.BukkitLoginSession;
import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.core.PlayerProfile;

import java.lang.reflect.InvocationTargetException;
import java.security.PublicKey;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.entity.Player;

public class NameCheckTask implements Runnable {

    private static final int VERIFY_TOKEN_LENGTH = 4;

    private final FastLoginBukkit plugin;
    private final PacketEvent packetEvent;

    private final Random random;

    private final Player player;
    private final String username;

    public NameCheckTask(FastLoginBukkit plugin, PacketEvent packetEvent, Random random, Player player, String username) {
        this.plugin = plugin;
        this.packetEvent = packetEvent;
        this.random = random;
        this.player = player;
        this.username = username;
    }

    @Override
    public void run() {
        try {
            nameCheck();
        } finally {
            ProtocolLibrary.getProtocolManager().getAsynchronousManager().signalPacketTransmission(packetEvent);
        }
    }

    private void nameCheck() {
        PlayerProfile profile = plugin.getCore().getStorage().loadProfile(username);
        if (profile == null) {
            return;
        }

        if (profile.getUserId() == -1) {
            UUID premiumUUID = null;
            
            //user not exists in the db
            try {
                if (plugin.getConfig().getBoolean("nameChangeCheck") || (plugin.getConfig().getBoolean("autoRegister")
                        && plugin.getAuthPlugin().isRegistered(username))) {
                    premiumUUID = plugin.getCore().getMojangApiConnector().getPremiumUUID(username);
                }

                if (premiumUUID != null && plugin.getConfig().getBoolean("nameChangeCheck")) {
                    PlayerProfile uuidProfile = plugin.getCore().getStorage().loadProfile(premiumUUID);
                    if (uuidProfile != null) {
                        plugin.getLogger().log(Level.FINER, "Player {0} changed it's username", premiumUUID);
                        enablePremiumLogin(uuidProfile, false);
                        return;
                    }
                }

                if (premiumUUID != null && plugin.getConfig().getBoolean("autoRegister")) {
                    plugin.getLogger().log(Level.FINER, "Player {0} uses a premium username", username);
                    enablePremiumLogin(profile, false);
                    return;
                }

                //no premium check passed so we save it as a cracked player
                BukkitLoginSession loginSession = new BukkitLoginSession(username, profile);
                plugin.getSessions().put(player.getAddress().toString(), loginSession);
            } catch (Exception ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to query isRegistered", ex);
            }
        } else if (profile.isPremium()) {
            enablePremiumLogin(profile, true);
        } else {
            BukkitLoginSession loginSession = new BukkitLoginSession(username, profile);
            plugin.getSessions().put(player.getAddress().toString(), loginSession);
        }
    }

    //minecraft server implementation
    //https://github.com/bergerkiller/CraftSource/blob/master/net.minecraft.server/LoginListener.java#L161
    private void enablePremiumLogin(PlayerProfile profile, boolean registered) {
        //randomized server id to make sure the request is for our server
        //this could be relevant http://www.sk89q.com/2011/09/minecraft-name-spoofing-exploit/
        String serverId = Long.toString(random.nextLong(), 16);

        //generate a random token which should be the same when we receive it from the client
        byte[] verify = new byte[VERIFY_TOKEN_LENGTH];
        random.nextBytes(verify);

        boolean success = sentEncryptionRequest(player, serverId, verify);
        if (success) {
            BukkitLoginSession playerSession = new BukkitLoginSession(username, serverId, verify, registered, profile);
            plugin.getSessions().put(player.getAddress().toString(), playerSession);
            //cancel only if the player has a paid account otherwise login as normal offline player
            synchronized (packetEvent.getAsyncMarker().getProcessingLock()) {
                packetEvent.setCancelled(true);
            }
        }
    }

    private boolean sentEncryptionRequest(Player player, String serverId, byte[] verifyToken) {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        try {
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
            return true;
        } catch (InvocationTargetException ex) {
            plugin.getLogger().log(Level.SEVERE, "Cannot send encryption packet. Falling back to normal login", ex);
        }

        return false;
    }
}
