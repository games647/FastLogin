package com.github.games647.fastlogin.bukkit;

import com.comphenix.protocol.AsynchronousManager;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.utility.SafeCacheBuilder;
import com.github.games647.fastlogin.bukkit.commands.CrackedCommand;
import com.github.games647.fastlogin.bukkit.commands.PremiumCommand;
import com.github.games647.fastlogin.bukkit.hooks.AuthMeHook;
import com.github.games647.fastlogin.bukkit.hooks.BukkitAuthPlugin;
import com.github.games647.fastlogin.bukkit.hooks.LogItHook;
import com.github.games647.fastlogin.bukkit.hooks.LoginSecurityHook;
import com.github.games647.fastlogin.bukkit.hooks.UltraAuthHook;
import com.github.games647.fastlogin.bukkit.hooks.xAuthHook;
import com.github.games647.fastlogin.bukkit.listener.BukkitJoinListener;
import com.github.games647.fastlogin.bukkit.listener.BungeeCordListener;
import com.github.games647.fastlogin.bukkit.listener.EncryptionPacketListener;
import com.github.games647.fastlogin.bukkit.listener.ProtocolSupportListener;
import com.github.games647.fastlogin.bukkit.listener.StartPacketListener;
import com.google.common.cache.CacheLoader;
import com.google.common.collect.Lists;
import de.st_ddt.crazylogin.CrazyLogin;

import java.lang.reflect.Method;
import java.security.KeyPair;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * This plugin checks if a player has a paid account and if so tries to skip offline mode authentication.
 */
public class FastLoginBukkit extends JavaPlugin {

    private static final int WORKER_THREADS = 5;

    public static UUID parseId(String withoutDashes) {
        return UUID.fromString(withoutDashes.substring(0, 8)
                + "-" + withoutDashes.substring(8, 12)
                + "-" + withoutDashes.substring(12, 16)
                + "-" + withoutDashes.substring(16, 20)
                + "-" + withoutDashes.substring(20, 32));
    }

    //provide a immutable key pair to be thread safe | used for encrypting and decrypting traffic
    private final KeyPair keyPair = EncryptionUtil.generateKeyPair();

    private boolean bungeeCord;
    private Storage storage;
    private boolean serverStarted;

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

    private BukkitAuthPlugin authPlugin;
    private final MojangApiConnector mojangApiConnector = new MojangApiConnector(this);
    private PasswordGenerator passwordGenerator = new DefaultPasswordGenerator();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (getServer().getOnlineMode()) {
            //we need to require offline to prevent a session request for a offline player
            getLogger().severe("Server have to be in offline mode");
            setEnabled(false);
            return;
        }

        try {
            if (Bukkit.spigot().getConfig().isBoolean("settings.bungeecord")) {
                bungeeCord = Bukkit.spigot().getConfig().getBoolean("settings.bungeecord");
            } else {
                Method getConfigMethod = FuzzyReflection.fromObject(getServer().spigot(), true)
                        .getMethodByName("getSpigotConfig");
                getConfigMethod.setAccessible(true);
                YamlConfiguration spigotConfig = (YamlConfiguration) getConfigMethod.invoke(getServer().spigot());
                bungeeCord = spigotConfig.getBoolean("settings.bungeecord");
            }
        } catch (Exception | NoSuchMethodError ex) {
            getLogger().warning("Cannot check bungeecord support. You use a non-spigot build");
        }

        boolean hookFound = registerHooks();
        if (bungeeCord) {
            getLogger().info("BungeeCord setting detected. No auth plugin is required");
        } else if (!hookFound) {
            getLogger().warning("No auth plugin were found by this plugin "
                    + "(other plugins could hook into this after the intialization of this plugin)"
                    + "and bungeecord is deactivated. "
                    + "Either one or both of the checks have to pass in order to use this plugin");
        }

