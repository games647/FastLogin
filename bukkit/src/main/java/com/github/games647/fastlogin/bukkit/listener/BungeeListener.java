package com.github.games647.fastlogin.bukkit.listener;

import com.github.games647.fastlogin.bukkit.BukkitLoginSession;
import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.bukkit.task.ForceLoginTask;
import com.github.games647.fastlogin.core.PremiumStatus;
import com.github.games647.fastlogin.core.hooks.AuthPlugin;
import com.github.games647.fastlogin.core.message.LoginActionMessage;
import com.github.games647.fastlogin.core.message.LoginActionMessage.Type;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

import java.net.InetSocketAddress;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

/**
 * Responsible for receiving messages from a BungeeCord instance.
 *
 * This class also receives the plugin message from the bungeecord version of this plugin in order to get notified if
 * the connection is in online mode.
 */
public class BungeeListener implements PluginMessageListener {

    private final FastLoginBukkit plugin;

    public BungeeListener(FastLoginBukkit plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        ByteArrayDataInput dataInput = ByteStreams.newDataInput(message);

        LoginActionMessage loginMessage = new LoginActionMessage();
        loginMessage.readFrom(dataInput);

        plugin.getLog().debug("Received plugin message {}", loginMessage);

        Player targetPlayer = player;
        if (!loginMessage.getPlayerName().equals(player.getName())) {
            targetPlayer = Bukkit.getPlayerExact(loginMessage.getPlayerName());;
        }

        if (targetPlayer == null) {
            plugin.getLog().warn("Force action player {} not found", loginMessage.getPlayerName());
            return;
        }

        // fail if target player is blocked because already authenticated or wrong bungeecord id
        if (targetPlayer.hasMetadata(plugin.getName())) {
            plugin.getLog().warn("Received message {} from a blocked player {}", loginMessage, targetPlayer);
        } else {
            UUID sourceId = loginMessage.getProxyId();
            if (plugin.getBungeeManager().isProxyAllowed(sourceId)) {
                readMessage(targetPlayer, loginMessage);
            } else {
                plugin.getLog().warn("Received proxy id: {} that doesn't exist in the proxy file", sourceId);
            }
        }
    }

    private void readMessage(Player player, LoginActionMessage message) {
        String playerName = message.getPlayerName();
        Type type = message.getType();

        InetSocketAddress address = player.getAddress();
        plugin.getLog().info("Player info {} command for {} from proxy", type, playerName);
        if (type == Type.LOGIN) {
            onLoginMessage(player, playerName, address);
        } else if (type == Type.REGISTER) {
            onRegisterMessage(player, playerName, address);
        } else if (type == Type.CRACKED) {
            //we don't start a force login task here so update it manually
            plugin.getPremiumPlayers().put(player.getUniqueId(), PremiumStatus.CRACKED);
        }
    }

    private void onLoginMessage(Player player, String playerName, InetSocketAddress address) {
        BukkitLoginSession playerSession = new BukkitLoginSession(playerName, true);
        startLoginTaskIfReady(player, playerSession);
    }

    private void onRegisterMessage(Player player, String playerName, InetSocketAddress address) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            AuthPlugin<Player> authPlugin = plugin.getCore().getAuthPluginHook();
            try {
                //we need to check if the player is registered on Bukkit too
                if (authPlugin == null || !authPlugin.isRegistered(playerName)) {
                    BukkitLoginSession playerSession = new BukkitLoginSession(playerName, false);
                    startLoginTaskIfReady(player, playerSession);
                }
            } catch (Exception ex) {
                plugin.getLog().error("Failed to query isRegistered for player: {}", player, ex);
            }
        });
    }

    private void startLoginTaskIfReady(Player player, BukkitLoginSession session) {
        session.setVerified(true);
        plugin.getSessionManager().startLoginSession(player.getAddress(), session);

        // only start a new login task if the join event fired earlier. This event then didn
        boolean result = plugin.getBungeeManager().didJoinEventFired(player);
        plugin.getLog().info("Delaying force login until join event fired?: {}", result);
        if (result) {
            Runnable forceLoginTask = new ForceLoginTask(plugin.getCore(), player, session);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, forceLoginTask);
        }
    }
}
