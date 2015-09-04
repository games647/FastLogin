package com.github.games647.fastlogin;

import com.github.games647.fastlogin.listener.PlayerListener;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.github.games647.fastlogin.listener.EncryptionPacketListener;
import com.github.games647.fastlogin.listener.StartPacketListener;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import org.bukkit.plugin.java.JavaPlugin;

public class FastLogin extends JavaPlugin {

    private final KeyPair keyPair = generateKey();
    private final Cache<String, PlayerData> session = CacheBuilder.newBuilder()
            .expireAfterWrite(2, TimeUnit.MINUTES)
            .build();

    @Override
    public void onEnable() {
        if (!isEnabled()) {
            return;
        }

        if (!getServer().getPluginManager().isPluginEnabled("AuthMe")
                && !getServer().getPluginManager().isPluginEnabled("xAuth")) {
            getLogger().warning("No support offline Auth plugin found. ");
            getLogger().warning("Disabling this plugin...");

            setEnabled(false);
            return;
        }

        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        protocolManager.addPacketListener(new EncryptionPacketListener(this, protocolManager));
        protocolManager.addPacketListener(new StartPacketListener(this, protocolManager));

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
    }

    @Override
    public void onLoad() {
        //online mode is only changeable aftter a restart
        if (getServer().getOnlineMode()) {
            getLogger().severe("Server have to be in offline mode");

            setEnabled(false);
        }

        generateKey();
    }

    private KeyPair generateKey() {
        try {
            KeyPairGenerator keypairgenerator = KeyPairGenerator.getInstance("RSA");

            keypairgenerator.initialize(1024);
            return keypairgenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            //Should be default existing in every vm
        }

        return null;
    }

    public Cache<String, PlayerData> getSession() {
        return session;
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }

    public HttpURLConnection getConnection(String url) throws IOException {
        final HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", "Premium-Checker");

        return connection;
    }
}
