package com.github.games647.fastlogin.bukkit;

import com.github.games647.fastlogin.bukkit.auth.proxy.ProxyManager;
import com.github.games647.fastlogin.bukkit.auth.protocollib.ProtocolLibListener;
import com.github.games647.fastlogin.bukkit.auth.protocolsupport.ProtocolSupportListener;
import com.github.games647.fastlogin.bukkit.command.CrackedCommand;
import com.github.games647.fastlogin.bukkit.command.PremiumCommand;
import com.github.games647.fastlogin.bukkit.hook.DelayedAuthHook;
import com.github.games647.fastlogin.bukkit.listener.ConnectionListener;
import com.github.games647.fastlogin.bukkit.listener.PaperPreLoginListener;
import com.github.games647.fastlogin.core.CommonUtil;
import com.github.games647.fastlogin.core.PremiumStatus;
import com.github.games647.fastlogin.core.storage.StoredProfile;
import com.github.games647.fastlogin.core.shared.FastLoginCore;
import com.github.games647.fastlogin.core.shared.PlatformPlugin;

import io.papermc.lib.PaperLib;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

/**
 * This plugin checks if a player has a paid account and if so tries to skip offline mode authentication.
 */
public class FastLoginBukkit extends JavaPlugin implements PlatformPlugin<CommandSender> {

    private final BukkitSessionManager sessionManager = new BukkitSessionManager();
    private final Map<UUID, PremiumStatus> premiumPlayers = new ConcurrentHashMap<>();
    private final FastLoginCore<Player, CommandSender, FastLoginBukkit> core = new FastLoginCore<>(this);

    private final Logger logger;
    private final BukkitScheduler scheduler;

    private ProxyManager proxyManager;

    private PremiumPlaceholder premiumPlaceholder;

    public FastLoginBukkit() {
        this.logger = CommonUtil.createLoggerFromJDK(getLogger());
        this.scheduler = new BukkitScheduler(this, logger, getThreadFactory());
    }

    @Override
    public void onEnable() {
        core.load();

        if (getServer().getOnlineMode()) {
            //we need to require offline to prevent a loginSession request for a offline player
            logger.error("Server has to be in offline mode");
            setEnabled(false);
            return;
        }

        proxyManager = new ProxyManager(this);
        proxyManager.initialize();
        
        PluginManager pluginManager = getServer().getPluginManager();
        if (!proxyManager.isEnabled()) {
            if (!core.setupDatabase()) {
                setEnabled(false);
                return;
            }

            if (pluginManager.isPluginEnabled("ProtocolSupport")) {
                pluginManager.registerEvents(new ProtocolSupportListener(this, core.getRateLimiter()), this);
            } else if (pluginManager.isPluginEnabled("ProtocolLib")) {
                ProtocolLibListener.register(this, core.getRateLimiter());
            } else {
                logger.warn("Either ProtocolLib or ProtocolSupport have to be installed if you don't use proxies");
            }
        }

        //delay dependency setup because we load the plugin very early where plugins are initialized yet
        getServer().getScheduler().runTaskLater(this, new DelayedAuthHook(this), 5L);

        pluginManager.registerEvents(new ConnectionListener(this), this);

        //if server is using paper - we need to add one more listener to correct the usercache usage
        if (PaperLib.isPaper()) {
            pluginManager.registerEvents(new PaperPreLoginListener(this), this);
        }

        //register commands using a unique name
        getCommand("premium").setExecutor(new PremiumCommand(this));
        getCommand("cracked").setExecutor(new CrackedCommand(this));

        if (pluginManager.isPluginEnabled("PlaceholderAPI")) {
            premiumPlaceholder = new PremiumPlaceholder(this);
            premiumPlaceholder.register();
        }
    }

    @Override
    public void onDisable() {
        premiumPlayers.clear();
        core.close();

        proxyManager.cleanup();
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI") && premiumPlaceholder != null) {
            premiumPlaceholder.unregister();
        }
    }

    public FastLoginCore<Player, CommandSender, FastLoginBukkit> getCore() {
        return core;
    }

    /**
     * Fetches the premium status of an online player.
     *
     * @param onlinePlayer
     * @return the online status or unknown if an error happened, the player isn't online or a proxy doesn't send
     * us the status message yet (This means you cannot check the login status on the PlayerJoinEvent).
     * @deprecated this method could be removed in future versions and exists only as a temporarily solution
     */
    @Deprecated
    public PremiumStatus getStatus(UUID onlinePlayer) {
        StoredProfile playSession = sessionManager.getPlaySession(onlinePlayer);
        return Optional.ofNullable(playSession).map(profile -> {
            if (profile.isPremium())
                return PremiumStatus.PREMIUM;
            return PremiumStatus.CRACKED;
        }).orElse(PremiumStatus.UNKNOWN);
    }

    /**
     * Gets a thread-safe map about players which are connecting to the server are being checked to be premium (paid
     * account)
     *
     * @return a thread-safe loginSession map
     */
    public BukkitSessionManager getSessionManager() {
        return sessionManager;
    }

    public Map<UUID, PremiumStatus> getPremiumPlayers() {
        return premiumPlayers;
    }

    public ProxyManager getProxyManager() {
        return proxyManager;
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
