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

    private final FastLoginBukkit plugin;

    public CrackedCommand(FastLoginBukkit plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (plugin.getStorage() == null) {
            sender.sendMessage(ChatColor.DARK_RED + "This command is disabled on the backend server");
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                //console or command block
                sender.sendMessage(ChatColor.DARK_RED + "Only players can remove themselves from the premium list");
                return true;
            }

            Player player = (Player) sender;
//            UUID uuid = player.getUniqueId();

            //todo: load async if it's not in the cache anymore
            final PlayerProfile profile = plugin.getStorage().getProfile(player.getName(), false);
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

                notifiyBungeeCord((Player) sender);
            } else {
                sender.sendMessage(ChatColor.DARK_RED + "You are not in the premium list");
            }

            return true;
        } else {
            sender.sendMessage(ChatColor.DARK_RED + "NOT IMPLEMENTED YET");
            //todo:
//            String playerName = args[0];
//            boolean existed = plugin.getEnabledPremium().remove(playerName);
//            if (existed) {
//                sender.sendMessage(ChatColor.DARK_GREEN + "Removed from the list of premium players");
//                notifiyBungeeCord((Player) sender);
//            } else {
//                sender.sendMessage(ChatColor.DARK_RED + "User is not in the premium list");
//            }
        }

        return true;
    }

    private void notifiyBungeeCord(Player target) {
        ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
        dataOutput.writeUTF("OFF");

        target.sendPluginMessage(plugin, plugin.getName(), dataOutput.toByteArray());
    }
}
