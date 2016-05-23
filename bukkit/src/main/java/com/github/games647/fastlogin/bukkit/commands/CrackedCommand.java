package com.github.games647.fastlogin.bukkit.commands;

import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.bukkit.PlayerProfile;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CrackedCommand implements CommandExecutor {

    protected final FastLoginBukkit plugin;

    public CrackedCommand(FastLoginBukkit plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                //console or command block
                sender.sendMessage(ChatColor.DARK_RED + "Only players can remove themselves from the premium list");
                return true;
            }

            if (plugin.isBungeeCord()) {
                notifiyBungeeCord(sender, sender.getName());
                sender.sendMessage(ChatColor.YELLOW + "Sending request...");
            } else {
                //todo: load async if it's not in the cache anymore
                final PlayerProfile profile = plugin.getStorage().getProfile(sender.getName(), true);
                if (profile.isPremium()) {
                    sender.sendMessage(ChatColor.DARK_GREEN + "Removed from the list of premium players");
                    profile.setPremium(false);
                    profile.setUuid(null);
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                        @Override
                        public void run() {
                            plugin.getStorage().save(profile);
                        }
                    });
                } else {
                    sender.sendMessage(ChatColor.DARK_RED + "You are not in the premium list");
                }
            }

            return true;
        } else {
            if (!sender.hasPermission(command.getPermission() + ".other")) {
                sender.sendMessage(ChatColor.DARK_RED + "Not enough permissions");
                return true;
            }

            if (plugin.isBungeeCord()) {
                notifiyBungeeCord(sender, args[0]);
                sender.sendMessage(ChatColor.YELLOW + "Sending request for player " + args[0] + "...");
            } else {
                //todo: load async if it's not in the cache anymore
                final PlayerProfile profile = plugin.getStorage().getProfile(args[0], true);
                if (profile == null) {
                    sender.sendMessage(ChatColor.DARK_RED + "Player not in the database");
                    return true;
                }

                if (profile.isPremium()) {
                    sender.sendMessage(ChatColor.DARK_GREEN + "Removed from the list of premium players");
                    profile.setPremium(false);
                    profile.setUuid(null);
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                        @Override
                        public void run() {
                            plugin.getStorage().save(profile);
                        }
                    });
                } else {
                    sender.sendMessage(ChatColor.DARK_RED + "Player is not in the premium list");
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
            dataOutput.writeUTF("OFF");
            dataOutput.writeUTF(target);

            plugin.getLogger().info("No player online to send a plugin message to the proxy");
        }
    }
}
