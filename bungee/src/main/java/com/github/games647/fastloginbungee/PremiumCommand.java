package com.github.games647.fastloginbungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

/**
 * Let players activate the fastlogin method on a BungeeCord instance.
 */
public class PremiumCommand extends Command {

    private final FastLogin plugin;

    public PremiumCommand(FastLogin plugin) {
        super(plugin.getDescription().getName()
                , plugin.getDescription().getName() + ".command." + "premium"
                , "prem" , "premium", "loginfast");

        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new ComponentBuilder("Only player can invoke this command")
                    .color(ChatColor.DARK_RED)
                    .create());
            return;
        }

        plugin.getEnabledPremium().add(sender.getName());
        sender.sendMessage(new ComponentBuilder("Added to the list of premium players")
                .color(ChatColor.DARK_GREEN)
                .create());
    }
}
