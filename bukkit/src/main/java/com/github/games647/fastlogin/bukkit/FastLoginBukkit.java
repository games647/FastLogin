package com.github.games647.fastlogin.bukkit;

import com.github.games647.fastlogin.bukkit.command.CrackedCommand;
import com.github.games647.fastlogin.bukkit.command.PremiumCommand;
import com.github.games647.fastlogin.bukkit.listener.ConnectionListener;
import com.github.games647.fastlogin.bukkit.listener.protocollib.ProtocolLibListener;
import com.github.games647.fastlogin.bukkit.listener.protocollib.SkinApplyListener;
import com.github.games647.fastlogin.bukkit.listener.protocolsupport.ProtocolSupportListener;
import com.github.games647.fastlogin.bukkit.task.DelayedAuthHook;
import com.github.games647.fastlogin.core.CommonUtil;
import com.github.games647.fastlogin.core.PremiumStatus;
import com.github.games647.fastlogin.core.shared.FastLoginCore;
import com.github.games647.fastlogin.core.shared.PlatformPlugin;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

/**
 * This plugin checks if a player has a paid account and if so tries to skip offline mode authentication.
 */
public class FastLoginBukkit extends JavaPlugin implements PlatformPlugin<CommandSender> {

    //1 minutes should be enough as a timeout for bad internet connection (Server, Client and Mojang)
    private final ConcurrentMap<String, BukkitLoginSession> loginSession = CommonUtil.buildCache(1, -1);
    private final Map<UUID, PremiumStatus> premiumPlayers = new ConcurrentHashMap<>();
    private final Logger logger;

    private boolean serverStarted;
    private BungeeManager bungeeManager;
    private final BukkitScheduler scheduler;
    private FastLoginCore<Player, CommandSender, FastLoginBukkit> core;

    public FastLoginBukkit() {
        this.logger = CommonUtil.createLoggerFromJDK(getLogger());
        this.scheduler = new BukkitScheduler(this, logger, getThreadFactory());
    }

    @Override
    public void onEnable() {
        core = new FastLoginCore<>(this);
        core.load();

        if (getServer().getOnlineMode()) {
            //we need to require offline to prevent a loginSession request for a offline player
            logger.error("Server has to be in offline mode");
            setEnabled(false);
            return;
        }

        bungeeManager = new BungeeManager(this);
        bungeeManager.initialize();
        
        PluginManager pluginManager = getServer().getPluginManager();
        if (bungeeManager.isEnabled()) {
            setServerStarted();
        } else {
            if (!core.setupDatabase()) {
                setEnabled(false);
                return;
            }

            if (pluginManager.isPluginEnabled("ProtocolSupport")) {
                pluginManager.registerEvents(new ProtocolSupportListener(this, core.getRateLimiter()), this);
            } else if (pluginManager.isPluginEnabled("ProtocolLib")) {
                ProtocolLibListener.register(this, core.getRateLimiter());
                pluginManager.registerEvents(new SkinApplyListener(this), this);
            } else {
                logger.warn("Either ProtocolLib or ProtocolSupport have to be installed if you don't use BungeeCord");
            }
        }

        //delay dependency setup because we load the plugin very early where plugins are initialized yet
        getServer().getScheduler().runTaskLater(this, new DelayedAuthHook(this), 5L);

        pluginManager.registerEvents(new ConnectionListener(this), this);

        //register commands using a unique name
        getCommand("premium").setExecutor(new PremiumCommand(this));
        getCommand("cracked").setExecutor(new CrackedCommand(this));

        if (pluginManager.isPluginEnabled("PlaceholderAPI")) {
            //prevents NoClassDef errors if it's not available
            PremiumPlaceholder.register(this);
        }
    }

    @Override
    public void onDisable() {
        loginSession.clear();
        premiumPlayers.clear();

        if (core != null) {
            core.close();
        }

        bungeeManager.cleanup();
    }

    public FastLoginCore<Player, CommandSender, FastLoginBukkit> getCore() {
        return core;
    }

    /**
     * Gets a thread-safe map about players which are connecting to the server are being checked to be premium (paid
     * account)
     *
     * @return a thread-safe loginSession map
     */
    public ConcurrentMap<String, BukkitLoginSession> getLoginSessions() {
        return loginSession;
    }

    public BukkitLoginSession getSession(InetSocketAddress addr) {
        String id = addr.getAddress().getHostAddress() + ':' + addr.getPort();
        return loginSession.get(id);
    }

    public void putSession(InetSocketAddress addr, BukkitLoginSession session) {
        String id = addr.getAddress().getHostAddress() + ':' + addr.getPort();
        loginSession.put(id, session);
    }

    public void removeSession(InetSocketAddress addr) {
        String id = addr.getAddress().getHostAddress() + ':' + addr.getPort();
        loginSession.remove(id);
    }

    public Map<UUID, PremiumStatus> getPremiumPlayers() {
        return premiumPlayers;
    }

    /**
     * Fetches the premium status of an online player.
     *
     * @param onlinePlayer
     * @return the online status or unknown if an error happened, the player isn't online or BungeeCord doesn't send
     * us the status message yet (This means you cannot check the login status on the PlayerJoinEvent).
     * @deprecated this method could be removed in future versions and exists only as a temporarily solution
     */
    @Deprecated
    public PremiumStatus getStatus(UUID onlinePlayer) {
        return premiumPlayers.getOrDefault(onlinePlayer, PremiumStatus.UNKNOWN);
    }

    /**
     * Wait before the server is fully started. This is workaround, because connections right on startup are not
     * injected by ProtocolLib
     *
     * @return true if ProtocolLib can now intercept packets
     */
    public boolean isServerFullyStarted() {
        return serverStarted;
    }

    public void setServerStarted() {
        if (!this.serverStarted) {
            this.serverStarted = true;
        }
    }

    public BungeeManager getBungeeManager() {
        return bungeeManager;
    }

    @Override
    public Path getPluginFolder() {
        return getDataFolder().toPath();
    }

    @Override
    public Logger getLog() {
        return logger;
    }

    @Override
    public BukkitScheduler getScheduler() {
        return scheduler;
    }

    @Override
    public void sendMessage(CommandSender receiver, String message) {
        receiver.sendMessage(message);
    }
}
