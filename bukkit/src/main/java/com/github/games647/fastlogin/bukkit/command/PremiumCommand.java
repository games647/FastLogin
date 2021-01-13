package com.github.games647.fastlogin.bukkit.command;

import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.bukkit.event.BukkitFastLoginPremiumToggleEvent;
import com.github.games647.fastlogin.core.StoredProfile;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.github.games647.fastlogin.core.shared.event.FastLoginPremiumToggleEvent;
import com.github.games647.fastlogin.core.shared.event.FastLoginPremiumToggleEvent.PremiumToggleReason;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.UUID;
import java.net.URL;
import java.net.URLConnection;

/**
 * Let users activate fast login by command. This only be accessible if
 * the user has access to it's account. So we can make sure that not another
 * person with a paid account and the same username can steal their account.
 */
public class PremiumCommand extends ToggleCommand {

    private static String mojangURL = "https://api.mojang.com/users/profiles/minecraft/{username}";
    
    public PremiumCommand(FastLoginBukkit plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            onPremiumSelf(sender, command, args);
        } else {
            onPremiumOther(sender, command, args);
        }

        return true;
    }

    private void onPremiumSelf(CommandSender sender, Command cmd, String[] args) {
        if (isConsole(sender)) {
            return;
        }

        if (forwardPremiumCommand(sender, sender.getName())) {
            return;
        }
        Player player = ((Player) sender);
        UUID id = player.getUniqueId();
        if (plugin.getConfig().getBoolean("premium-warning") && !plugin.getCore().getPendingConfirms().contains(id)) {
            sender.sendMessage(plugin.getCore().getMessage("premium-warning"));
            plugin.getCore().getPendingConfirms().add(id);
            return;
        }

        plugin.getCore().getPendingConfirms().remove(id);
        //todo: load async
        StoredProfile profile = plugin.getCore().getStorage().loadProfile(sender.getName());
        if (profile.isPremium()) {
            plugin.getCore().sendLocaleMessage("already-exists", sender);
        } else {
            profile.setPremium(true);
            plugin.getScheduler().runAsync(() -> {
                String uuidPremium = getUUIDPremiumByMojang(player.getName());
                if (uuidPremium != null) profile.setId(UUID.fromString(uuidPremium));
                plugin.getCore().getStorage().save(profile);
                plugin.getServer().getPluginManager().callEvent(
                        new BukkitFastLoginPremiumToggleEvent(profile, PremiumToggleReason.COMMAND_SELF));
            });

            plugin.getCore().sendLocaleMessage("add-premium", sender);
        }
    }

    private void onPremiumOther(CommandSender sender, Command command, String[] args) {
        if (!hasOtherPermission(sender, command)) {
            return;
        }

        if (forwardPremiumCommand(sender, args[0])) {
            return;
        }

        //todo: load async
        StoredProfile profile = plugin.getCore().getStorage().loadProfile(args[0]);
        if (profile == null) {
            plugin.getCore().sendLocaleMessage("player-unknown", sender);
            return;
        }

        if (profile.isPremium()) {
            plugin.getCore().sendLocaleMessage("already-exists-other", sender);
        } else {
            profile.setPremium(true);
            plugin.getScheduler().runAsync(() -> {
                String uuidPremium = getUUIDPremiumByMojang(args[0]);
                if (uuidPremium != null) profile.setId(UUID.fromString(uuidPremium));
                plugin.getCore().getStorage().save(profile);
                plugin.getServer().getPluginManager().callEvent(
                        new BukkitFastLoginPremiumToggleEvent(profile, PremiumToggleReason.COMMAND_OTHER));
            });

            plugin.getCore().sendLocaleMessage("add-premium-other", sender);
        }
    }

    private boolean forwardPremiumCommand(CommandSender sender, String target) {
        return forwardBungeeCommand(sender, target, true);
    }
    
    private String getUUIDPremiumByMojang(String username) {
         try {
            String mojangRequest = mojangURL.replace("{username}", "doctaenkoda");
            URL url = new URL(mojangRequest);
            URLConnection request = url.openConnection();
            request.connect();
            JsonParser jp = new JsonParser();
            JsonElement root = jp.parse(new InputStreamReader((InputStream) request.getContent()));
            JsonObject rootobj = root.getAsJsonObject();
            return rootobj.get("id").getAsString();
        }  catch (Exception exception) {
            return null;
        }
    }
    
}
