package com.github.games647.fastlogin.bukkit;

import com.comphenix.protocol.AsynchronousManager;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.utility.SafeCacheBuilder;
import com.github.games647.fastlogin.bukkit.commands.CrackedCommand;
import com.github.games647.fastlogin.bukkit.commands.PremiumCommand;
import com.github.games647.fastlogin.bukkit.hooks.AuthPlugin;
import com.github.games647.fastlogin.bukkit.listener.BukkitJoinListener;
import com.github.games647.fastlogin.bukkit.listener.BungeeCordListener;
import com.github.games647.fastlogin.bukkit.listener.EncryptionPacketListener;
import com.github.games647.fastlogin.bukkit.listener.HandshakePacketListener;
import com.github.games647.fastlogin.bukkit.listener.ProtcolSupportListener;
import com.github.games647.fastlogin.bukkit.listener.StartPacketListener;
import com.google.common.cache.CacheLoader;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Sets;
import com.google.common.reflect.ClassPath;

import java.io.IOException;
import java.security.KeyPair;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.apache.commons.lang.RandomStringUtils;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * This plugin checks if a player has a paid account and if so tries to skip offline mode authentication.
 */
public class FastLoginBukkit extends JavaPlugin {

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

    private AuthPlugin authPlugin;
    private final MojangApiConnector mojangApiConnector = new MojangApiConnector(this);

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (getServer().getOnlineMode() || !registerHooks()) {
            //we need to require offline to prevent a session request for a offline player
            getLogger().severe("Server have to be in offline mode and have an auth plugin installed");
            setEnabled(false);
            return;
        }

        //register listeners on success
        if (getServer().getPluginManager().isPluginEnabled("ProtocolSupport")) {
            getServer().getPluginManager().registerEvents(new ProtcolSupportListener(this), this);
        } else {
            ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
            protocolManager.addPacketListener(new HandshakePacketListener(this));

            //we are performing HTTP request on these so run it async (seperate from the Netty IO threads)
            AsynchronousManager asynchronousManager = protocolManager.getAsynchronousManager();
            asynchronousManager.registerAsyncHandler(new StartPacketListener(this, protocolManager)).start();
            asynchronousManager.registerAsyncHandler(new EncryptionPacketListener(this, protocolManager)).start();

            getServer().getPluginManager().registerEvents(new BukkitJoinListener(this), this);
        }

        //register commands using a unique name
        getCommand("premium").setExecutor(new PremiumCommand(this));
        getCommand("cracked").setExecutor(new CrackedCommand(this));

        //check for incoming messages from the bungeecord version of this plugin
        getServer().getMessenger().registerIncomingPluginChannel(this, getName(), new BungeeCordListener(this));
        getServer().getMessenger().registerOutgoingPluginChannel(this, getName());
    }

    @Override
    public void onDisable() {
        //clean up
        session.clear();
        enabledPremium.clear();
        bungeeCordUsers.clear();

        //remove old blacklists
        for (Player player : getServer().getOnlinePlayers()) {
            player.removeMetadata(getName(), this);
        }
    }

    public String generateStringPassword() {
        return RandomStringUtils.random(8, true, true);
    }

    /**
     * Gets a thread-safe map about players which are connecting to the server are being checked to be premium (paid
     * account)
     *
     * @return a thread-safe session map
     */
    public ConcurrentMap<String, PlayerSession> getSessions() {
        return session;
    }

    /**
     * Gets a concurrent map with weak keys for all bungeecord users which could be detected. It's mapped by a fake
     * instance of player created by Protocollib and a non-null raw object.
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
     * Gets the auth plugin hook in order to interact with the plugins
     *
     * @return interface to any supported auth plugin
     */
    public AuthPlugin getAuthPlugin() {
        return authPlugin;
    }

    /**
     * Gets the a connection in order to access important
     * features from the Mojang API.
     *
     * @return the connector instance
     */
    public MojangApiConnector getApiConnector() {
        return mojangApiConnector;
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

        authPlugin = authPluginHook;
        return true;
    }
}
