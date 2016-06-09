package com.github.games647.fastlogin.bukkit.commands;

import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.core.PlayerProfile;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

    protected final FastLoginBukkit plugin;

    public PremiumCommand(FastLoginBukkit plugin) {
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
                notifiyBungeeCord(sender, sender.getName());
                sender.sendMessage(plugin.getCore().getMessage("wait-on-proxy"));
            } else {
//            //todo: load async if it's not in the cache anymore
                final PlayerProfile profile = plugin.getCore().getStorage().loadProfile(sender.getName());
                if (profile.isPremium()) {
                    sender.sendMessage(ChatColor.DARK_RED + "You are already on the premium list");
                } else {
                    //todo: resolve uuid
                    profile.setPremium(true);
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                        @Override
                        public void run() {
                            plugin.getCore().getStorage().save(profile);
                        }
                    });

                    sender.sendMessage(ChatColor.DARK_GREEN + "Added to the list of premium players");
                }
            }

            return true;
        } else {
            if (!sender.hasPermission(command.getPermission() + ".other")) {
                sender.sendMessage(plugin.getCore().getMessage("no-permission"));
                return true;
            }

            if (plugin.isBungeeCord()) {
                notifiyBungeeCord(sender, args[0]);
                sender.sendMessage(plugin.getCore().getMessage("wait-on-proxy"));
            } else {
                //todo: load async if it's not in the cache anymore
                final PlayerProfile profile = plugin.getCore().getStorage().loadProfile(args[0]);
                if (profile == null) {
                    sender.sendMessage(plugin.getCore().getMessage("player-unknown"));
                    return true;
                }

                if (profile.isPremium()) {
                    sender.sendMessage(ChatColor.DARK_RED + "Player is already on the premium list");
                } else {
                    //todo: resolve uuid
                    profile.setPremium(true);
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                        @Override
                        public void run() {
                            plugin.getCore().getStorage().save(profile);
                        }
                    });

                    sender.sendMessage(ChatColor.DARK_GREEN + "Added to the list of premium players");
                }
            }
        }

        return true;
    }

    private void notifiyBungeeCord(CommandSender sender, String target) {
        if (sender instanceof Player) {
            notifiyBungeeCord((Player) sender, target);
        } else {
            //todo: add console support
//            Player firstPlayer = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
//            notifiyBungeeCord(firstPlayer, target);
        }
    }

    private void notifiyBungeeCord(Player sender, String target) {
        if (plugin.isBungeeCord()) {
            ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
            dataOutput.writeUTF("ON");
            dataOutput.writeUTF(target);

            sender.sendPluginMessage(plugin, plugin.getName(), dataOutput.toByteArray());
        }
    }
}
