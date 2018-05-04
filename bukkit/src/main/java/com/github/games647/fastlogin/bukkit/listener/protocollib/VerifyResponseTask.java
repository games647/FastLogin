package com.github.games647.fastlogin.bukkit.listener.protocollib;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.injector.server.TemporaryPlayerFactory;
import com.comphenix.protocol.reflect.FieldUtils;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.github.games647.craftapi.model.auth.Verification;
import com.github.games647.craftapi.model.skin.Property;
import com.github.games647.craftapi.resolver.MojangResolver;
import com.github.games647.fastlogin.bukkit.BukkitLoginSession;
import com.github.games647.fastlogin.bukkit.EncryptionUtil;
import com.github.games647.fastlogin.bukkit.FastLoginBukkit;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import org.bukkit.entity.Player;

import static com.comphenix.protocol.PacketType.Login.Client.START;
import static com.comphenix.protocol.PacketType.Login.Server.DISCONNECT;

public class VerifyResponseTask implements Runnable {

    private final FastLoginBukkit plugin;
    private final PacketEvent packetEvent;
    private final KeyPair serverKey;

    private final Player player;

    private final byte[] sharedSecret;

    public VerifyResponseTask(FastLoginBukkit plugin, PacketEvent packetEvent, Player player,
                              byte[] sharedSecret, KeyPair keyPair) {
        this.plugin = plugin;
        this.packetEvent = packetEvent;
        this.player = player;
        this.sharedSecret = Arrays.copyOf(sharedSecret, sharedSecret.length);
        this.serverKey = keyPair;
    }

    @Override
    public void run() {
        try {
            BukkitLoginSession session = plugin.getLoginSessions().get(player.getAddress().toString());
            if (session == null) {
                disconnect(plugin.getCore().getMessage("invalid-request"), true
                        , "GameProfile {0} tried to send encryption response at invalid state", player.getAddress());
            } else {
                verifyResponse(session);
            }
        } finally {
            //this is a fake packet; it shouldn't be send to the server
            synchronized (packetEvent.getAsyncMarker().getProcessingLock()) {
                packetEvent.setCancelled(true);
            }

            ProtocolLibrary.getProtocolManager().getAsynchronousManager().signalPacketTransmission(packetEvent);
        }
    }

    private void verifyResponse(BukkitLoginSession session) {
        PrivateKey privateKey = serverKey.getPrivate();

        Cipher cipher;
        SecretKey loginKey;
        try {
            cipher = Cipher.getInstance(privateKey.getAlgorithm());

            loginKey = EncryptionUtil.decryptSharedKey(cipher, privateKey, sharedSecret);
        } catch (GeneralSecurityException securityEx) {
            disconnect("error-kick", false, "Cannot decrypt received contents", securityEx);
            return;
        }

        try {
            if (!checkVerifyToken(session, cipher, privateKey) || !encryptConnection(loginKey)) {
                return;
            }
        } catch (Exception ex) {
            disconnect("error-kick", false, "Cannot decrypt received contents", ex);
            return;
        }

        String serverId = EncryptionUtil.getServerIdHashString("", loginKey, serverKey.getPublic());

        String username = session.getUsername();
        InetSocketAddress socketAddress = player.getAddress();
        try {
            MojangResolver resolver = plugin.getCore().getResolver();
            InetAddress address = socketAddress.getAddress();
            Optional<Verification> response = resolver.hasJoined(username, serverId, address);
            if (response.isPresent()) {
                plugin.getLog().info("GameProfile {} has a verified premium account", username);

                Property[] properties = response.get().getProperties();
                if (properties.length > 0) {
                    session.setSkinProperty(properties[0]);
                }

                session.setUuid(response.get().getId());
                session.setVerified(true);

                setPremiumUUID(session.getUuid());
                receiveFakeStartPacket(username);
            } else {
                //user tried to fake a authentication
                disconnect(plugin.getCore().getMessage("invalid-session"), true
                        , "GameProfile {0} ({1}) tried to log in with an invalid session ServerId: {2}"
                        , session.getUsername(), socketAddress, serverId);
            }
        } catch (IOException ioEx) {
            disconnect("error-kick", false, "Failed to connect to session server", ioEx);
        }
    }

