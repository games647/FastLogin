package com.github.games647.fastlogin.bungee.listener;

import com.github.games647.fastlogin.bungee.FastLoginBungee;
import com.github.games647.fastlogin.core.PlayerProfile;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class AsyncStatusMessage implements Runnable {

    private final FastLoginBungee plugin;
    private final ProxiedPlayer fromPlayer;
    private final String targetPlayer;
    private final boolean toPremium;

    public AsyncStatusMessage(FastLoginBungee plugin, ProxiedPlayer fromPlayer, String targetPlayer, boolean toPremium) {
        this.plugin = plugin;
        this.fromPlayer = fromPlayer;
        this.targetPlayer = targetPlayer;
        this.toPremium = toPremium;
    }

    @Override
    public void run() {
        if (toPremium) {
            activatePremium();
        } else {
            turnOffPremium();
        }
    }

    private void turnOffPremium() {
        PlayerProfile playerProfile = plugin.getCore().getStorage().getProfile(targetPlayer, true);
        if (!playerProfile.isPremium()) {
            if (fromPlayer.isConnected()) {
                TextComponent textComponent = new TextComponent("You are not in the premium list");
                textComponent.setColor(ChatColor.DARK_RED);
                fromPlayer.sendMessage(textComponent);
            }

            return;
        }

        playerProfile.setPremium(false);
        playerProfile.setUuid(null);
        plugin.getCore().getStorage().save(playerProfile);
        TextComponent textComponent = new TextComponent("Removed to the list of premium players");
        textComponent.setColor(ChatColor.DARK_GREEN);
        fromPlayer.sendMessage(textComponent);
    }

    private void activatePremium() {
        PlayerProfile playerProfile = plugin.getCore().getStorage().getProfile(targetPlayer, true);
        if (playerProfile.isPremium()) {
            if (fromPlayer.isConnected()) {
                TextComponent textComponent = new TextComponent("You are already on the premium list");
                textComponent.setColor(ChatColor.DARK_RED);
                fromPlayer.sendMessage(textComponent);
            }

            return;
        }

        playerProfile.setPremium(true);
        //todo: set uuid
        plugin.getCore().getStorage().save(playerProfile);
        TextComponent textComponent = new TextComponent("Added to the list of premium players");
        textComponent.setColor(ChatColor.DARK_GREEN);
        fromPlayer.sendMessage(textComponent);
    }
}
