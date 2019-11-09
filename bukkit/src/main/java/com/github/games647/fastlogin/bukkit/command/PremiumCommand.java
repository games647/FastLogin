package com.github.games647.fastlogin.bukkit.command;

import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.bukkit.event.BukkitFastLoginPremiumToggleEvent;
import com.github.games647.fastlogin.core.StoredProfile;

import java.util.UUID;

import com.github.games647.fastlogin.core.shared.event.PremiumToggleReason;
import org.bukkit.Bukkit;
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

        if (forwardPremiumCommand(sender, sender.getName())) {
            return;
        }

        UUID id = ((Player) sender).getUniqueId();
        if (plugin.getConfig().getBoolean("premium-warning") && !plugin.getCore().getPendingConfirms().contains(id)) {
            sender.sendMessage(plugin.getCore().getMessage("premium-warning"));
            plugin.getCore().getPendingConfirms().add(id);
            return;
        }

        plugin.getCore().getPendingConfirms().remove(id);
        //todo: load async
        StoredProfile profile = plugin.getCore().getStorage().loadProfile(sender.getName());
        if (profile.isPremium()) {
            plugin.getCore().sendLocaleMessage("already-exists", sender);
        } else {
            //todo: resolve uuid
            profile.setPremium(true);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getCore().getStorage().save(profile);
                plugin.getServer().getPluginManager().callEvent(
                        new BukkitFastLoginPremiumToggleEvent(profile, PremiumToggleReason.COMMAND_SELF));
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
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getCore().getStorage().save(profile);
                plugin.getServer().getPluginManager().callEvent(
                        new BukkitFastLoginPremiumToggleEvent(profile, PremiumToggleReason.COMMAND_OTHER));
            });

            plugin.getCore().sendLocaleMessage("add-premium-other", sender);
        }
    }

    private boolean forwardPremiumCommand(CommandSender sender, String target) {
        return forwardBungeeCommand(sender, target, true);
    }
}
