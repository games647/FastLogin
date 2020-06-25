package com.github.games647.fastlogin.bungee.task;

import com.github.games647.fastlogin.bungee.FastLoginBungee;
import com.github.games647.fastlogin.bungee.event.BungeeFastLoginPremiumToggleEvent;
import com.github.games647.fastlogin.core.StoredProfile;
import com.github.games647.fastlogin.core.shared.FastLoginCore;

import com.github.games647.fastlogin.core.shared.event.FastLoginPremiumToggleEvent;
import com.github.games647.fastlogin.core.shared.event.FastLoginPremiumToggleEvent.PremiumToggleReason;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class AsyncToggleMessage implements Runnable {

    private final FastLoginCore<ProxiedPlayer, CommandSender, FastLoginBungee> core;
    private final ProxiedPlayer sender;
    private final String targetPlayer;
    private final boolean toPremium;
    private final boolean isPlayerSender;

    public AsyncToggleMessage(FastLoginCore<ProxiedPlayer, CommandSender, FastLoginBungee> core,
             ProxiedPlayer sender, String playerName, boolean toPremium, boolean playerSender) {
        this.core = core;
        this.sender = sender;
        this.targetPlayer = playerName;
        this.toPremium = toPremium;
        this.isPlayerSender = playerSender;
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
        StoredProfile playerProfile = core.getStorage().loadProfile(targetPlayer);
        //existing player is already cracked
        if (playerProfile.isSaved() && !playerProfile.isPremium()) {
            sendMessage("not-premium");
            return;
        }

        playerProfile.setPremium(false);
        playerProfile.setId(null);
        core.getStorage().save(playerProfile);
        PremiumToggleReason reason = (!isPlayerSender || !sender.getName().equalsIgnoreCase(playerProfile.getName())) ?
                PremiumToggleReason.COMMAND_OTHER : PremiumToggleReason.COMMAND_SELF;
        core.getPlugin().getProxy().getPluginManager().callEvent(
                new BungeeFastLoginPremiumToggleEvent(playerProfile, reason));
        sendMessage("remove-premium");
    }

    private void activatePremium() {
        StoredProfile playerProfile = core.getStorage().loadProfile(targetPlayer);
        if (playerProfile.isPremium()) {
            sendMessage("already-exists");
            return;
        }

        playerProfile.setPremium(true);
        core.getStorage().save(playerProfile);
        PremiumToggleReason reason = (!isPlayerSender || !sender.getName().equalsIgnoreCase(playerProfile.getName())) ?
                PremiumToggleReason.COMMAND_OTHER : PremiumToggleReason.COMMAND_SELF;
        core.getPlugin().getProxy().getPluginManager().callEvent(
                new BungeeFastLoginPremiumToggleEvent(playerProfile, reason));
        sendMessage("add-premium");
    }

    private void sendMessage(String localeId) {
        String message = core.getMessage(localeId);
        if (isPlayerSender) {
            sender.sendMessage(TextComponent.fromLegacyText(message));
        } else {
            CommandSender console = ProxyServer.getInstance().getConsole();
            console.sendMessage(TextComponent.fromLegacyText(message));
        }
    }
}
