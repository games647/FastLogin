package com.github.games647.fastlogin.bukkit;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.PlaceholderHook;

import org.bukkit.entity.Player;

public class PremiumPlaceholder extends PlaceholderHook {

    private final FastLoginBukkit plugin;

    public PremiumPlaceholder(FastLoginBukkit plugin) {
        this.plugin = plugin;
    }

    @Override
    public String onPlaceholderRequest(Player player, String variable) {
        if (player != null && "fastlogin_status".contains(variable)) {
            return plugin.getStatus(player.getUniqueId()).name();
        }

        return "";
    }

    public static void register(FastLoginBukkit plugin) {
        PlaceholderAPI.registerPlaceholderHook(plugin, new PremiumPlaceholder(plugin));
    }
}
