package com.github.games647.fastlogin.bukkit.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.bukkit.PlayerSession;
import com.github.games647.fastlogin.bukkit.hooks.BukkitAuthPlugin;

import java.lang.reflect.InvocationTargetException;
import java.security.PublicKey;
import java.util.Random;
import java.util.logging.Level;

import org.bukkit.entity.Player;

/**
 * Handles incoming start packets from connecting clients. It
 * checks if we can start checking if the player is premium and
 * start a request to the client that it should start online mode
 * login.
 *
 * Receiving packet information:
 * http://wiki.vg/Protocol#Login_Start
 *
 * String=Username
 */
public class StartPacketListener extends PacketAdapter {

    private static final int VERIFY_TOKEN_LENGTH = 4;

    private final ProtocolManager protocolManager;
    //hides the inherit Plugin plugin field, but we need a more detailed type than just Plugin
    private final FastLoginBukkit plugin;

    //just create a new once on plugin enable. This used for verify token generation
    private final Random random = new Random();

    public StartPacketListener(FastLoginBukkit plugin, ProtocolManager protocolManger) {
        //run async in order to not block the server, because we are making api calls to Mojang
        super(params(plugin, PacketType.Login.Client.START).optionAsync());

        this.plugin = plugin;
        this.protocolManager = protocolManger;
    }

    /**
     * C->S : Handshake State=2
     * C->S : Login Start
     * S->C : Encryption Key Request
     * (Client Auth)
     * C->S : Encryption Key Response
     * (Server Auth, Both enable encryption)
     * S->C : Login Success (*)
     *
     * On offline logins is Login Start followed by Login Success
     */
    @Override
    public void onPacketReceiving(PacketEvent packetEvent) {
        final Player player = packetEvent.getPlayer();

        //this includes ip:port. Should be unique for an incoming login request with a timeout of 2 minutes
        String sessionKey = player.getAddress().toString();

        //remove old data every time on a new login in order to keep the session only for one person
        plugin.getSessions().remove(sessionKey);

        //player.getName() won't work at this state
        PacketContainer packet = packetEvent.getPacket();
        String username = packet.getGameProfiles().read(0).getName();
        plugin.getLogger().log(Level.FINER, "Player {0} with {1} connecting to the server"
                , new Object[]{sessionKey, username});
        if (!plugin.getBungeeCordUsers().containsKey(player)) {
            BukkitAuthPlugin authPlugin = plugin.getAuthPlugin();
            if (plugin.getEnabledPremium().contains(username)) {
                enablePremiumLogin(username, sessionKey, player, packetEvent, true);
            } else if (plugin.getConfig().getBoolean("autoRegister")
                    && authPlugin != null && !plugin.getAuthPlugin().isRegistered(username)) {
                enablePremiumLogin(username, sessionKey, player, packetEvent, false);
                plugin.getEnabledPremium().add(username);
            }
        }
    }

    private void enablePremiumLogin(String username, String sessionKey, Player player, PacketEvent packetEvent
            , boolean registered) {
        if (plugin.getApiConnector().isPremiumName(username)) {
            plugin.getLogger().log(Level.FINER, "Player {0} uses a premium username", username);
            //minecraft server implementation
            //https://github.com/bergerkiller/CraftSource/blob/master/net.minecraft.server/LoginListener.java#L161

            //randomized server id to make sure the request is for our server
            //this could be relevant http://www.sk89q.com/2011/09/minecraft-name-spoofing-exploit/
            String serverId = Long.toString(random.nextLong(), 16);

            //generate a random token which should be the same when we receive it from the client
            byte[] verifyToken = new byte[VERIFY_TOKEN_LENGTH];
            random.nextBytes(verifyToken);

            boolean success = sentEncryptionRequest(player, serverId, verifyToken);
            if (success) {
                PlayerSession playerSession = new PlayerSession(username, serverId, verifyToken);
                playerSession.setRegistered(registered);
                plugin.getSessions().put(sessionKey, playerSession);
                //cancel only if the player has a paid account otherwise login as normal offline player
                packetEvent.setCancelled(true);
            }
        }
    }

    private boolean sentEncryptionRequest(Player player, String serverId, byte[] verifyToken) {
        try {
            /**
             * Packet Information: http://wiki.vg/Protocol#Encryption_Request
             *
             * ServerID="" (String)
             * key=public server key
             * verifyToken=random 4 byte array
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
