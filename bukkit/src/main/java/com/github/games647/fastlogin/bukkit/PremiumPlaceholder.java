package com.github.games647.fastlogin.bukkit;

import java.util.List;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.PlaceholderHook;

import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;

public class PremiumPlaceholder extends PlaceholderHook {

    private final FastLoginBukkit plugin;

    public PremiumPlaceholder(FastLoginBukkit plugin) {
        this.plugin = plugin;
    }

    @Override
    public String onPlaceholderRequest(Player player, String variable) {
        if (player != null && "fastlogin_status".contains(variable)) {
            List<MetadataValue> metadata = player.getMetadata(plugin.getName());
            if (metadata == null) {
                return "unknown";
            }

            if (!metadata.isEmpty()) {
                return "premium";
            } else {
                return "cracked";
            }
        }

        return null;
    }

    public static void register(FastLoginBukkit plugin) {
        PlaceholderAPI.registerPlaceholderHook(plugin, new PremiumPlaceholder(plugin));
    }
}
