package com.github.games647.fastlogin.bukkit.task;

import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.bukkit.hook.AuthMeHook;
import com.github.games647.fastlogin.bukkit.hook.CrazyLoginHook;
import com.github.games647.fastlogin.bukkit.hook.LogItHook;
import com.github.games647.fastlogin.bukkit.hook.LoginSecurityHook;
import com.github.games647.fastlogin.bukkit.hook.UltraAuthHook;
import com.github.games647.fastlogin.bukkit.hook.xAuthHook;
import com.github.games647.fastlogin.bukkit.hook.SodionAuthHook;
import com.github.games647.fastlogin.core.hooks.AuthPlugin;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public class DelayedAuthHook implements Runnable {

    private final FastLoginBukkit plugin;

    public DelayedAuthHook(FastLoginBukkit plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        boolean hookFound = isHookFound();
        if (plugin.getBungeeManager().isEnabled()) {
            plugin.getLog().info("BungeeCord setting detected. No auth plugin is required");
        } else if (!hookFound) {
            plugin.getLog().warn("No auth plugin were found by this plugin "
                    + "(other plugins could hook into this after the initialization of this plugin)"
                    + "and BungeeCord is deactivated. "
                    + "Either one or both of the checks have to pass in order to use this plugin");
        }

        if (hookFound) {
            plugin.markInitialized();
        }
    }

    private boolean isHookFound() {
        return plugin.getCore().getAuthPluginHook() != null || registerHooks();
    }

    private boolean registerHooks() {
        AuthPlugin<Player> authPluginHook = getAuthHook();
        if (authPluginHook == null) {
            //run this check for exceptions (errors) and not found plugins
            plugin.getLog().warn("No support offline Auth plugin found. ");
            return false;
        }

        if (authPluginHook instanceof Listener) {
            Bukkit.getPluginManager().registerEvents((Listener) authPluginHook, plugin);
        }

        if (plugin.getCore().getAuthPluginHook() == null) {
            plugin.getLog().info("Hooking into auth plugin: {}", authPluginHook.getClass().getSimpleName());
            plugin.getCore().setAuthPluginHook(authPluginHook);
        }

        return true;
    }

    private AuthPlugin<Player> getAuthHook() {
        try {
            @SuppressWarnings("unchecked")
            List<Class<? extends AuthPlugin<Player>>> hooks = Arrays.asList(AuthMeHook.class,
                    CrazyLoginHook.class, LogItHook.class, LoginSecurityHook.class,
                    SodionAuthHook.class, UltraAuthHook.class, xAuthHook.class);

            for (Class<? extends AuthPlugin<Player>> clazz : hooks) {
                String pluginName = clazz.getSimpleName().replace("Hook", "");
                //uses only member classes which uses AuthPlugin interface (skip interfaces)
                if (Bukkit.getPluginManager().isPluginEnabled(pluginName)) {
                    //check only for enabled plugins. A single plugin could be disabled by plugin managers
                    return newInstance(clazz);
                }
            }
        } catch (ReflectiveOperationException ex) {
            plugin.getLog().error("Couldn't load the auth hook class", ex);
        }

        return null;
    }

    private AuthPlugin<Player> newInstance(Class<? extends AuthPlugin<Player>> clazz)
            throws ReflectiveOperationException {
        try {
            Constructor<? extends AuthPlugin<Player>> cons = clazz.getDeclaredConstructor(FastLoginBukkit.class);
            return cons.newInstance(plugin);
        } catch (NoSuchMethodException noMethodEx) {
            return clazz.getDeclaredConstructor().newInstance();
        }
    }
}
