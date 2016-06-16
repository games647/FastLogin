package com.github.games647.fastlogin.bukkit.commands;

import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.core.PlayerProfile;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import org.bukkit.Bukkit;
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
                sender.sendMessage(plugin.getCore().getMessage("no-console"));
                return true;
            }

            if (plugin.isBungeeCord()) {
                notifiyBungeeCord(sender, sender.getName());
                String message = plugin.getCore().getMessage("wait-on-proxy");
                if (message != null) {
                    sender.sendMessage(message);
                }
            } else {
                //todo: load async if
                final PlayerProfile profile = plugin.getCore().getStorage().loadProfile(sender.getName());
                if (profile.isPremium()) {
                    sender.sendMessage(plugin.getCore().getMessage("remove-premium"));
                    profile.setPremium(false);
                    profile.setUuid(null);
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                        @Override
                        public void run() {
                            plugin.getCore().getStorage().save(profile);
                        }
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
            notifiyBungeeCord(sender, args[0]);
            String message = plugin.getCore().getMessage("wait-on-proxy");
            if (message != null) {
                sender.sendMessage(message);
            }
        } else {
            //todo: load async
            final PlayerProfile profile = plugin.getCore().getStorage().loadProfile(args[0]);
            if (profile == null) {
                sender.sendMessage(plugin.getCore().getMessage("player-unknown"));
                return;
            }

            if (profile.isPremium()) {
                sender.sendMessage(plugin.getCore().getMessage("remove-premium"));
                profile.setPremium(false);
                Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                    @Override
                    public void run() {
                        plugin.getCore().getStorage().save(profile);
                    }
                });
            } else {
                sender.sendMessage(plugin.getCore().getMessage("not-premium-other"));
            }
        }
    }

    private void notifiyBungeeCord(CommandSender sender, String target) {
        if (sender instanceof Player) {
            notifiyBungeeCord((Player) sender, target);
        } else {
            plugin.getLogger().info("No player online to send a plugin message to the proxy");
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

            sender.sendPluginMessage(plugin, plugin.getName(), dataOutput.toByteArray());
        }
    }
}
