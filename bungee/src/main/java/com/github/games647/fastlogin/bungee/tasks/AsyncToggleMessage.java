package com.github.games647.fastlogin.bungee.tasks;

import com.github.games647.fastlogin.bungee.FastLoginBungee;
import com.github.games647.fastlogin.core.PlayerProfile;

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class AsyncToggleMessage implements Runnable {

    private final FastLoginBungee plugin;
    private final ProxiedPlayer fromPlayer;
    private final String targetPlayer;
    private final boolean toPremium;

    public AsyncToggleMessage(FastLoginBungee plugin, ProxiedPlayer fromPlayer, String targetPlayer
            , boolean toPremium) {
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
        PlayerProfile playerProfile = plugin.getCore().getStorage().loadProfile(targetPlayer);
        //existing player is already cracked
        if (playerProfile.getUserId() != -1 && !playerProfile.isPremium()) {
            fromPlayer.sendMessage(TextComponent.fromLegacyText(plugin.getCore().getMessage("not-premium")));
            return;
        }

        playerProfile.setPremium(false);
        playerProfile.setUuid(null);
        plugin.getCore().getStorage().save(playerProfile);
        fromPlayer.sendMessage(TextComponent.fromLegacyText(plugin.getCore().getMessage("remove-premium")));
    }

    private void activatePremium() {
        PlayerProfile playerProfile = plugin.getCore().getStorage().loadProfile(targetPlayer);
        if (playerProfile.isPremium()) {
            fromPlayer.sendMessage(TextComponent.fromLegacyText(plugin.getCore().getMessage("already-exists")));
            return;
        }

        playerProfile.setPremium(true);
        plugin.getCore().getStorage().save(playerProfile);
        fromPlayer.sendMessage(TextComponent.fromLegacyText(plugin.getCore().getMessage("add-premium")));
    }
}
