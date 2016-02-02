package com.github.games647.fastlogin.bukkit.listener;

import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.bukkit.PlayerSession;

import java.net.InetSocketAddress;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import protocolsupport.api.events.PlayerLoginStartEvent;
import protocolsupport.api.events.PlayerPropertiesResolveEvent;
import protocolsupport.api.events.PlayerPropertiesResolveEvent.ProfileProperty;

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

        //remove old data every time on a new login in order to keep the session only for one person
        plugin.getSessions().remove(playerName);
        if (plugin.getEnabledPremium().contains(playerName)) {
            //the player have to be registered in order to invoke the command
            startPremiumSession(playerName, loginStartEvent, true);
        } else if (plugin.getConfig().getBoolean("autologin") && !plugin.getAuthPlugin().isRegistered(playerName)) {
            startPremiumSession(playerName, loginStartEvent, false);
            plugin.getEnabledPremium().add(playerName);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPropertiesResolve(PlayerPropertiesResolveEvent propertiesResolveEvent) {
        InetSocketAddress address = propertiesResolveEvent.getAddress();
        PlayerSession session = plugin.getSessions().get(address.toString());
        if (session != null) {
            session.setVerified(true);

            ProfileProperty skinProperty = propertiesResolveEvent.getProperties().get("textures");
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
                        if (session.needsRegistration()) {
                            plugin.getLogger().log(Level.FINE, "Register player {0}", player.getName());

                            String generatedPassword = plugin.generateStringPassword();
                            plugin.getAuthPlugin().forceRegister(player, generatedPassword);
                            player.sendMessage(ChatColor.DARK_GREEN + "Auto registered with password: "
                                    + generatedPassword);
                            player.sendMessage(ChatColor.DARK_GREEN + "You may want change it?");
                        } else {
                            plugin.getLogger().log(Level.FINE, "Logging player {0} in", player.getName());
                            plugin.getAuthPlugin().forceLogin(player);
                            player.sendMessage(ChatColor.DARK_GREEN + "Auto logged in");
                        }
                    }
                }
            }
            //Wait before auth plugin and we received a message from BungeeCord initializes the player
        }, DELAY_LOGIN);
    }

    private void startPremiumSession(String playerName, PlayerLoginStartEvent loginStartEvent, boolean registered) {
        if (plugin.getApiConnector().isPremiumName(playerName)) {
            loginStartEvent.setOnlineMode(true);
            InetSocketAddress address = loginStartEvent.getAddress();

            PlayerSession playerSession = new PlayerSession(playerName, null, null);
            playerSession.setRegistered(registered);
            plugin.getSessions().put(address.toString(), playerSession);
//            loginStartEvent.setUseOnlineModeUUID(true);
        }
    }
}