    private void setPremiumUUID(UUID premiumUUID) {
        if (plugin.getConfig().getBoolean("premiumUuid") && premiumUUID != null) {
            try {
                Object networkManager = getNetworkManager();
                //https://github.com/bergerkiller/CraftSource/blob/master/net.minecraft.server/NetworkManager.java#L69
                FieldUtils.writeField(networkManager, "spoofedUUID", premiumUUID, true);
            } catch (Exception exc) {
                plugin.getLog().error("Error setting premium uuid of {}", player, exc);
            }
        }
    }

    private boolean checkVerifyToken(BukkitLoginSession session, Cipher cipher, PrivateKey privateKey)
            throws GeneralSecurityException {
        byte[] requestVerify = session.getVerifyToken();
        //encrypted verify token
        byte[] responseVerify = packetEvent.getPacket().getByteArrays().read(1);

        //https://github.com/bergerkiller/CraftSource/blob/master/net.minecraft.server/LoginListener.java#L182
        if (!Arrays.equals(requestVerify, EncryptionUtil.decrypt(cipher, privateKey, responseVerify))) {
            //check if the verify token are equal to the server sent one
            disconnect(plugin.getCore().getMessage("invalid-verify-token"), true
                    , "GameProfile {0} ({1}) tried to login with an invalid verify token. Server: {2} Client: {3}"
                    , session.getUsername(), packetEvent.getPlayer().getAddress(), requestVerify, responseVerify);
            return false;
        }

        return true;
    }

    //try to get the networkManager from ProtocolLib
    private Object getNetworkManager() throws IllegalAccessException, ClassNotFoundException {
        Object injectorContainer = TemporaryPlayerFactory.getInjectorFromPlayer(player);

        //ChannelInjector
        Class<?> injectorClass = Class.forName("com.comphenix.protocol.injector.netty.Injector");
        Object rawInjector = FuzzyReflection.getFieldValue(injectorContainer, injectorClass, true);
        return FieldUtils.readField(rawInjector, "networkManager", true);
    }

    private boolean encryptConnection(SecretKey loginKey) throws IllegalArgumentException {
        try {
            //get the NMS connection handle of this player
            Object networkManager = getNetworkManager();

            //try to detect the method by parameters
            Method encryptMethod = FuzzyReflection
                    .fromObject(networkManager).getMethodByParameters("a", SecretKey.class);

            //encrypt/decrypt following packets
            //the client expects this behaviour
            encryptMethod.invoke(networkManager, loginKey);
        } catch (Exception ex) {
            disconnect("error-kick", false, "Couldn't enable encryption", ex);
            return false;
        }

        return true;
    }

    private void disconnect(String kickReason, boolean debug, String logMessage, Object... arguments) {
        if (debug) {
            plugin.getLog().debug(logMessage, arguments);
        } else {
            plugin.getLog().error(logMessage, arguments);
        }

        kickPlayer(plugin.getCore().getMessage(kickReason));
    }

    private void kickPlayer(String reason) {
        PacketContainer kickPacket = new PacketContainer(DISCONNECT);
        kickPacket.getChatComponents().write(0, WrappedChatComponent.fromText(reason));
        try {
            //send kick packet at login state
            //the normal event.getPlayer.kickPlayer(String) method does only work at play state
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, kickPacket);
            //tell the server that we want to close the connection
            player.kickPlayer("Disconnect");
        } catch (InvocationTargetException ex) {
            plugin.getLog().error("Error sending kick packet for: {}", player, ex);
        }
    }

    //fake a new login packet in order to let the server handle all the other stuff
    private void receiveFakeStartPacket(String username) {
        //see StartPacketListener for packet information
        PacketContainer startPacket = new PacketContainer(START);

        //uuid is ignored by the packet definition
        WrappedGameProfile fakeProfile = new WrappedGameProfile(UUID.randomUUID(), username);
        startPacket.getGameProfiles().write(0, fakeProfile);
        try {
            //we don't want to handle our own packets so ignore filters
            ProtocolLibrary.getProtocolManager().recieveClientPacket(player, startPacket, false);
        } catch (InvocationTargetException | IllegalAccessException ex) {
            plugin.getLog().warn("Failed to fake a new start packet for: {}", username, ex);
            //cancel the event in order to prevent the server receiving an invalid packet
            kickPlayer(plugin.getCore().getMessage("error-kick"));
        }
    }
}
