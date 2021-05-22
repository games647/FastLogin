package com.github.games647.fastlogin.bukkit.hook.floodgate;

import java.io.IOException;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import com.github.games647.craftapi.model.Profile;
import com.github.games647.craftapi.resolver.RateLimitException;
import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.core.shared.LoginSource;

public class FloodgateHook {

    private final FastLoginBukkit plugin;

    public FloodgateHook(FastLoginBukkit plugin) {
        this.plugin = plugin;
    }

    /**
     * Check if the player's name conflicts an existing Java player's name, and
     * kick them if it does
     * 
     * @param core     the FastLoginCore
     * @param username the name of the player
     * @param source   an instance of LoginSource
     * @param plugin   the FastLoginBukkit plugin
     */
    public void checkNameConflict(String username, LoginSource source, FloodgatePlayer floodgatePlayer) {
        String allowConflict = plugin.getCore().getConfig().get("allowFloodgateNameConflict").toString().toLowerCase();
        if (allowConflict.equals("false")) {

            // check for conflicting Premium Java name
            Optional<Profile> premiumUUID = Optional.empty();
            try {
                premiumUUID = plugin.getCore().getResolver().findProfile(username);
            } catch (IOException | RateLimitException e) {
                e.printStackTrace();
                plugin.getLog().error(
                        "Could not check wether Floodgate Player {}'s name conflicts a premium Java player's name.",
                        username);
                try {
                    source.kick("Could not check if your name conflicts an existing Java Premium Player's name");
                } catch (Exception e1) {
                    plugin.getLog().error("Could not kick Player {}", username);
                }
            }

            if (premiumUUID.isPresent()) {
                plugin.getLog().info("Bedrock Player {}'s name conflicts an existing Java Premium Player's name",
                        username);
                try {
                    source.kick("Your name conflicts an existing Java Premium Player's name");
                } catch (Exception e) {
                    e.printStackTrace();
                    plugin.getLog().error("Could not kick Player {}", username);
                }
            }
        } else {
            plugin.getLog().info("Skipping name conflict checking for player {}", username);
        }
    }

    /**
     * The FloodgateApi does not support querying players by name, so this function
     * iterates over every online FloodgatePlayer and checks if the requested
     * username can be found
     * 
     * @param username the name of the player
     * @return FloodgatePlayer if found, null otherwise
     */
    public FloodgatePlayer getFloodgatePlayer(String username) {
        if (Bukkit.getServer().getPluginManager().isPluginEnabled("floodgate")) {
            for (FloodgatePlayer floodgatePlayer : FloodgateApi.getInstance().getPlayers()) {
                if (floodgatePlayer.getUsername().equals(username)) {
                    return floodgatePlayer;
                }
            }
        }
        return null;
    }

}
