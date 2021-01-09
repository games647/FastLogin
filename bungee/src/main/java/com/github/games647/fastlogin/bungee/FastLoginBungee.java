package com.github.games647.fastlogin.bungee;

import com.github.games647.fastlogin.bungee.hook.BungeeAuthHook;
import com.github.games647.fastlogin.bungee.listener.ConnectListener;
import com.github.games647.fastlogin.bungee.listener.PluginMessageListener;
import com.github.games647.fastlogin.core.AsyncScheduler;
import com.github.games647.fastlogin.core.CommonUtil;
import com.github.games647.fastlogin.core.message.ChangePremiumMessage;
import com.github.games647.fastlogin.core.message.ChannelMessage;
import com.github.games647.fastlogin.core.message.NamespaceKey;
import com.github.games647.fastlogin.core.message.SuccessMessage;
import com.github.games647.fastlogin.core.shared.FastLoginCore;
import com.github.games647.fastlogin.core.shared.PlatformPlugin;
import com.google.common.collect.MapMaker;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadFactory;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.api.scheduler.GroupedThreadFactory;

import org.slf4j.Logger;

/**
 * BungeeCord version of FastLogin. This plugin keeps track on online mode connections.
 */
public class FastLoginBungee extends Plugin implements PlatformPlugin<CommandSender> {

    private final ConcurrentMap<PendingConnection, BungeeLoginSession> session = new MapMaker().weakKeys().makeMap();

    private FastLoginCore<ProxiedPlayer, CommandSender, FastLoginBungee> core;
    private AsyncScheduler scheduler;
    private Logger logger;

    @Override
    public void onEnable() {
        logger = CommonUtil.createLoggerFromJDK(getLogger());
        scheduler = new AsyncScheduler(logger, getThreadFactory());

        core = new FastLoginCore<>(this);
        core.load();
        if (!core.setupDatabase()) {
            return;
        }

        //events
        PluginManager pluginManager = getProxy().getPluginManager();
        boolean floodgateAvail = pluginManager.getPlugin("floodgate") != null;
        ConnectListener connectListener = new ConnectListener(this, core.getRateLimiter(), floodgateAvail);

        pluginManager.registerListener(this, connectListener);
        pluginManager.registerListener(this, new PluginMessageListener(this));

        //this is required to listen to incoming messages from the server
        getProxy().registerChannel(NamespaceKey.getCombined(getName(), ChangePremiumMessage.CHANGE_CHANNEL));
        getProxy().registerChannel(NamespaceKey.getCombined(getName(), SuccessMessage.SUCCESS_CHANNEL));

        registerHook();
    }

    @Override
    public void onDisable() {
        if (core != null) {
            core.close();
        }
    }

    public FastLoginCore<ProxiedPlayer, CommandSender, FastLoginBungee> getCore() {
        return core;
    }

    public ConcurrentMap<PendingConnection, BungeeLoginSession> getSession() {
        return session;
    }

    private void registerHook() {
        Plugin plugin = getProxy().getPluginManager().getPlugin("BungeeAuth");
        if (plugin != null) {
            core.setAuthPluginHook(new BungeeAuthHook());
            logger.info("Hooked into BungeeAuth");
        }
        Plugin plugin2 = getProxy().getPluginManager().getPlugin("BungeeCordAuthenticatorBungee");
        if (plugin2 != null) {
            core.setAuthPluginHook(new BungeeCordAuthenticatorHook(plugin2));
            logger.info("Hooked into BungeeCordAuthenticator");
        }
    }

    public void sendPluginMessage(Server server, ChannelMessage message) {
        if (server != null) {
            ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
            message.writeTo(dataOutput);

            NamespaceKey channel = new NamespaceKey(getName(), message.getChannelName());
            server.sendData(channel.getCombinedName(), dataOutput.toByteArray());
        }
    }

    @Override
    public String getName() {
        return getDescription().getName();
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
        receiver.sendMessage(TextComponent.fromLegacyText(message));
    }

    @Override
    @SuppressWarnings("deprecation")
    public ThreadFactory getThreadFactory() {
        return new ThreadFactoryBuilder()
                .setNameFormat(getName() + " Pool Thread #%1$d")
                //Hikari create daemons by default
                .setDaemon(true)
                .setThreadFactory(new GroupedThreadFactory(this, getName()))
                .build();
    }

    @Override
    public AsyncScheduler getScheduler() {
        return scheduler;
    }
}
