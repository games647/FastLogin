package com.github.games647.fastlogin.bungee.tasks;

import com.github.games647.fastlogin.bungee.BungeeCore;
import com.github.games647.fastlogin.core.PlayerProfile;

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class AsyncToggleMessage implements Runnable {

    private final BungeeCore core;
    private final ProxiedPlayer fromPlayer;
    private final String targetPlayer;
    private final boolean toPremium;

    public AsyncToggleMessage(BungeeCore core, ProxiedPlayer fromPlayer, String targetPlayer, boolean toPremium) {
        this.core = core;
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
        PlayerProfile playerProfile = core.getStorage().loadProfile(targetPlayer);
        //existing player is already cracked
        if (playerProfile.getUserId() != -1 && !playerProfile.isPremium()) {
            fromPlayer.sendMessage(TextComponent.fromLegacyText(core.getMessage("not-premium")));
            return;
        }

        playerProfile.setPremium(false);
        playerProfile.setUuid(null);
        core.getStorage().save(playerProfile);
        fromPlayer.sendMessage(TextComponent.fromLegacyText(core.getMessage("remove-premium")));
    }

    private void activatePremium() {
        PlayerProfile playerProfile = core.getStorage().loadProfile(targetPlayer);
        if (playerProfile.isPremium()) {
            fromPlayer.sendMessage(TextComponent.fromLegacyText(core.getMessage("already-exists")));
            return;
        }

        playerProfile.setPremium(true);
        core.getStorage().save(playerProfile);
        fromPlayer.sendMessage(TextComponent.fromLegacyText(core.getMessage("add-premium")));
    }
}
