package com.github.games647.fastlogin.bukkit.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.injector.server.TemporaryPlayerFactory;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.github.games647.fastlogin.bukkit.EncryptionUtil;
import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.bukkit.PlayerSession;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Level;

import javax.crypto.SecretKey;

import org.bukkit.entity.Player;

/**
 * Handles incoming encryption responses from connecting clients.
 * It prevents them from reaching the server because that cannot handle
 * it in offline mode.
 *
 * Moreover this manages a started premium check from
 * this plugin. So check if all data is correct and we can prove him as a
 * owner of a paid minecraft account.
 *
 * Receiving packet information:
 * http://wiki.vg/Protocol#Encryption_Response
 *
 * sharedSecret=encrypted byte array
 * verify token=encrypted byte array
 */
public class EncryptionPacketListener extends PacketAdapter {

    private final ProtocolManager protocolManager;
    //hides the inherit Plugin plugin field, but we need this type
    private final FastLoginBukkit plugin;

    public EncryptionPacketListener(FastLoginBukkit plugin, ProtocolManager protocolManger) {
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
        System.out.println("ENCRYPTION REQUEST");
        Player player = packetEvent.getPlayer();

        //the player name is unknown to ProtocolLib (so getName() doesn't work) - now uses ip:port as key
        String uniqueSessionKey = player.getAddress().toString();
        PlayerSession session = plugin.getSessions().get(uniqueSessionKey);
        if (session == null) {
            disconnect(packetEvent, "Invalid request", Level.FINE
                    , "Player {0} tried to send encryption response at invalid state"
                    , player.getAddress());
            return;
        }

        PrivateKey privateKey = plugin.getServerKey().getPrivate();

        byte[] sharedSecret = packetEvent.getPacket().getByteArrays().read(0);
        SecretKey loginKey = EncryptionUtil.decryptSharedKey(privateKey, sharedSecret);
        if (!checkVerifyToken(session, privateKey, packetEvent) || !encryptConnection(player, loginKey, packetEvent)) {
            return;
        }

        //this makes sure the request from the client is for us
        //this might be relevant http://www.sk89q.com/2011/09/minecraft-name-spoofing-exploit/
        String generatedId = session.getServerId();

        //https://github.com/bergerkiller/CraftSource/blob/master/net.minecraft.server/LoginListener.java#L193
        //generate the server id based on client and server data
        byte[] serverIdHash = EncryptionUtil.getServerIdHash(generatedId, plugin.getServerKey().getPublic(), loginKey);
        String serverId = (new BigInteger(serverIdHash)).toString(16);

        String username = session.getUsername();
        if (plugin.getApiConnector().hasJoinedServer(session, serverId)) {
            plugin.getLogger().log(Level.FINE, "Player {0} has a verified premium account", username);

            session.setVerified(true);
            setPremiumUUID(session, player);
            receiveFakeStartPacket(username, player);
        } else {
            //user tried to fake a authentication
            disconnect(packetEvent, "Invalid session", Level.FINE
                    , "Player {0} ({1}) tried to log in with an invalid session ServerId: {2}"
                    , session.getUsername(), player.getAddress(), serverId);
        }

        //this is a fake packet; it shouldn't be send to the server
        packetEvent.setCancelled(true);
    }

    private void setPremiumUUID(PlayerSession session, Player player) {
        UUID uuid = session.getUuid();
        if (plugin.getConfig().getBoolean("premiumUuid") && uuid != null) {
            try {
                Object networkManager = getNetworkManager(player);
                //https://github.com/bergerkiller/CraftSource/blob/master/net.minecraft.server/NetworkManager.java#L69
                Field spoofField = FuzzyReflection.fromObject(networkManager).getFieldByType("spoofedUUID", UUID.class);
                spoofField.set(networkManager, uuid);
            } catch (ReflectiveOperationException reflectiveOperationException) {
                plugin.getLogger().log(Level.SEVERE, "Error setting premium uuid", reflectiveOperationException);
            }
        }
    }

    private boolean checkVerifyToken(PlayerSession session, PrivateKey privateKey, PacketEvent packetEvent) {
        byte[] requestVerify = session.getVerifyToken();
        //encrypted verify token
        byte[] responseVerify = packetEvent.getPacket().getByteArrays().read(1);

        //https://github.com/bergerkiller/CraftSource/blob/master/net.minecraft.server/LoginListener.java#L182
        if (!Arrays.equals(requestVerify, EncryptionUtil.decryptData(privateKey, responseVerify))) {
            //check if the verify token are equal to the server sent one
            disconnect(packetEvent, "Invalid token", Level.FINE
                    , "Player {0} ({1}) tried to login with an invalid verify token. "
                            + "Server: {2} Client: {3}"
                    , session.getUsername(), packetEvent.getPlayer().getAddress(), requestVerify, responseVerify);
            return false;
        }

        return true;
    }

    //try to get the networkManager from ProtocolLib
    private Object getNetworkManager(Player player)
            throws IllegalAccessException, NoSuchFieldException {
        Object socketInjector = TemporaryPlayerFactory.getInjectorFromPlayer(player);
        Field injectorField = socketInjector.getClass().getDeclaredField("injector");
        injectorField.setAccessible(true);

        Object rawInjector = injectorField.get(socketInjector);

        injectorField = rawInjector.getClass().getDeclaredField("networkManager");
        injectorField.setAccessible(true);
        return injectorField.get(rawInjector);
    }

    private boolean encryptConnection(Player player, SecretKey loginKey, PacketEvent packetEvent)
            throws IllegalArgumentException {
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
            return false;
        }

        return true;
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

    //fake a new login packet in order to let the server handle all the other stuff
    private void receiveFakeStartPacket(String username, Player from) {
        //see StartPacketListener for packet information
        PacketContainer startPacket = protocolManager.createPacket(PacketType.Login.Client.START);

        //uuid is ignored by the packet definition
        WrappedGameProfile fakeProfile = new WrappedGameProfile(UUID.randomUUID(), username);
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
