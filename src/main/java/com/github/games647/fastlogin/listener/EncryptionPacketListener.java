package com.github.games647.fastlogin.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.injector.server.SocketInjector;
import com.comphenix.protocol.injector.server.TemporaryPlayerFactory;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.github.games647.fastlogin.FastLogin;
import com.github.games647.fastlogin.PlayerData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.logging.Level;

import javax.crypto.SecretKey;

import net.minecraft.server.v1_8_R3.MinecraftEncryption;
import net.minecraft.server.v1_8_R3.NetworkManager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class EncryptionPacketListener extends PacketAdapter {

    private static final String HAS_JOINED_URL = "https://sessionserver.mojang.com/session/minecraft/hasJoined?";

    private final ProtocolManager protocolManager;
    private final FastLogin fastLogin;

    public EncryptionPacketListener(FastLogin plugin, ProtocolManager protocolManger) {
        super(params(plugin, PacketType.Login.Client.ENCRYPTION_BEGIN).optionAsync());

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
    public void onPacketReceiving(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        Player player = event.getPlayer();

        final byte[] sharedSecret = packet.getByteArrays().read(0);
        byte[] clientVerify = packet.getByteArrays().read(1);

        PrivateKey privateKey = fastLogin.getKeyPair().getPrivate();

        String addressString = player.getAddress().toString();
        PlayerData cachedEntry = fastLogin.getSession().asMap().get(addressString);
        byte[] serverVerify = cachedEntry.getVerifyToken();
        if (!Arrays.equals(serverVerify, MinecraftEncryption.b(privateKey, clientVerify))) {
            player.kickPlayer("Invalid token");
            event.setCancelled(true);
            return;
        }

        //encrypt all following packets
        NetworkManager networkManager = getNetworkManager(event);
        SecretKey loginKey = MinecraftEncryption.a(privateKey, sharedSecret);
        networkManager.a(loginKey);
        String serverId = (new BigInteger(MinecraftEncryption.a("", fastLogin.getKeyPair().getPublic(), loginKey)))
                .toString(16);

        String username = cachedEntry.getUsername();
        if (!hasJoinedServer(username, serverId)) {
            //user tried to fake a authentification
            player.kickPlayer("Invalid session");
            event.setCancelled(true);
            return;
        }

        //fake a new login packet
        PacketContainer startPacket = protocolManager.createPacket(PacketType.Login.Client.START, true);
        WrappedGameProfile fakeProfile = WrappedGameProfile.fromOfflinePlayer(Bukkit.getOfflinePlayer(username));
        startPacket.getGameProfiles().write(0, fakeProfile);
        try {
            protocolManager.recieveClientPacket(event.getPlayer(), startPacket, false);
        } catch (InvocationTargetException | IllegalAccessException ex) {
            plugin.getLogger().log(Level.WARNING, null, ex);
        }

        event.setCancelled(true);
    }

    private NetworkManager getNetworkManager(PacketEvent event) throws IllegalArgumentException {
        SocketInjector injector = TemporaryPlayerFactory.getInjectorFromPlayer(event.getPlayer());
        NetworkManager networkManager = null;
        try {
            Field declaredField = injector.getClass().getDeclaredField("injector");
            declaredField.setAccessible(true);

            Object rawInjector = declaredField.get(injector);

            declaredField = rawInjector.getClass().getDeclaredField("networkManager");
            declaredField.setAccessible(true);
            networkManager = (NetworkManager) declaredField.get(rawInjector);
        } catch (IllegalAccessException | NoSuchFieldException ex) {
            plugin.getLogger().log(Level.WARNING, null, ex);
        }

        return networkManager;
    }

    private boolean hasJoinedServer(String username, String serverId) {
        try {
            String url = HAS_JOINED_URL + "username=" + username + "&serverId=" + serverId;

            HttpURLConnection conn = fastLogin.getConnection(url);

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = reader.readLine();
            if (!line.equals("null")) {
                JSONObject object = (JSONObject) JSONValue.parse(line);
                String uuid = (String) object.get("id");
                String name = (String) object.get("name");

                return true;
            }
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, null, ex);
        }

        return false;
    }
}
