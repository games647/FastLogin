package com.github.games647.fastlogin.bukkit;

import com.avaje.ebeaninternal.api.ClassUtil;
import com.comphenix.protocol.AsynchronousManager;
import com.comphenix.protocol.ProtocolLibrary;
import com.github.games647.fastlogin.bukkit.commands.CrackedCommand;
import com.github.games647.fastlogin.bukkit.commands.ImportCommand;
import com.github.games647.fastlogin.bukkit.commands.PremiumCommand;
import com.github.games647.fastlogin.bukkit.hooks.BukkitAuthPlugin;
import com.github.games647.fastlogin.bukkit.listener.BukkitJoinListener;
import com.github.games647.fastlogin.bukkit.listener.BungeeCordListener;
import com.github.games647.fastlogin.bukkit.listener.protocollib.EncryptionPacketListener;
import com.github.games647.fastlogin.bukkit.listener.protocollib.LoginSkinApplyListener;
import com.github.games647.fastlogin.bukkit.listener.protocollib.StartPacketListener;
import com.github.games647.fastlogin.bukkit.listener.protocolsupport.ProtocolSupportListener;
import com.github.games647.fastlogin.bukkit.tasks.DelayedAuthHook;
import com.github.games647.fastlogin.core.FastLoginCore;
import com.google.common.collect.Sets;

import java.security.KeyPair;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * This plugin checks if a player has a paid account and if so tries to skip offline mode authentication.
 */
public class FastLoginBukkit extends JavaPlugin {

    private static final int WORKER_THREADS = 3;

    //provide a immutable key pair to be thread safe | used for encrypting and decrypting traffic
    private final KeyPair keyPair = EncryptionUtil.generateKeyPair();

    private boolean bungeeCord;
    private final FastLoginCore core = new BukkitCore(this);
    private boolean serverStarted;

    private final Set<UUID> pendingConfirms = Sets.newHashSet();

    //this map is thread-safe for async access (Packet Listener)
    //SafeCacheBuilder is used in order to be version independent
    private final ConcurrentMap<String, BukkitLoginSession> session = BukkitCore.buildCache(1, -1);
    //1 minutes should be enough as a timeout for bad internet connection (Server, Client and Mojang)

    private BukkitAuthPlugin authPlugin;
    private PasswordGenerator passwordGenerator = new DefaultPasswordGenerator();
    
    @Override
    public void onEnable() {
        core.loadConfig();
        core.loadMessages();

        List<String> ipAddresses = getConfig().getStringList("ip-addresses");
        int requestLimit = getConfig().getInt("mojang-request-limit");
        ConcurrentMap<Object, Object> requestCache = BukkitCore.buildCache(10, -1);
        MojangApiBukkit mojangApi = new MojangApiBukkit(requestCache, getLogger(), ipAddresses, requestLimit);
        core.setMojangApiConnector(mojangApi);

        try {
            if (ClassUtil.isPresent("org.spigotmc.SpigotConfig")) {
                bungeeCord = Class.forName("org.spigotmc.SpigotConfig").getDeclaredField("bungee").getBoolean(null);
            }
        } catch (Exception | NoSuchMethodError ex) {
            getLogger().warning("Cannot check bungeecord support. You use a non-spigot build");
            ex.printStackTrace();
        }

        if (getServer().getOnlineMode()) {
            //we need to require offline to prevent a session request for a offline player
            getLogger().severe("Server have to be in offline mode");
            setEnabled(false);
            return;
        }

        if (bungeeCord) {
            setServerStarted();
            
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

            if (!core.setupDatabase(driver, host, port, database, username, password)) {
                setEnabled(false);
                return;
            }

            if (getServer().getPluginManager().isPluginEnabled("ProtocolSupport")) {
                getServer().getPluginManager().registerEvents(new ProtocolSupportListener(this), this);
            } else if (getServer().getPluginManager().isPluginEnabled("ProtocolLib")) {
                //we are performing HTTP request on these so run it async (seperate from the Netty IO threads)
                AsynchronousManager asynchronousManager = ProtocolLibrary.getProtocolManager().getAsynchronousManager();

                StartPacketListener startPacketListener = new StartPacketListener(this);
                EncryptionPacketListener encryptionPacketListener = new EncryptionPacketListener(this);

                asynchronousManager.registerAsyncHandler(startPacketListener).start(WORKER_THREADS);
                asynchronousManager.registerAsyncHandler(encryptionPacketListener).start(WORKER_THREADS);
                getServer().getPluginManager().registerEvents(new LoginSkinApplyListener(this), this);
            } else {
                getLogger().warning("Either ProtocolLib or ProtocolSupport have to be installed "
                        + "if you don't use BungeeCord");
            }
        }

        //delay dependency setup because we load the plugin very early where plugins are initialized yet
        getServer().getScheduler().runTask(this, new DelayedAuthHook(this));

        getServer().getPluginManager().registerEvents(new BukkitJoinListener(this), this);

        //register commands using a unique name
        getCommand("premium").setExecutor(new PremiumCommand(this));
        getCommand("cracked").setExecutor(new CrackedCommand(this));
        getCommand("import-auth").setExecutor(new ImportCommand(this));
    }

    @Override
    public void onDisable() {
        //clean up
        session.clear();

        if (core != null) {
            core.close();
        }

        //remove old blacklists
        for (Player player : getServer().getOnlinePlayers()) {
            player.removeMetadata(getName(), this);
        }
    }

    public FastLoginCore getCore() {
        return core;
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
    public ConcurrentMap<String, BukkitLoginSession> getSessions() {
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

    /**
     * Gets the auth plugin hook in order to interact with the plugins. This can be null if no supporting auth plugin
     * was found.
     *
     * @return interface to any supported auth plugin
     */
    public BukkitAuthPlugin getAuthPlugin() {
        if (authPlugin == null) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                getLogger().log(Level.SEVERE, null, ex);
            }
        }

        return authPlugin;
    }

    public void setAuthPluginHook(BukkitAuthPlugin authPlugin) {
        this.authPlugin = authPlugin;
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

    public Set<UUID> getPendingConfirms() {
        return pendingConfirms;
    }

    public void setServerStarted() {
        if (!this.serverStarted) {
            this.serverStarted = true;
        }
    }
}
