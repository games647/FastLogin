package com.github.games647.fastlogin.bukkit.commands;

import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.core.PlayerProfile;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Let users activate fast login by command. This only be accessible if
 * the user has access to it's account. So we can make sure that not another
 * person with a paid account and the same username can steal his account.
 */
public class PremiumCommand implements CommandExecutor {

    private final FastLoginBukkit plugin;

    public PremiumCommand(FastLoginBukkit plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                //console or command block
                plugin.getCore().sendLocaleMessage("no-console", sender);
                return true;
            }

            if (plugin.isBungeeCord()) {
                plugin.sendBungeeActivateMessage(sender, sender.getName(), true);
                plugin.getCore().sendLocaleMessage("wait-on-proxy", sender);
            } else {
                UUID id = ((Player) sender).getUniqueId();
                if (plugin.getConfig().getBoolean("premium-warning")
                        && !plugin.getCore().getPendingConfirms().contains(id)) {
                    sender.sendMessage(plugin.getCore().getMessage("premium-warning"));
                    plugin.getCore().getPendingConfirms().add(id);
                    return true;
                }

                plugin.getCore().getPendingConfirms().remove(id);
                //todo: load async
                PlayerProfile profile = plugin.getCore().getStorage().loadProfile(sender.getName());
                if (profile.isPremium()) {
                    plugin.getCore().sendLocaleMessage("already-exists", sender);
                } else {
                    //todo: resolve uuid
                    profile.setPremium(true);
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        plugin.getCore().getStorage().save(profile);
                    });

                    plugin.getCore().sendLocaleMessage("add-premium", sender);
                }
            }

            return true;
        } else {
            onPremiumOther(sender, command, args);
        }

        return true;
    }

    private void onPremiumOther(CommandSender sender, Command command, String[] args) {
        if (!sender.hasPermission(command.getPermission() + ".other")) {
            plugin.getCore().sendLocaleMessage("no-permission", sender);
            return ;
        }

        if (plugin.isBungeeCord()) {
            plugin.sendBungeeActivateMessage(sender, args[0], true);
            plugin.getCore().sendLocaleMessage("wait-on-proxy", sender);
        } else {
            //todo: load async
            PlayerProfile profile = plugin.getCore().getStorage().loadProfile(args[0]);
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
                });

                plugin.getCore().sendLocaleMessage("add-premium-other", sender);
            }
        }
    }
}
