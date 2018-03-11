package com.github.games647.fastlogin.bukkit.commands;

import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.core.messages.ChangePremiumMessage;
import com.github.games647.fastlogin.core.messages.ChannelMessage;

import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageRecipient;

public abstract class ToggleCommand implements CommandExecutor {

    protected final FastLoginBukkit plugin;

    public ToggleCommand(FastLoginBukkit plugin) {
        this.plugin = plugin;
    }

    protected boolean hasOtherPermission(CommandSender sender, Command cmd) {
        if (!sender.hasPermission(cmd.getPermission() + ".other")) {
            plugin.getCore().sendLocaleMessage("no-permission", sender);
            return false;
        }

        return true;
    }

    protected boolean forwardBungeeCommand(CommandSender sender, String target, boolean activate) {
        if (plugin.isBungeeEnabled()) {
            sendBungeeActivateMessage(sender, target, activate);
            plugin.getCore().sendLocaleMessage("wait-on-proxy", sender);
            return true;
        }

        return false;
    }

    protected boolean isConsole(CommandSender sender) {
        if (sender instanceof Player) {
            return false;
        }

        //console or command block
        sender.sendMessage(plugin.getCore().getMessage("no-console"));
        return true;
    }

    protected void sendBungeeActivateMessage(CommandSender invoker, String target, boolean activate) {
        if (invoker instanceof PluginMessageRecipient) {
            ChannelMessage message = new ChangePremiumMessage(target, activate, true);
            plugin.sendPluginMessage((PluginMessageRecipient) invoker, message);
        } else {
            Optional<? extends Player> optPlayer = Bukkit.getServer().getOnlinePlayers().stream().findFirst();
            if (!optPlayer.isPresent()) {
                plugin.getLog().info("No player online to send a plugin message to the proxy");
                return;
            }

            Player sender = optPlayer.get();
            ChannelMessage message = new ChangePremiumMessage(target, activate, false);
            plugin.sendPluginMessage(sender, message);
        }
    }
}
