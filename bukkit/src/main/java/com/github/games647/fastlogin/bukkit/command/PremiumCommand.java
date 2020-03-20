package com.github.games647.fastlogin.bukkit.command;

import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.core.ConfirmationState;
import com.github.games647.fastlogin.core.StoredProfile;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Let users activate fast login by command. This only be accessible if
 * the user has access to it's account. So we can make sure that not another
 * person with a paid account and the same username can steal his account.
 */
public class PremiumCommand extends ToggleCommand {

    public PremiumCommand(FastLoginBukkit plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            onPremiumSelf(sender, command, args);
        } else {
            onPremiumOther(sender, command, args);
        }

        return true;
    }

    private void onPremiumSelf(CommandSender sender, Command cmd, String[] args) {
        if (isConsole(sender)) {
            return;
        }

        Player player = (Player) sender;
        String playerName = sender.getName();
        if (forwardPremiumCommand(sender, playerName)) {
            return;
        }

        // non-bungee mode
        if (plugin.getConfig().getBoolean("premium-confirm")) {
            ConfirmationState state = plugin.getCore().getPendingConfirms().get(playerName);
            if (state == null) {
                // no pending confirmation
                plugin.getCore().getPendingConfirms().put(playerName, ConfirmationState.REQUIRE_RELOGIN);
                player.kickPlayer(plugin.getCore().getMessage("premium-confirm"));
            } else if (state == ConfirmationState.REQUIRE_AUTH_PLUGIN_LOGIN) {
                // player logged in successful using premium authentication
                activate(sender, playerName);
            }
        } else {
            activate(sender, playerName);
        }
    }

    private void activate(CommandSender sender, String playerName) {
        plugin.getCore().getPendingConfirms().remove(playerName);

        //todo: load async
        StoredProfile profile = plugin.getCore().getStorage().loadProfile(playerName);
        if (profile.isPremium()) {
            plugin.getCore().sendLocaleMessage("already-exists", sender);
        } else {
            //todo: resolve uuid
            profile.setPremium(true);
            plugin.getScheduler().runAsync(() -> {
                plugin.getCore().getStorage().save(profile);
            });

            plugin.getCore().sendLocaleMessage("add-premium", sender);
        }
    }

    private void onPremiumOther(CommandSender sender, Command command, String[] args) {
        if (!hasOtherPermission(sender, command)) {
            return;
        }

        if (forwardPremiumCommand(sender, args[0])) {
            return;
        }

        //todo: load async
        StoredProfile profile = plugin.getCore().getStorage().loadProfile(args[0]);
        if (profile == null) {
            plugin.getCore().sendLocaleMessage("player-unknown", sender);
            return;
        }

        if (profile.isPremium()) {
            plugin.getCore().sendLocaleMessage("already-exists-other", sender);
        } else {
            //todo: resolve uuid
            profile.setPremium(true);
            plugin.getScheduler().runAsync(() -> {
                plugin.getCore().getStorage().save(profile);
            });

            plugin.getCore().sendLocaleMessage("add-premium-other", sender);
        }
    }

    private boolean forwardPremiumCommand(CommandSender sender, String target) {
        return forwardBungeeCommand(sender, target, true);
    }
}
