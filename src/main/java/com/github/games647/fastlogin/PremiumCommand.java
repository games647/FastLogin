package com.github.games647.fastlogin;

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

    private final FastLogin plugin;

    public PremiumCommand(FastLogin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                //console or command block
                sender.sendMessage(ChatColor.DARK_RED + "Only players can add themselves as premium");
                return true;
            }

            String playerName = sender.getName();
            plugin.getEnabledPremium().add(playerName);
            sender.sendMessage(ChatColor.DARK_GREEN + "Added to the list of premium players");
            return true;
        }

        return true;
    }
}
