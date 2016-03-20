package com.github.games647.fastlogin.bukkit.commands;

import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

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
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                //console or command block
                sender.sendMessage(ChatColor.DARK_RED + "Only players can remove themselves from the premium list");
                return true;
            }

            String playerName = sender.getName();
            boolean existed = plugin.getEnabledPremium().remove(playerName);
            if (existed) {
                sender.sendMessage(ChatColor.DARK_GREEN + "Removed from the list of premium players");
                notifiyBungeeCord((Player) sender);
            } else {
                sender.sendMessage(ChatColor.DARK_RED + "You are not in the premium list");
            }

            return true;
        } else {
            String playerName = args[0];
            boolean existed = plugin.getEnabledPremium().remove(playerName);
            if (existed) {
                sender.sendMessage(ChatColor.DARK_GREEN + "Removed from the list of premium players");
//                notifiyBungeeCord((Player) sender);
            } else {
                sender.sendMessage(ChatColor.DARK_RED + "User is not in the premium list");
            }
        }

        return true;
    }

    private void notifiyBungeeCord(Player target) {
        ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
        dataOutput.writeUTF("OFF");

        target.sendPluginMessage(plugin, plugin.getName(), dataOutput.toByteArray());
    }
}
