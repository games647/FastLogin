package com.github.games647.fastlogin.bungee;

import com.github.games647.fastlogin.bungee.hooks.BungeeAuthHook;
import com.github.games647.fastlogin.bungee.listener.ConnectListener;
import com.github.games647.fastlogin.bungee.listener.MessageListener;
import com.github.games647.fastlogin.core.mojang.MojangApiConnector;
import com.github.games647.fastlogin.core.shared.FastLoginCore;
import com.github.games647.fastlogin.core.shared.PlatformPlugin;
import com.google.common.collect.Maps;
import com.google.common.net.HostAndPort;

import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadFactory;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.GroupedThreadFactory;

/**
 * BungeeCord version of FastLogin. This plugin keeps track on online mode connections.
 */
public class FastLoginBungee extends Plugin implements PlatformPlugin<CommandSender> {

    private final ConcurrentMap<PendingConnection, BungeeLoginSession> session = Maps.newConcurrentMap();

    private FastLoginCore<ProxiedPlayer, CommandSender, FastLoginBungee> core;

    @Override
    public void onEnable() {
        core = new FastLoginCore<>(this);
        core.load();
        if (!core.setupDatabase()) {
            return;
        }

        //events
        getProxy().getPluginManager().registerListener(this, new ConnectListener(this));
        getProxy().getPluginManager().registerListener(this, new MessageListener(this));

        //this is required to listen to messages from the server
        getProxy().registerChannel(getDescription().getName());

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
            getLogger().info("Hooked into BungeeAuth");
        }
    }

    @Override
    public String getName() {
        return getDescription().getName();
    }

    @Override
    public void sendMessage(CommandSender receiver, String message) {
        receiver.sendMessage(TextComponent.fromLegacyText(message));
    }

    @Override
    @SuppressWarnings("deprecation")
    public ThreadFactory getThreadFactory() {
        return new GroupedThreadFactory(this, getName());
    }

    @Override
    public MojangApiConnector makeApiConnector(List<String> addresses, int requests, List<HostAndPort> proxies) {
        return new MojangApiConnector(getLogger(), addresses, requests, proxies);
    }
}
