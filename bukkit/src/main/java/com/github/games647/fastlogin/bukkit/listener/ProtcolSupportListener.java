package com.github.games647.fastlogin.bukkit.listener;

import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.bukkit.PlayerSession;

import java.net.InetSocketAddress;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import protocolsupport.api.events.PlayerLoginStartEvent;
import protocolsupport.api.events.PlayerPropertiesResolveEvent;

public class ProtcolSupportListener implements Listener {

    private static final long DELAY_LOGIN = 1 * 20L / 2;

    protected final FastLoginBukkit plugin;

    public ProtcolSupportListener(FastLoginBukkit plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onLoginStart(PlayerLoginStartEvent loginStartEvent) {
        if (loginStartEvent.isLoginDenied()) {
            return;
        }

        String playerName = loginStartEvent.getName();
        if (plugin.getEnabledPremium().contains(playerName)) {
            loginStartEvent.setOnlineMode(true);
            InetSocketAddress address = loginStartEvent.getAddress();

            PlayerSession playerSession = new PlayerSession(playerName, null, null);
            plugin.getSessions().put(address.toString(), playerSession);
//            loginStartEvent.setUseOnlineModeUUID(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPropertiesResolve(PlayerPropertiesResolveEvent propertiesResolveEvent) {
        InetSocketAddress address = propertiesResolveEvent.getAddress();
        PlayerSession session = plugin.getSessions().get(address.toString());
        if (session != null) {
            session.setVerified(true);

            PlayerPropertiesResolveEvent.ProfileProperty skinProperty = propertiesResolveEvent.getProperties()
                    .get("textures");
            if (skinProperty != null) {
                WrappedSignedProperty signedProperty = WrappedSignedProperty
                        .fromValues(skinProperty.getName(), skinProperty.getValue(), skinProperty.getSignature());
                session.setSkin(signedProperty);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent joinEvent) {
        final Player player = joinEvent.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {

            @Override
            public void run() {
                String address = player.getAddress().getAddress().toString();
                //removing the session because we now use it
                PlayerSession session = plugin.getSessions().remove(address);

                if (player.isOnline()) {
                    //check if it's the same player as we checked before
                    if (session != null && player.getName().equals(session.getUsername()) && session.isVerified()) {
                        plugin.getLogger().log(Level.FINE, "Logging player {0} in", player.getName());
                        plugin.getAuthPlugin().forceLogin(player);
                    }
                }
            }
            //Wait before auth plugin and we received a message from BungeeCord initializes the player
        }, DELAY_LOGIN);
    }
}
