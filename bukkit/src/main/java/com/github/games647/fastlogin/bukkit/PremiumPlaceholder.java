package com.github.games647.fastlogin.bukkit;

import java.util.stream.Collectors;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import org.bukkit.entity.Player;

public class PremiumPlaceholder extends PlaceholderExpansion {

    private static final String PLACEHOLDER_VARIABLE = "fastlogin_status";

    private final FastLoginBukkit plugin;

    public PremiumPlaceholder(FastLoginBukkit plugin) {
        this.plugin = plugin;
    }

    public static void register(FastLoginBukkit plugin) {
        PremiumPlaceholder placeholderHook = new PremiumPlaceholder(plugin);
        PlaceholderAPI.registerPlaceholderHook(PLACEHOLDER_VARIABLE, placeholderHook);
    }

    @Override
    public String onPlaceholderRequest(Player player, String variable) {
        if (player != null && PLACEHOLDER_VARIABLE.equals(variable)) {
            return plugin.getStatus(player.getUniqueId()).name();
        }

        return "";
    }

    @Override
    public String getIdentifier() {
        return PLACEHOLDER_VARIABLE;
    }

    @Override
    public String getPlugin() {
        return plugin.getName();
    }

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().stream().collect(Collectors.joining(", "));
    }

    @Override
    public String getVersion() {
        return plugin.getName();
    }
}