        if (bungeeCord) {
            //check for incoming messages from the bungeecord version of this plugin
            getServer().getMessenger().registerIncomingPluginChannel(this, getName(), new BungeeCordListener(this));
            getServer().getMessenger().registerOutgoingPluginChannel(this, getName());
            //register listeners on success
        } else {
            String driver = getConfig().getString("driver");
            String host =  getConfig().getString("host", "");
            int port = getConfig().getInt("port", 3306);
            String database = getConfig().getString("database");

            String username = getConfig().getString("username", "");
            String password = getConfig().getString("password", "");

            this.storage = new Storage(this, driver, host, port, database, username, password);
            try {
                storage.createTables();
            } catch (SQLException sqlEx) {
                getLogger().log(Level.SEVERE, "Failed to create database tables. Disabling plugin...", sqlEx);
                setEnabled(false);
                return;
            }

            if (getServer().getPluginManager().isPluginEnabled("ProtocolSupport")) {
                getServer().getPluginManager().registerEvents(new ProtocolSupportListener(this), this);
            } else {
                ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

                //we are performing HTTP request on these so run it async (seperate from the Netty IO threads)
                AsynchronousManager asynchronousManager = protocolManager.getAsynchronousManager();

                StartPacketListener startPacketListener = new StartPacketListener(this, protocolManager);
                EncryptionPacketListener encryptionPacketListener = new EncryptionPacketListener(this, protocolManager);

                asynchronousManager.registerAsyncHandler(startPacketListener).start(WORKER_THREADS);
                asynchronousManager.registerAsyncHandler(encryptionPacketListener).start(WORKER_THREADS);
            }
        }

        getServer().getPluginManager().registerEvents(new BukkitJoinListener(this), this);

        //register commands using a unique name
        getCommand("premium").setExecutor(new PremiumCommand(this));
        getCommand("cracked").setExecutor(new CrackedCommand(this));
    }

    @Override
    public void onDisable() {
        //clean up
        session.clear();

        //remove old blacklists
        for (Player player : getServer().getOnlinePlayers()) {
            player.removeMetadata(getName(), this);
        }

        if (storage != null) {
            storage.close();
        }
    }

    public String generateStringPassword(Player player) {
        return passwordGenerator.getRandomPassword(player);
    }

    public void setPasswordGenerator(PasswordGenerator passwordGenerator) {
        this.passwordGenerator = passwordGenerator;
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
     * Gets the server KeyPair. This is used to encrypt or decrypt traffic between the client and server
     *
     * @return the server KeyPair
     */
    public KeyPair getServerKey() {
        return keyPair;
    }

    public Storage getStorage() {
        return storage;
    }

    /**
     * Gets the auth plugin hook in order to interact with the plugins. This can be null if no supporting auth plugin
     * was found.
     *
     * @return interface to any supported auth plugin
     */
    public BukkitAuthPlugin getAuthPlugin() {
        return authPlugin;
    }

    public void setAuthPluginHook(BukkitAuthPlugin authPlugin) {
        this.authPlugin = authPlugin;
    }

    /**
     * Gets the a connection in order to access important features from the Mojang API.
     *
     * @return the connector instance
     */
    public MojangApiConnector getApiConnector() {
        return mojangApiConnector;
    }

    private boolean registerHooks() {
        BukkitAuthPlugin authPluginHook = null;
        try {
            List<Class<?>> supportedHooks = Lists.newArrayList(AuthMeHook.class, CrazyLogin.class
                    , LogItHook.class, LoginSecurityHook.class, UltraAuthHook.class, xAuthHook.class);
            for (Class<?> clazz : supportedHooks) {
                String pluginName = clazz.getSimpleName().replace("Hook", "");
                //uses only member classes which uses AuthPlugin interface (skip interfaces)
                if (BukkitAuthPlugin.class.isAssignableFrom(clazz)
                        //check only for enabled plugins. A single plugin could be disabled by plugin managers
                        && getServer().getPluginManager().isPluginEnabled(pluginName)) {
                    authPluginHook = (BukkitAuthPlugin) clazz.newInstance();
                    getLogger().log(Level.INFO, "Hooking into auth plugin: {0}", pluginName);
                    break;
                }
            }
        } catch (InstantiationException | IllegalAccessException ex) {
            getLogger().log(Level.SEVERE, "Couldn't load the integration class", ex);
        }

        if (authPluginHook == null) {
            //run this check for exceptions (errors) and not found plugins
            getLogger().warning("No support offline Auth plugin found. ");
            return false;
        }

        authPlugin = authPluginHook;
        return true;
    }

    public boolean isBungeeCord() {
        return bungeeCord;
    }

    /**
     * Wait before the server is fully started. This is workaround, because connections right on startup are not
     * injected by ProtocolLib
     *
     * @return
     */
    public boolean isServerFullyStarted() {
        return serverStarted;
    }

    public void setServerStarted() {
        if (!this.serverStarted) {
            this.serverStarted = true;
        }
    }
}
