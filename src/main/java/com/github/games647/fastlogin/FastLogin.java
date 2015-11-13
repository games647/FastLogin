package com.github.games647.fastlogin;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.utility.SafeCacheBuilder;
import com.github.games647.fastlogin.hooks.AuthPlugin;
import com.github.games647.fastlogin.listener.BukkitJoinListener;
import com.github.games647.fastlogin.listener.BungeeCordListener;
import com.github.games647.fastlogin.listener.EncryptionPacketListener;
import com.github.games647.fastlogin.listener.HandshakePacketListener;
import com.github.games647.fastlogin.listener.StartPacketListener;
import com.google.common.cache.CacheLoader;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Sets;
import com.google.common.reflect.ClassPath;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyPair;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * This plugin checks if a player has a paid account and if so tries to skip offline mode authentication.
 */
public class FastLogin extends JavaPlugin {

    //http connection, read timeout and user agent for a connection to mojang api servers
    private static final int TIMEOUT = 1 * 1000;
    private static final String USER_AGENT = "Premium-Checker";

    //provide a immutable key pair to be thread safe | used for encrypting and decrypting traffic
    private final KeyPair keyPair = EncryptionUtil.generateKeyPair();

    //we need a thread-safe set because we access it async in the packet listener
    private final Set<String> enabledPremium = Sets.newConcurrentHashSet();

    //player=fake player created by Protocollib | this mapmaker creates a concurrent map with weak keys
    private final ConcurrentMap<Player, Object> bungeeCordUsers = new MapMaker().weakKeys().makeMap();

    //this map is thread-safe for async access (Packet Listener)
    //SafeCacheBuilder is used in order to be version independent
    private final ConcurrentMap<String, PlayerSession> session = SafeCacheBuilder.<String, PlayerSession>newBuilder()
            //2 minutes should be enough as a timeout for bad internet connection (Server, Client and Mojang)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            //mapped by ip:port -> PlayerSession
            .build(new CacheLoader<String, PlayerSession>() {

                @Override
                public PlayerSession load(String key) throws Exception {
                    //A key should be inserted manually on start packet
                    throw new UnsupportedOperationException("Not supported");
                }
            });

    @Override
    public void onLoad() {
        //online mode is only changeable after a restart so check it here
        if (getServer().getOnlineMode()) {
            //we need to require offline to prevent a session request for a offline player
            getLogger().severe("Server have to be in offline mode");

            setEnabled(false);
        }
    }

    @Override
    public void onEnable() {
        if (!isEnabled() || !registerHooks()) {
            return;
        }

        //register packet listeners on success
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        protocolManager.addPacketListener(new HandshakePacketListener(this));
        protocolManager.addPacketListener(new StartPacketListener(this, protocolManager));
        protocolManager.addPacketListener(new EncryptionPacketListener(this, protocolManager));

        //register commands using a unique name
        getCommand("premium").setExecutor(new PremiumCommand(this));

        //check for incoming messages from the bungeecord version of this plugin
        getServer().getMessenger().registerIncomingPluginChannel(this, this.getName(), new BungeeCordListener(this));
    }

    @Override
    public void onDisable() {
        //clean up
        session.clear();
        enabledPremium.clear();
        bungeeCordUsers.clear();
    }

    /**
     * Gets a thread-safe map about players which are connecting to the server
     * are being checked to be premium (paid account)
     *
     * @return a thread-safe session map
     */
    public ConcurrentMap<String, PlayerSession> getSessions() {
        return session;
    }

    /**
     * Gets a concurrent map with weak keys for all bungeecord users
     * which could be detected. It's mapped by a fake instance of player
     * created by Protocollib and a non-null raw object.
     *
     * Represents a similar set collection
     *
     * @return
     */
    public ConcurrentMap<Player, Object> getBungeeCordUsers() {
        return bungeeCordUsers;
    }

    /**
     * Gets the server KeyPair. This is used to encrypt or decrypt traffic between the client and server
     *
     * @return the server KeyPair
     */
    public KeyPair getServerKey() {
        return keyPair;
    }

    /**
     * Gets a set of user who activated premium logins
     *
     * @return user who activated premium logins
     */
    public Set<String> getEnabledPremium() {
        return enabledPremium;
    }

    /**
     * Prepares a Mojang API connection. The connection is not started in this method
     *
     * @param url the url connecting to
     * @return the prepared connection
     *
     * @throws IOException on invalid url format or on {@link java.net.URL#openConnection() }
     */
    public HttpURLConnection getConnection(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(TIMEOUT);
        connection.setReadTimeout(TIMEOUT);
        //the new Mojang API just uses json as response
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", USER_AGENT);

        return connection;
    }

    private boolean registerHooks() {
        AuthPlugin authPluginHook = null;
        try {
            String hooksPackage = this.getClass().getPackage().getName() + ".hooks";
            //Look through all classes in the hooks package and look for supporting plugins on the server
            for (ClassPath.ClassInfo clazzInfo : ClassPath.from(getClassLoader()).getTopLevelClasses(hooksPackage)) {
                //remove the hook suffix
                String pluginName = clazzInfo.getSimpleName().replace("Hook", "");
                Class<?> clazz = clazzInfo.load();
                //uses only member classes which uses AuthPlugin interface (skip interfaces)
                if (AuthPlugin.class.isAssignableFrom(clazz)
                        //check only for enabled plugins. A single plugin could be disabled by plugin managers
                        && getServer().getPluginManager().isPluginEnabled(pluginName)) {
                    authPluginHook = (AuthPlugin) clazz.newInstance();
                    getLogger().log(Level.INFO, "Hooking into auth plugin: {0}", pluginName);
                    break;
                }
            }
        } catch (InstantiationException | IllegalAccessException | IOException ex) {
            getLogger().log(Level.SEVERE, "Couldn't load the integration class", ex);
        }

        if (authPluginHook == null) {
            //run this check for exceptions (errors) and not found plugins
            getLogger().warning("No support offline Auth plugin found. ");
            getLogger().warning("Disabling this plugin...");

            setEnabled(false);
            return false;
        }

        //We found a supporting plugin - we can now register a forwarding listener to skip authentication from them
        getServer().getPluginManager().registerEvents(new BukkitJoinListener(this, authPluginHook), this);
        return true;
    }
}
