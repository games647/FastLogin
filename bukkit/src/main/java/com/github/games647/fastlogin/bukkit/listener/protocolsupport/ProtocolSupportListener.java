package com.github.games647.fastlogin.bukkit.listener.protocolsupport;

import com.github.games647.fastlogin.bukkit.BukkitLoginSession;
import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.core.shared.JoinManagement;
import com.github.games647.fastlogin.core.PlayerProfile;

import java.net.InetSocketAddress;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import protocolsupport.api.events.PlayerLoginStartEvent;
import protocolsupport.api.events.PlayerPropertiesResolveEvent;

public class ProtocolSupportListener extends JoinManagement<Player, ProtocolLoginSource> implements Listener {

    private final FastLoginBukkit plugin;

    public ProtocolSupportListener(FastLoginBukkit plugin) {
        super(plugin.getCore(), plugin.getCore().getAuthPluginHook());

        this.plugin = plugin;
    }

    @EventHandler
    public void onLoginStart(PlayerLoginStartEvent loginStartEvent) {
        plugin.setServerStarted();
        if (loginStartEvent.isLoginDenied() || plugin.getCore().getAuthPluginHook() == null) {
            return;
        }

        String username = loginStartEvent.getName();
        InetSocketAddress address = loginStartEvent.getAddress();

        //remove old data every time on a new login in order to keep the session only for one person
        plugin.getSessions().remove(address.toString());

        super.onLogin(username, new ProtocolLoginSource(loginStartEvent));
    }

    @EventHandler
    public void onPropertiesResolve(PlayerPropertiesResolveEvent propertiesResolveEvent) {
        InetSocketAddress address = propertiesResolveEvent.getAddress();
        BukkitLoginSession session = plugin.getSessions().get(address.toString());

        //skin was resolved -> premium player
        if (propertiesResolveEvent.hasProperty("textures") && session != null) {
            String ip = address.getAddress().getHostAddress();
            plugin.getCore().getPendingLogins().remove(ip + session.getUsername());

            session.setVerified(true);
        }
    }

    @Override
    public void requestPremiumLogin(ProtocolLoginSource source, PlayerProfile profile, String username, boolean registered) {
        source.setOnlineMode();

        String ip = source.getAddress().getAddress().getHostAddress();
        plugin.getCore().getPendingLogins().put(ip + username, new Object());

        BukkitLoginSession playerSession = new BukkitLoginSession(username, null, null, registered, profile);
        plugin.getSessions().put(source.getAddress().toString(), playerSession);
        if (plugin.getConfig().getBoolean("premiumUuid")) {
            source.getLoginStartEvent().setUseOnlineModeUUID(true);
        }
    }

    @Override
    public void startCrackedSession(ProtocolLoginSource source, PlayerProfile profile, String username) {
        BukkitLoginSession loginSession = new BukkitLoginSession(username, profile);
        plugin.getSessions().put(source.getAddress().toString(), loginSession);
    }
}
