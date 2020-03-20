package com.github.games647.fastlogin.bungee.listener;

import com.github.games647.fastlogin.bungee.BungeeLoginSession;
import com.github.games647.fastlogin.bungee.FastLoginBungee;
import com.github.games647.fastlogin.bungee.task.AsyncToggleMessage;
import com.github.games647.fastlogin.core.ConfirmationState;
import com.github.games647.fastlogin.core.StoredProfile;
import com.github.games647.fastlogin.core.message.ChangePremiumMessage;
import com.github.games647.fastlogin.core.message.NamespaceKey;
import com.github.games647.fastlogin.core.message.SuccessMessage;
import com.github.games647.fastlogin.core.shared.FastLoginCore;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

import java.util.Arrays;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class PluginMessageListener implements Listener {

    private final FastLoginBungee plugin;

    private final String successChannel;
    private final String changeChannel;

    public PluginMessageListener(FastLoginBungee plugin) {
        this.plugin = plugin;

        this.successChannel = new NamespaceKey(plugin.getName(), SuccessMessage.SUCCESS_CHANNEL).getCombinedName();
        this.changeChannel = new NamespaceKey(plugin.getName(), ChangePremiumMessage.CHANGE_CHANNEL).getCombinedName();
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent pluginMessageEvent) {
        String channel = pluginMessageEvent.getTag();
        if (pluginMessageEvent.isCancelled() || !channel.startsWith(plugin.getName().toLowerCase())) {
            return;
        }

        //the client shouldn't be able to read the messages in order to know something about server internal states
        //moreover the client shouldn't be able fake a running premium check by sending the result message
        pluginMessageEvent.setCancelled(true);

        if (!(pluginMessageEvent.getSender() instanceof Server)) {
            //check if the message is sent from the server
            return;
        }

        //so that we can safely process this in the background
        byte[] data = Arrays.copyOf(pluginMessageEvent.getData(), pluginMessageEvent.getData().length);
        ProxiedPlayer forPlayer = (ProxiedPlayer) pluginMessageEvent.getReceiver();

        plugin.getScheduler().runAsync(() -> readMessage(forPlayer, channel, data));
    }

    private void readMessage(ProxiedPlayer fromPlayer, String channel, byte[] data) {
        ByteArrayDataInput dataInput = ByteStreams.newDataInput(data);
        if (successChannel.equals(channel)) {
            onSuccessMessage(fromPlayer);
        } else if (changeChannel.equals(channel)) {
            ChangePremiumMessage changeMessage = new ChangePremiumMessage();
            changeMessage.readFrom(dataInput);

            String playerName = changeMessage.getPlayerName();
            boolean isSourceInvoker = changeMessage.isSourceInvoker();
            onChangeMessage(fromPlayer, changeMessage.shouldEnable(), playerName, isSourceInvoker);
        }
    }

    private void onChangeMessage(ProxiedPlayer fromPlayer, boolean shouldEnable, String playerName, boolean isSourceInvoker) {
        FastLoginCore<ProxiedPlayer, CommandSender, FastLoginBungee> core = plugin.getCore();
        if (shouldEnable) {
            if (!isSourceInvoker) {
                // fromPlayer is not the target player
                activePremiumLogin(fromPlayer, playerName, false);
                return;
            }

            if (plugin.getCore().getConfig().getBoolean("premium-confirm", true)) {
                ConfirmationState state = plugin.getCore().getPendingConfirms().get(playerName);
                if (state == null) {
                    // no pending confirmation
                    core.sendLocaleMessage("premium-confirm", fromPlayer);
                    core.getPendingConfirms().put(playerName, ConfirmationState.REQUIRE_RELOGIN);
                } else if (state == ConfirmationState.REQUIRE_AUTH_PLUGIN_LOGIN) {
                    // player logged in successful using premium authentication
                    activePremiumLogin(fromPlayer, playerName, true);
                }
            } else {
                activePremiumLogin(fromPlayer, playerName, true);
            }
        } else {
            Runnable task = new AsyncToggleMessage(core, fromPlayer, playerName, false, isSourceInvoker);
            plugin.getScheduler().runAsync(task);
        }
    }

    private void activePremiumLogin(ProxiedPlayer fromPlayer, String playerName, boolean isSourceInvoker) {
        plugin.getCore().getPendingConfirms().remove(playerName);
        Runnable task = new AsyncToggleMessage(plugin.getCore(), fromPlayer, playerName, true, isSourceInvoker);
        plugin.getScheduler().runAsync(task);
    }

    private void onSuccessMessage(ProxiedPlayer forPlayer) {
        if (forPlayer.getPendingConnection().isOnlineMode()) {
            //bukkit module successfully received and force logged in the user
            //update only on success to prevent corrupt data
            BungeeLoginSession loginSession = plugin.getSession().get(forPlayer.getPendingConnection());
            StoredProfile playerProfile = loginSession.getProfile();
            loginSession.setRegistered(true);

            if (!loginSession.isAlreadySaved()) {
                loginSession.setAlreadySaved(true);

                playerProfile.setId(loginSession.getUuid());
                playerProfile.setPremium(true);

                plugin.getCore().getStorage().save(playerProfile);
            }
        }
    }
}
