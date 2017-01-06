package com.github.games647.fastlogin.bukkit;

import me.clip.placeholderapi.PlaceholderHook;

import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;

import java.util.List;

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

            if (metadata.size() > 0) {
                return "premium";
            } else {
                return "cracked";
            }
        }

        return null;
    }
}
