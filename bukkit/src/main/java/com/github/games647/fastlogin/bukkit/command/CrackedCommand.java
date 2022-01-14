/*
 * SPDX-License-Identifier: MIT
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2021 <Your name and contributors>
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
import com.github.games647.fastlogin.bukkit.event.BukkitFastLoginPremiumToggleEvent;
import com.github.games647.fastlogin.core.StoredProfile;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import static com.github.games647.fastlogin.core.shared.event.FastLoginPremiumToggleEvent.PremiumToggleReason;

public class CrackedCommand extends ToggleCommand {

    public CrackedCommand(FastLoginBukkit plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            onCrackedSelf(sender, command, args);
        } else {
            onCrackedOther(sender, command, args);
        }

        return true;
    }

    private void onCrackedSelf(CommandSender sender, Command cmd, String[] args) {
        if (isConsole(sender)) {
            return;
        }

        if (forwardCrackedCommand(sender, sender.getName())) {
            return;
        }

        if (plugin.getBungeeManager().isEnabled()) {
            sendBungeeActivateMessage(sender, sender.getName(), false);
            plugin.getCore().sendLocaleMessage("wait-on-proxy", sender);
        } else {
            //todo: load async if
            StoredProfile profile = plugin.getCore().getStorage().loadProfile(sender.getName());
            if (profile.isPremium()) {
                plugin.getCore().sendLocaleMessage("remove-premium", sender);

                profile.setPremium(false);
                profile.setId(null);
                plugin.getScheduler().runAsync(() -> {
                    plugin.getCore().getStorage().save(profile);
                    plugin.getServer().getPluginManager().callEvent(
                            new BukkitFastLoginPremiumToggleEvent(profile, PremiumToggleReason.COMMAND_OTHER));
                });
            } else {
                plugin.getCore().sendLocaleMessage("not-premium", sender);
            }
        }
    }

    private void onCrackedOther(CommandSender sender, Command command, String[] args) {
        if (!hasOtherPermission(sender, command)) {
            return;
        }

        if (forwardCrackedCommand(sender, args[0])) {
            return;
        }

        //todo: load async
        StoredProfile profile = plugin.getCore().getStorage().loadProfile(args[0]);
        if (profile == null) {
            sender.sendMessage("Error occurred");
            return;
        }

        //existing player is already cracked
        if (profile.isSaved() && !profile.isPremium()) {
            plugin.getCore().sendLocaleMessage("not-premium-other", sender);
        } else {
            plugin.getCore().sendLocaleMessage("remove-premium", sender);

            profile.setPremium(false);
            plugin.getScheduler().runAsync(() -> {
                plugin.getCore().getStorage().save(profile);
                plugin.getServer().getPluginManager().callEvent(
                        new BukkitFastLoginPremiumToggleEvent(profile, PremiumToggleReason.COMMAND_OTHER));
            });
        }
    }

    private boolean forwardCrackedCommand(CommandSender sender, String target) {
        return forwardBungeeCommand(sender, target, false);
    }
}
