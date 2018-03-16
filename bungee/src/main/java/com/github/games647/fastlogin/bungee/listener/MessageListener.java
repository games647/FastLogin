package com.github.games647.fastlogin.bungee.listener;

import com.github.games647.fastlogin.bungee.BungeeLoginSession;
import com.github.games647.fastlogin.bungee.FastLoginBungee;
import com.github.games647.fastlogin.bungee.tasks.AsyncToggleMessage;
import com.github.games647.fastlogin.core.StoredProfile;
import com.github.games647.fastlogin.core.messages.ChangePremiumMessage;
import com.github.games647.fastlogin.core.shared.FastLoginCore;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

import java.util.Arrays;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class MessageListener implements Listener {

    private final FastLoginBungee plugin;

    public MessageListener(FastLoginBungee plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent pluginMessageEvent) {
        String channel = pluginMessageEvent.getTag();
        if (pluginMessageEvent.isCancelled() || !plugin.getName().equals(channel)) {
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

        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> readMessage(forPlayer, data));
    }

    private void readMessage(ProxiedPlayer forPlayer, byte[] data) {
        FastLoginCore<ProxiedPlayer, CommandSender, FastLoginBungee> core = plugin.getCore();

        ByteArrayDataInput dataInput = ByteStreams.newDataInput(data);
        String subChannel = dataInput.readUTF();
        if ("Success".equals(subChannel)) {
            onSuccessMessage(forPlayer);
        } else if ("ChangeStatus".equals(subChannel)) {
            ChangePremiumMessage changeMessage = new ChangePremiumMessage();
            changeMessage.readFrom(dataInput);

            String playerName = changeMessage.getPlayerName();
            boolean isSourceInvoker = changeMessage.isSourceInvoker();
            if (changeMessage.isWillEnable()) {
                if (playerName.equals(forPlayer.getName()) && plugin.getCore().getConfig().get("premium-warning", true)
                        && !core.getPendingConfirms().contains(forPlayer.getUniqueId())) {
                    String message = core.getMessage("premium-warning");
                    forPlayer.sendMessage(TextComponent.fromLegacyText(message));
                    core.getPendingConfirms().add(forPlayer.getUniqueId());
                    return;
                }

                core.getPendingConfirms().remove(forPlayer.getUniqueId());
                Runnable task = new AsyncToggleMessage(core, forPlayer, playerName, true, isSourceInvoker);
                ProxyServer.getInstance().getScheduler().runAsync(plugin, task);
            } else {
                Runnable task = new AsyncToggleMessage(core, forPlayer, playerName, false, isSourceInvoker);
                ProxyServer.getInstance().getScheduler().runAsync(plugin, task);
            }
        }
    }

    private void onSuccessMessage(ProxiedPlayer forPlayer) {
        if (forPlayer.getPendingConnection().isOnlineMode()) {
            //bukkit module successfully received and force logged in the user
            //update only on success to prevent corrupt data
            BungeeLoginSession loginSession = plugin.getSession().get(forPlayer.getPendingConnection());
            StoredProfile playerProfile = loginSession.getProfile();
            loginSession.setRegistered(true);

            if (!loginSession.isAlreadySaved()) {
                playerProfile.setPremium(true);
                plugin.getCore().getStorage().save(playerProfile);
                loginSession.setAlreadySaved(true);
            }
        }
    }
}
