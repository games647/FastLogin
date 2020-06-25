package com.github.games647.fastlogin.bukkit.command;

import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.bukkit.event.BukkitFastLoginPremiumToggleEvent;
import com.github.games647.fastlogin.core.StoredProfile;

import com.github.games647.fastlogin.core.shared.event.FastLoginPremiumToggleEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import static com.github.games647.fastlogin.core.shared.event.FastLoginPremiumToggleEvent.*;

public class CrackedCommand extends ToggleCommand {

    public CrackedCommand(FastLoginBukkit plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            onCrackedSelf(sender, command, args);
        } else {
            onCrackedOther(sender, command, args);
        }

        return true;
    }

    private void onCrackedSelf(CommandSender sender, Command cmd, String[] args) {
        if (isConsole(sender)) {
            return;
        }

        if (forwardCrackedCommand(sender, sender.getName())) {
            return;
        }

        if (plugin.getBungeeManager().isEnabled()) {
            sendBungeeActivateMessage(sender, sender.getName(), false);
            plugin.getCore().sendLocaleMessage("wait-on-proxy", sender);
        } else {
            //todo: load async if
            StoredProfile profile = plugin.getCore().getStorage().loadProfile(sender.getName());
            if (profile.isPremium()) {
                plugin.getCore().sendLocaleMessage("remove-premium", sender);

                profile.setPremium(false);
                profile.setId(null);
                plugin.getScheduler().runAsync(() -> {
                    plugin.getCore().getStorage().save(profile);
                    plugin.getServer().getPluginManager().callEvent(
                            new BukkitFastLoginPremiumToggleEvent(profile, PremiumToggleReason.COMMAND_OTHER));
                });
            } else {
                plugin.getCore().sendLocaleMessage("not-premium", sender);
            }
        }
    }

    private void onCrackedOther(CommandSender sender, Command command, String[] args) {
        if (!hasOtherPermission(sender, command)) {
            return;
        }

        if (forwardCrackedCommand(sender, args[0])) {
            return;
        }

        //todo: load async
        StoredProfile profile = plugin.getCore().getStorage().loadProfile(args[0]);
        if (profile == null) {
            sender.sendMessage("Error occurred");
            return;
        }

        //existing player is already cracked
        if (profile.isSaved() && !profile.isPremium()) {
            plugin.getCore().sendLocaleMessage("not-premium-other", sender);
        } else {
            plugin.getCore().sendLocaleMessage("remove-premium", sender);

            profile.setPremium(false);
            plugin.getScheduler().runAsync(() -> {
                plugin.getCore().getStorage().save(profile);
                plugin.getServer().getPluginManager().callEvent(
                        new BukkitFastLoginPremiumToggleEvent(profile, PremiumToggleReason.COMMAND_OTHER));
            });
        }
    }

    private boolean forwardCrackedCommand(CommandSender sender, String target) {
        return forwardBungeeCommand(sender, target, false);
    }
}
