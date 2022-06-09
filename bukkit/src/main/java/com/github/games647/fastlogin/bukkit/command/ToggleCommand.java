/*
 * SPDX-License-Identifier: MIT
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2022 games647 and contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.github.games647.fastlogin.bukkit.command;

import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.core.message.ChangePremiumMessage;
import com.github.games647.fastlogin.core.message.ChannelMessage;

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
        if (sender.hasPermission(cmd.getPermission() + ".other")) {
            return true;
        }

        plugin.getCore().sendLocaleMessage("no-permission", sender);
        return false;
    }

    protected boolean forwardBungeeCommand(CommandSender sender, String target, boolean activate) {
        if (plugin.getBungeeManager().isEnabled()) {
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
            plugin.getBungeeManager().sendPluginMessage((PluginMessageRecipient) invoker, message);
        } else {
            Optional<? extends Player> optPlayer = Bukkit.getServer().getOnlinePlayers().stream().findFirst();
            if (optPlayer.isEmpty()) {
                plugin.getLog().info("No player online to send a plugin message to the proxy");
                return;
            }

            Player sender = optPlayer.get();
            ChannelMessage message = new ChangePremiumMessage(target, activate, false);
            plugin.getBungeeManager().sendPluginMessage(sender, message);
        }
    }
}
