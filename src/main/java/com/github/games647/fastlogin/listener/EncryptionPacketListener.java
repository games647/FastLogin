package com.github.games647.fastlogin.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.injector.server.TemporaryPlayerFactory;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.github.games647.fastlogin.Encryption;
import com.github.games647.fastlogin.FastLogin;
import com.github.games647.fastlogin.PlayerSession;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.logging.Level;

import javax.crypto.SecretKey;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * Receiving packet information:
 * http://wiki.vg/Protocol#Encryption_Response
 *
 * sharedSecret=encrypted byte array
 * verify token=encrypted byte array
 */
public class EncryptionPacketListener extends PacketAdapter {

    private static final String HAS_JOINED_URL = "https://sessionserver.mojang.com/session/minecraft/hasJoined?";

    private final ProtocolManager protocolManager;
    //hides the inherit Plugin plugin field, but we need this type
    private final FastLogin plugin;

    public EncryptionPacketListener(FastLogin plugin, ProtocolManager protocolManger) {
        //run async in order to not block the server, because we make api calls to Mojang
        super(params(plugin, PacketType.Login.Client.ENCRYPTION_BEGIN).optionAsync());

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
     *
     * Minecraft Server implementation
     * https://github.com/bergerkiller/CraftSource/blob/master/net.minecraft.server/LoginListener.java#L180
     */
    @Override
    public void onPacketReceiving(PacketEvent packetEvent) {
        PacketContainer packet = packetEvent.getPacket();
        Player player = packetEvent.getPlayer();

        //the player name is unknown to ProtocolLib - now uses ip:port as key
        String uniqueSessionKey = player.getAddress().toString();
        PlayerSession session = plugin.getSessions().get(uniqueSessionKey);
        if (session == null) {
            disconnect(packetEvent, "Invalid request", Level.FINE
                    , "Player {0} tried to send encryption response"
                            + "on an invalid connection state"
                    , player.getAddress());
            return;
        }

        byte[] sharedSecret = packet.getByteArrays().read(0);
        byte[] clientVerify = packet.getByteArrays().read(1);

        PrivateKey privateKey = plugin.getKeyPair().getPrivate();
        byte[] serverVerify = session.getVerifyToken();
        //https://github.com/bergerkiller/CraftSource/blob/master/net.minecraft.server/LoginListener.java#L182
        if (!Arrays.equals(serverVerify, Encryption.decryptData(privateKey, clientVerify))) {
            //check if the verify token are equal to the server sent one
            disconnect(packetEvent, "Invalid token", Level.FINE
                    , "Player {0} ({1}) tried to login with an invalid verify token. "
                            + "Server: {2} Client: {3}"
                    , session.getUsername(), player.getAddress(), serverVerify, clientVerify);
            return;
        }

        SecretKey loginKey = Encryption.decryptSharedKey(privateKey, sharedSecret);
        try {
            //get the NMS connection handle of this player
            Object networkManager = getNetworkManager(player);

            //try to detect the method by parameters
            Method encryptConnectionMethod = FuzzyReflection.fromObject(networkManager)
                    .getMethodByParameters("a", SecretKey.class);

            //encrypt/decrypt following packets
            //the client expects this behaviour
            encryptConnectionMethod.invoke(networkManager, loginKey);
        } catch (ReflectiveOperationException ex) {
            disconnect(packetEvent, "Error occurred", Level.SEVERE, "Couldn't enable encryption", ex);
            return;
        }

        //https://github.com/bergerkiller/CraftSource/blob/master/net.minecraft.server/LoginListener.java#L193
        //generate the server id based on client and server data
        String serverId = (new BigInteger(Encryption.getServerIdHash("", plugin.getKeyPair().getPublic(), loginKey)))
                .toString(16);

        String username = session.getUsername();
        if (hasJoinedServer(username, serverId)) {
            session.setVerified(true);

            plugin.getLogger().log(Level.FINE, "Player {0} has a verified premium account", username);

            receiveFakeStartPacket(username, player);
        } else {
            //user tried to fake a authentification
            disconnect(packetEvent, "Invalid session", Level.FINE
                    , "Player {0} ({1}) tried to log in with an invalid session ServerId: {2}"
                    , session.getUsername(), player.getAddress(), serverId);
        }

        packetEvent.setCancelled(true);
    }

    private void disconnect(PacketEvent packetEvent, String kickReason, Level logLevel, String logMessage
            , Object... arguments) {
        plugin.getLogger().log(logLevel, logMessage, arguments);
        kickPlayer(packetEvent.getPlayer(), kickReason);
        //cancel the event in order to prevent the server receiving an invalid packet
        packetEvent.setCancelled(true);
    }

    private void kickPlayer(Player player, String reason) {
        PacketContainer kickPacket = protocolManager.createPacket(PacketType.Login.Server.DISCONNECT);
        kickPacket.getChatComponents().write(0, WrappedChatComponent.fromText(reason));

        try {
            //send kick packet at login state
            //the normal event.getPlayer.kickPlayer(String) method does only work at play state
            protocolManager.sendServerPacket(player, kickPacket);
            //tell the server that we want to close the connection
            player.kickPlayer("Disconnect");
        } catch (InvocationTargetException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error sending kickpacket", ex);
        }
    }

    private Object getNetworkManager(Player player)
            throws SecurityException, IllegalAccessException, NoSuchFieldException {
        Object injector = TemporaryPlayerFactory.getInjectorFromPlayer(player);
        Field injectorField = injector.getClass().getDeclaredField("injector");
        injectorField.setAccessible(true);

        Object rawInjector = injectorField.get(injector);

        injectorField = rawInjector.getClass().getDeclaredField("networkManager");
        injectorField.setAccessible(true);
        return injectorField.get(rawInjector);
    }

    private boolean hasJoinedServer(String username, String serverId) {
        try {
            String url = HAS_JOINED_URL + "username=" + username + "&serverId=" + serverId;

            HttpURLConnection conn = plugin.getConnection(url);

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = reader.readLine();
            if (!line.equals("null")) {
                //validate parsing
                JSONObject object = (JSONObject) JSONValue.parseWithException(line);
                String uuid = (String) object.get("id");
                String name = (String) object.get("name");

                return true;
            }
        } catch (Exception ex) {
            //catch not only ioexceptions also parse and NPE on unexpected json format
            plugin.getLogger().log(Level.WARNING, "Failed to verify if session is valid", ex);
        }

        //this connection doesn't need to be closed. So can make use of keep alive in java
        return false;
    }

    private void receiveFakeStartPacket(String username, Player from) {
        //fake a new login packet
        //see StartPacketListener for packet information
        PacketContainer startPacket = protocolManager.createPacket(PacketType.Login.Client.START, true);

        WrappedGameProfile fakeProfile = WrappedGameProfile.fromOfflinePlayer(Bukkit.getOfflinePlayer(username));
        startPacket.getGameProfiles().write(0, fakeProfile);
        try {
            //we don't want to handle our own packets so ignore filters
            protocolManager.recieveClientPacket(from, startPacket, false);
        } catch (InvocationTargetException | IllegalAccessException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to fake a new start packet", ex);
            //cancel the event in order to prevent the server receiving an invalid packet
            kickPlayer(from, "Error occured");
        }
    }
}
