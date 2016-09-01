package com.github.games647.fastlogin.bukkit.listener.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.injector.server.TemporaryPlayerFactory;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.github.games647.fastlogin.bukkit.BukkitLoginSession;
import com.github.games647.fastlogin.bukkit.EncryptionUtil;
import com.github.games647.fastlogin.bukkit.FastLoginBukkit;

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

public class VerifyResponseTask implements Runnable {

    private final FastLoginBukkit plugin;
    private final PacketEvent packetEvent;

    private final Player fromPlayer;

    private final byte[] sharedSecret;

    public VerifyResponseTask(FastLoginBukkit plugin, PacketEvent packetEvent, Player fromPlayer, byte[] sharedSecret) {
        this.plugin = plugin;
        this.packetEvent = packetEvent;
        this.fromPlayer = fromPlayer;
        this.sharedSecret = sharedSecret;
    }

    @Override
    public void run() {
        try {
            BukkitLoginSession session = plugin.getSessions().get(fromPlayer.getAddress().toString());
            if (session == null) {
                disconnect(plugin.getCore().getMessage("invalid-requst"), true
                        , "Player {0} tried to send encryption response at invalid state", fromPlayer.getAddress());
            } else {
                String ip = fromPlayer.getAddress().getAddress().getHostAddress();
                plugin.getCore().getPendingLogins().remove(ip + session.getUsername());

                verifyResponse(session);
            }
        } finally {
            ProtocolLibrary.getProtocolManager().getAsynchronousManager().signalPacketTransmission(packetEvent);
        }
    }

    private void verifyResponse(BukkitLoginSession session) {
        PrivateKey privateKey = plugin.getServerKey().getPrivate();

        SecretKey loginKey = EncryptionUtil.decryptSharedKey(privateKey, sharedSecret);
        if (!checkVerifyToken(session, privateKey) || !encryptConnection(loginKey)) {
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
        if (plugin.getCore().getMojangApiConnector().hasJoinedServer(session, serverId)) {
            plugin.getLogger().log(Level.FINE, "Player {0} has a verified premium account", username);

            session.setVerified(true);
            setPremiumUUID(session.getUuid());
            receiveFakeStartPacket(username);
        } else {
            //user tried to fake a authentication
            disconnect(plugin.getCore().getMessage("invalid-session"), true
                    , "Player {0} ({1}) tried to log in with an invalid session ServerId: {2}"
                    , session.getUsername(), fromPlayer.getAddress(), serverId);
        }

        //this is a fake packet; it shouldn't be send to the server
        synchronized (packetEvent.getAsyncMarker().getProcessingLock()) {
            packetEvent.setCancelled(true);
        }
    }

    private void setPremiumUUID(UUID premiumUUID) {
        if (plugin.getConfig().getBoolean("premiumUuid") && premiumUUID != null) {
            try {
                Object networkManager = getNetworkManager();
                //https://github.com/bergerkiller/CraftSource/blob/master/net.minecraft.server/NetworkManager.java#L69
                Field spoofField = FuzzyReflection.fromObject(networkManager).getFieldByType("spoofedUUID", UUID.class);
                spoofField.set(networkManager, premiumUUID);
            } catch (ReflectiveOperationException reflectiveOperationException) {
                plugin.getLogger().log(Level.SEVERE, "Error setting premium uuid", reflectiveOperationException);
            }
        }
    }

    private boolean checkVerifyToken(BukkitLoginSession session, PrivateKey privateKey) {
        byte[] requestVerify = session.getVerifyToken();
        //encrypted verify token
        byte[] responseVerify = packetEvent.getPacket().getByteArrays().read(1);

        //https://github.com/bergerkiller/CraftSource/blob/master/net.minecraft.server/LoginListener.java#L182
        if (!Arrays.equals(requestVerify, EncryptionUtil.decryptData(privateKey, responseVerify))) {
            //check if the verify token are equal to the server sent one
            disconnect(plugin.getCore().getMessage("invalid-verify-token"), true
                    , "Player {0} ({1}) tried to login with an invalid verify token. Server: {2} Client: {3}"
                    , session.getUsername(), packetEvent.getPlayer().getAddress(), requestVerify, responseVerify);
            return false;
        }

        return true;
    }

    //try to get the networkManager from ProtocolLib
    private Object getNetworkManager() throws IllegalAccessException, NoSuchFieldException {
        Object socketInjector = TemporaryPlayerFactory.getInjectorFromPlayer(fromPlayer);
        Field injectorField = socketInjector.getClass().getDeclaredField("injector");
        injectorField.setAccessible(true);

        Object rawInjector = injectorField.get(socketInjector);

        injectorField = rawInjector.getClass().getDeclaredField("networkManager");
        injectorField.setAccessible(true);
        return injectorField.get(rawInjector);
    }

    private boolean encryptConnection(SecretKey loginKey) throws IllegalArgumentException {
        try {
            //get the NMS connection handle of this player
            Object networkManager = getNetworkManager();

            //try to detect the method by parameters
            Method encryptConnectionMethod = FuzzyReflection
                    .fromObject(networkManager).getMethodByParameters("a", SecretKey.class);

            //encrypt/decrypt following packets
            //the client expects this behaviour
            encryptConnectionMethod.invoke(networkManager, loginKey);
        } catch (Exception ex) {
            plugin.getLogger().log(Level.SEVERE, "Couldn't enable encryption", ex);
            disconnect(plugin.getCore().getMessage("error-kick"), false, "Couldn't enable encryption");
            return false;
        }

        return true;
    }

    private void disconnect(String kickReason, boolean debug, String logMessage, Object... arguments) {
        if (debug) {
            plugin.getLogger().log(Level.FINE, logMessage, arguments);
        } else {
            plugin.getLogger().log(Level.SEVERE, logMessage, arguments);
        }

        kickPlayer(packetEvent.getPlayer(), kickReason);
        //cancel the event in order to prevent the server receiving an invalid packet
        synchronized (packetEvent.getAsyncMarker().getProcessingLock()) {
            packetEvent.setCancelled(true);
        }
    }

    private void kickPlayer(Player player, String reason) {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

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
    private void receiveFakeStartPacket(String username) {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        
        //see StartPacketListener for packet information
        PacketContainer startPacket = protocolManager.createPacket(PacketType.Login.Client.START);

        //uuid is ignored by the packet definition
        WrappedGameProfile fakeProfile = new WrappedGameProfile(UUID.randomUUID(), username);
        startPacket.getGameProfiles().write(0, fakeProfile);
        try {
            //we don't want to handle our own packets so ignore filters
            protocolManager.recieveClientPacket(fromPlayer, startPacket, false);
        } catch (InvocationTargetException | IllegalAccessException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to fake a new start packet", ex);
            //cancel the event in order to prevent the server receiving an invalid packet
            kickPlayer(fromPlayer, plugin.getCore().getMessage("error-kick"));
        }
    }
}
