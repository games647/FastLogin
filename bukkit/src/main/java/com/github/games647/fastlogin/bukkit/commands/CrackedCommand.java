package com.github.games647.fastlogin.bukkit.commands;

import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.core.PlayerProfile;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CrackedCommand implements CommandExecutor {

    private final FastLoginBukkit plugin;

    public CrackedCommand(FastLoginBukkit plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                //console or command block
                sender.sendMessage(plugin.getCore().getMessage("no-console"));
                return true;
            }

            if (plugin.isBungeeCord()) {
                plugin.sendBungeeActivateMessage(sender, sender.getName(), false);
                String message = plugin.getCore().getMessage("wait-on-proxy");
                if (message != null) {
                    sender.sendMessage(message);
                }
            } else {
                //todo: load async if
                PlayerProfile profile = plugin.getCore().getStorage().loadProfile(sender.getName());
                if (profile.isPremium()) {
                    sender.sendMessage(plugin.getCore().getMessage("remove-premium"));

                    profile.setPremium(false);
                    profile.setUuid(null);
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        plugin.getCore().getStorage().save(profile);
                    });
                } else {
                    sender.sendMessage(plugin.getCore().getMessage("not-premium"));
                }
            }

            return true;
        } else {
            onCrackedOther(sender, command, args);
        }

        return true;
    }

    private void onCrackedOther(CommandSender sender, Command command, String[] args) {
        if (!sender.hasPermission(command.getPermission() + ".other")) {
            sender.sendMessage(plugin.getCore().getMessage("no-permission"));
            return;
        }
        
        if (plugin.isBungeeCord()) {
            plugin.sendBungeeActivateMessage(sender, args[0], false);
            String message = plugin.getCore().getMessage("wait-on-proxy");
            if (message != null) {
                sender.sendMessage(message);
            }
        } else {
            //todo: load async
            PlayerProfile profile = plugin.getCore().getStorage().loadProfile(args[0]);
            if (profile == null) {
                sender.sendMessage("Error occured");
                return;
            }

            //existing player is already cracked
            if (profile.getUserId() != -1 && !profile.isPremium()) {
                sender.sendMessage(plugin.getCore().getMessage("not-premium-other"));
            } else {
                sender.sendMessage(plugin.getCore().getMessage("remove-premium"));
                profile.setPremium(false);
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    plugin.getCore().getStorage().save(profile);
                });
            }
        }
    }
}
