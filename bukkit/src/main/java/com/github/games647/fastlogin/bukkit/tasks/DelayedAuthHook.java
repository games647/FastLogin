package com.github.games647.fastlogin.bukkit.tasks;

import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.bukkit.hooks.AuthMeHook;
import com.github.games647.fastlogin.bukkit.hooks.CrazyLoginHook;
import com.github.games647.fastlogin.bukkit.hooks.LogItHook;
import com.github.games647.fastlogin.bukkit.hooks.LoginSecurityHook;
import com.github.games647.fastlogin.bukkit.hooks.UltraAuthHook;
import com.github.games647.fastlogin.bukkit.hooks.xAuthHook;
import com.github.games647.fastlogin.core.hooks.AuthPlugin;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class DelayedAuthHook implements Runnable {

    private final FastLoginBukkit plugin;

    public DelayedAuthHook(FastLoginBukkit plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        boolean hookFound = plugin.getCore().getAuthPluginHook() != null || registerHooks();
        if (plugin.isBungeeCord()) {
            plugin.getLogger().info("BungeeCord setting detected. No auth plugin is required");
        } else if (!hookFound) {
            plugin.getLogger().warning("No auth plugin were found by this plugin "
                    + "(other plugins could hook into this after the intialization of this plugin)"
                    + "and bungeecord is deactivated. "
                    + "Either one or both of the checks have to pass in order to use this plugin");
        }
    }

    private boolean registerHooks() {
        AuthPlugin<Player> authPluginHook = null;
        try {
            @SuppressWarnings("unchecked")
            List<Class<? extends AuthPlugin<Player>>> supportedHooks = Lists.newArrayList(AuthMeHook.class
                    , CrazyLoginHook.class, LogItHook.class, LoginSecurityHook.class, UltraAuthHook.class
                    , xAuthHook.class);

            for (Class<? extends AuthPlugin<Player>> clazz : supportedHooks) {
                String pluginName = clazz.getSimpleName().replace("Hook", "");
                //uses only member classes which uses AuthPlugin interface (skip interfaces)
                if (Bukkit.getServer().getPluginManager().getPlugin(pluginName) != null) {
                    //check only for enabled plugins. A single plugin could be disabled by plugin managers
                    authPluginHook = clazz.newInstance();
                    plugin.getLogger().log(Level.INFO, "Hooking into auth plugin: {0}", pluginName);
                    break;
                }
            }
        } catch (InstantiationException | IllegalAccessException ex) {
            plugin.getLogger().log(Level.SEVERE, "Couldn't load the integration class", ex);
        }

        if (authPluginHook == null) {
            //run this check for exceptions (errors) and not found plugins
            plugin.getLogger().warning("No support offline Auth plugin found. ");
            return false;
        }

        if (plugin.getCore().getAuthPluginHook() == null) {
            plugin.getCore().setAuthPluginHook(authPluginHook);
            plugin.setServerStarted();
        }

        return true;
    }
}
