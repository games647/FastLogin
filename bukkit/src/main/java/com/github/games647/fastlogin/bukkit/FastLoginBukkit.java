package com.github.games647.fastlogin.bukkit;

import com.github.games647.fastlogin.bukkit.commands.CrackedCommand;
import com.github.games647.fastlogin.bukkit.commands.PremiumCommand;
import com.github.games647.fastlogin.bukkit.listener.BungeeListener;
import com.github.games647.fastlogin.bukkit.listener.JoinListener;
import com.github.games647.fastlogin.bukkit.listener.protocollib.ProtocolLibListener;
import com.github.games647.fastlogin.bukkit.listener.protocollib.SkinApplyListener;
import com.github.games647.fastlogin.bukkit.listener.protocolsupport.ProtocolSupportListener;
import com.github.games647.fastlogin.bukkit.tasks.DelayedAuthHook;
import com.github.games647.fastlogin.core.CommonUtil;
import com.github.games647.fastlogin.core.messages.ChangePremiumMessage;
import com.github.games647.fastlogin.core.messages.ChannelMessage;
import com.github.games647.fastlogin.core.shared.FastLoginCore;
import com.github.games647.fastlogin.core.shared.PlatformPlugin;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageRecipient;
import org.slf4j.Logger;

/**
 * This plugin checks if a player has a paid account and if so tries to skip offline mode authentication.
 */
public class FastLoginBukkit extends JavaPlugin implements PlatformPlugin<CommandSender> {

    private final Logger logger = CommonUtil.createLoggerFromJDK(getLogger());

    private boolean bungeeCord;
    private FastLoginCore<Player, CommandSender, FastLoginBukkit> core;
    private boolean serverStarted;

    //1 minutes should be enough as a timeout for bad internet connection (Server, Client and Mojang)
    private final ConcurrentMap<String, BukkitLoginSession> loginSession = CommonUtil.buildCache(1, -1);

    @Override
    public void onEnable() {
        core = new FastLoginCore<>(this);
        core.load();
        try {
            bungeeCord = Class.forName("org.spigotmc.SpigotConfig").getDeclaredField("bungee").getBoolean(null);
        } catch (ClassNotFoundException notFoundEx) {
            //ignore server has no bungee support
        } catch (Exception ex) {
            logger.warn("Cannot check bungeecord support. You use a non-Spigot build", ex);
        }

        if (getServer().getOnlineMode()) {
            //we need to require offline to prevent a loginSession request for a offline player
            logger.error("Server have to be in offline mode");
            setEnabled(false);
            return;
        }

        PluginManager pluginManager = getServer().getPluginManager();
        if (bungeeCord) {
            setServerStarted();

            //check for incoming messages from the bungeecord version of this plugin
            getServer().getMessenger().registerIncomingPluginChannel(this, getName(), new BungeeListener(this));
            getServer().getMessenger().registerOutgoingPluginChannel(this, getName());
        } else {
            if (!core.setupDatabase()) {
                setEnabled(false);
                return;
            }

            if (pluginManager.isPluginEnabled("ProtocolSupport")) {
                pluginManager.registerEvents(new ProtocolSupportListener(this), this);
            } else if (pluginManager.isPluginEnabled("ProtocolLib")) {
                ProtocolLibListener.register(this);
                pluginManager.registerEvents(new SkinApplyListener(this), this);
            } else {
                logger.warn("Either ProtocolLib or ProtocolSupport have to be installed if you don't use BungeeCord");
            }
        }

        //delay dependency setup because we load the plugin very early where plugins are initialized yet
        getServer().getScheduler().runTaskLater(this, new DelayedAuthHook(this), 5L);

        pluginManager.registerEvents(new JoinListener(this), this);

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

        if (core != null) {
            core.close();
        }

        //remove old blacklists
        getServer().getOnlinePlayers().forEach(player -> player.removeMetadata(getName(), this));
    }

    public FastLoginCore<Player, CommandSender, FastLoginBukkit> getCore() {
        return core;
    }

    public void sendBungeeActivateMessage(CommandSender invoker, String target, boolean activate) {
        if (invoker instanceof PluginMessageRecipient) {
            ChannelMessage message = new ChangePremiumMessage(target, activate, true);
            sendPluginMessage((PluginMessageRecipient) invoker, message);
        } else {
            Optional<? extends Player> optPlayer = getServer().getOnlinePlayers().stream().findFirst();
            if (!optPlayer.isPresent()) {
                logger.info("No player online to send a plugin message to the proxy");
                return;
            }

            Player sender = optPlayer.get();
            ChannelMessage message = new ChangePremiumMessage(target, activate, false);
            sendPluginMessage(sender, message);
        }
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

    public void sendPluginMessage(PluginMessageRecipient player, ChannelMessage message) {
        if (player != null) {
            ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
            dataOutput.writeUTF(message.getChannelName());

            message.writeTo(dataOutput);
            player.sendPluginMessage(this, this.getName(), dataOutput.toByteArray());
        }
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
    public void sendMessage(CommandSender receiver, String message) {
        receiver.sendMessage(message);
    }
}
