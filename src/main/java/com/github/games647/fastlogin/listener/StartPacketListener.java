package com.github.games647.fastlogin.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.github.games647.fastlogin.FastLogin;
import com.github.games647.fastlogin.PlayerSession;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.security.PublicKey;

import java.util.Random;
import java.util.logging.Level;
import java.util.regex.Pattern;

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

    //only premium (paid account) users have a uuid from here
    private static final String UUID_LINK = "https://api.mojang.com/users/profiles/minecraft/";
    //this includes a-zA-Z1-9_
    private static final String VALID_PLAYERNAME = "^\\w{2,16}$";
    private static final int VERIFY_TOKEN_LENGTH = 4;

    private final ProtocolManager protocolManager;
    //hides the inherit Plugin plugin field, but we need a more detailed type than just Plugin
    private final FastLogin plugin;

    //just create a new once on plugin enable. This used for verify token generation
    private final Random random = new Random();
    //compile the pattern on plugin enable
    private final Pattern playernameMatcher = Pattern.compile(VALID_PLAYERNAME);

    public StartPacketListener(FastLogin plugin, ProtocolManager protocolManger) {
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
        Player player = packetEvent.getPlayer();

        //this includes ip:port. Should be unique for an incoming login request with a timeout of 2 minutes
        String sessionKey = player.getAddress().toString();

        //remove old data every time on a new login in order to keep the session only for one person
        plugin.getSessions().remove(sessionKey);

        //player.getName() won't work at this state
        PacketContainer packet = packetEvent.getPacket();
        String username = packet.getGameProfiles().read(0).getName();
        plugin.getLogger().log(Level.FINER, "Player {0} with {1} connecting to the server"
                , new Object[]{sessionKey, username});
        if (plugin.getEnabledPremium().contains(username) && isPremiumName(username)) {
            //minecraft server implementation
            //https://github.com/bergerkiller/CraftSource/blob/master/net.minecraft.server/LoginListener.java#L161
            sentEncryptionRequest(sessionKey, username, player, packetEvent);
        }
    }

    private boolean isPremiumName(String playerName) {
        //check if it's a valid playername
        if (playernameMatcher.matcher(playerName).matches()) {
            //only make a API call if the name is valid existing mojang account
            try {
                HttpURLConnection connection = plugin.getConnection(UUID_LINK + playerName);
                int responseCode = connection.getResponseCode();

                return responseCode == HttpURLConnection.HTTP_OK;
                //204 - no content for not found
            } catch (IOException ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to check if player has a paid account", ex);
            }
            //this connection doesn't need to be closed. So can make use of keep alive in java
        }

        return false;
    }

    private void sentEncryptionRequest(String sessionKey, String username, Player player, PacketEvent packetEvent) {
        plugin.getLogger().log(Level.FINER, "Player {0} uses a premium username", username);
        try {
            /**
             * Packet Information: http://wiki.vg/Protocol#Encryption_Request
             *
             * ServerID="" (String)
             * key=public server key
             * verifyToken=random 4 byte array
             */
            PacketContainer newPacket = protocolManager.createPacket(PacketType.Login.Server.ENCRYPTION_BEGIN, true);

            newPacket.getSpecificModifier(PublicKey.class).write(0, plugin.getServerKey().getPublic());
            //generate a random token which should be the same when we receive it from the client
            byte[] verifyToken = new byte[VERIFY_TOKEN_LENGTH];
            random.nextBytes(verifyToken);
            newPacket.getByteArrays().write(0, verifyToken);

            //serverId is a empty string
            protocolManager.sendServerPacket(player, newPacket);

            //cancel only if the player has a paid account otherwise login as normal offline player
            plugin.getSessions().put(sessionKey, new PlayerSession(verifyToken, username));
            packetEvent.setCancelled(true);
        } catch (InvocationTargetException ex) {
            plugin.getLogger().log(Level.SEVERE, "Cannot send encryption packet. Falling back to normal login", ex);
        }
    }
}
