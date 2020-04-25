package com.github.games647.fastlogin.bukkit.listener.protocolsupport;

import com.github.games647.craftapi.UUIDAdapter;
import com.github.games647.fastlogin.bukkit.BukkitLoginSession;
import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.bukkit.event.BukkitFastLoginPreLoginEvent;
import com.github.games647.fastlogin.core.StoredProfile;
import com.github.games647.fastlogin.core.shared.JoinManagement;
import com.github.games647.fastlogin.core.shared.event.FastLoginPreLoginEvent;

import java.net.InetSocketAddress;
import java.util.Optional;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import protocolsupport.api.events.ConnectionCloseEvent;
import protocolsupport.api.events.PlayerLoginStartEvent;
import protocolsupport.api.events.PlayerProfileCompleteEvent;

public class ProtocolSupportListener extends JoinManagement<Player, CommandSender, ProtocolLoginSource>
        implements Listener {

    private final FastLoginBukkit plugin;

    public ProtocolSupportListener(FastLoginBukkit plugin) {
        super(plugin.getCore(), plugin.getCore().getAuthPluginHook());

        this.plugin = plugin;
    }

    @EventHandler
    public void onLoginStart(PlayerLoginStartEvent loginStartEvent) {
        if (loginStartEvent.isLoginDenied() || plugin.getCore().getAuthPluginHook() == null) {
            return;
        }

        String username = loginStartEvent.getConnection().getProfile().getName();
        InetSocketAddress address = loginStartEvent.getAddress();

        //remove old data every time on a new login in order to keep the session only for one person
        plugin.removeSession(address);

        super.onLogin(username, new ProtocolLoginSource(loginStartEvent));
    }

    @EventHandler
    public void onConnectionClosed(ConnectionCloseEvent closeEvent) {
        InetSocketAddress address = closeEvent.getConnection().getAddress();
        plugin.removeSession(address);
    }

    @EventHandler
    public void onPropertiesResolve(PlayerProfileCompleteEvent profileCompleteEvent) {
        InetSocketAddress address = profileCompleteEvent.getAddress();
        BukkitLoginSession session = plugin.getSession(address);

        if (session != null && profileCompleteEvent.getConnection().getProfile().isOnlineMode()) {
            session.setVerified(true);

            if (!plugin.getConfig().getBoolean("premiumUuid")) {
                String username = Optional.ofNullable(profileCompleteEvent.getForcedName())
                        .orElse(profileCompleteEvent.getConnection().getProfile().getName());
                profileCompleteEvent.setForcedUUID(UUIDAdapter.generateOfflineId(username));
            }
        }
    }

    @Override
    public FastLoginPreLoginEvent callFastLoginPreLoginEvent(String username, ProtocolLoginSource source, StoredProfile profile) {
        BukkitFastLoginPreLoginEvent event = new BukkitFastLoginPreLoginEvent(username, source, profile);
        plugin.getServer().getPluginManager().callEvent(event);
        return event;
    }

    @Override
    public void requestPremiumLogin(ProtocolLoginSource source, StoredProfile profile, String username
            , boolean registered) {
        source.setOnlineMode();

        String ip = source.getAddress().getAddress().getHostAddress();
        plugin.getCore().getPendingLogin().put(ip + username, new Object());

        BukkitLoginSession playerSession = new BukkitLoginSession(username, registered, profile);
        plugin.putSession(source.getAddress(), playerSession);
        if (plugin.getConfig().getBoolean("premiumUuid")) {
            source.getLoginStartEvent().setOnlineMode(true);
        }
    }

    @Override
    public void startCrackedSession(ProtocolLoginSource source, StoredProfile profile, String username) {
        BukkitLoginSession loginSession = new BukkitLoginSession(username, profile);
        plugin.putSession(source.getAddress(), loginSession);
    }
}
