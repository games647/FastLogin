/*
 * SPDX-License-Identifier: MIT
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2024 games647 and contributors
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
package com.github.games647.fastlogin.bukkit.auth.proxy;

import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.bukkit.auth.AuthenticationBackend;
import com.github.games647.fastlogin.core.message.LoginActionMessage;
import com.github.games647.fastlogin.core.message.NamespaceKey;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.PluginManager;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import static com.github.games647.fastlogin.core.message.ChangePremiumMessage.CHANGE_CHANNEL;
import static com.github.games647.fastlogin.core.message.SuccessMessage.SUCCESS_CHANNEL;

public class ProxyAuthentication implements AuthenticationBackend {

    private final FastLoginBukkit plugin;
    private ProxyVerifier verifier;

    public ProxyAuthentication(FastLoginBukkit plugin) {
        this.plugin = plugin;
    }


    @Override
    public boolean isAvailable() {
        return detectProxy();
    }

    @Override
    public void init(PluginManager pluginManager) {
        verifier = new ProxyVerifier(plugin);
        verifier.loadSecrets();

        registerPluginChannels();

        pluginManager.registerEvents(new ProxyConnectionListener(plugin, verifier), plugin);

        plugin.getLog().info("Found enabled proxy configuration");
        plugin.getLog().info("Remember to follow the proxy guide to complete your setup");
    }

    private void registerPluginChannels() {
        Server server = Bukkit.getServer();

        // check for incoming messages from the bungeecord version of this plugin
        String groupId = plugin.getName();
        String forceChannel = NamespaceKey.getCombined(groupId, LoginActionMessage.FORCE_CHANNEL);
        server.getMessenger().registerIncomingPluginChannel(plugin, forceChannel, new ProxyListener(plugin, verifier));

        // outgoing
        String successChannel = new NamespaceKey(groupId, SUCCESS_CHANNEL).getCombinedName();
        String changeChannel = new NamespaceKey(groupId, CHANGE_CHANNEL).getCombinedName();
        server.getMessenger().registerOutgoingPluginChannel(plugin, successChannel);
        server.getMessenger().registerOutgoingPluginChannel(plugin, changeChannel);
    }

    @Override
    public void stop() {
        if (verifier != null) {
            verifier.cleanup();
        }
    }

    private boolean detectProxy() {
        try {
            if (isProxySupported("org.spigotmc.SpigotConfig", "bungee")) {
                return true;
            }
        } catch (ClassNotFoundException classNotFoundException) {
            // leave stacktrace for class not found out
            plugin.getLog().warn("Cannot check for BungeeCord support: {}", classNotFoundException.getMessage());
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            plugin.getLog().warn("Cannot check for BungeeCord support", ex);
        }

        try {
            return isVelocityEnabled();
        } catch (ClassNotFoundException classNotFoundException) {
            plugin.getLog().warn("Cannot check for Velocity support in Paper: {}", classNotFoundException.getMessage());
        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
            plugin.getLog().warn("Cannot check for Velocity support in Paper", ex);
        }

        return false;
    }

    private boolean isProxySupported(String className, String fieldName)
            throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        return Class.forName(className).getDeclaredField(fieldName).getBoolean(null);
    }

    private boolean isVelocityEnabled()
            throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException, ClassNotFoundException,
            NoSuchMethodException, InvocationTargetException {
        try {
            Class<?> globalConfig = Class.forName("io.papermc.paper.configuration.GlobalConfiguration");
            Object global = globalConfig.getDeclaredMethod("get").invoke(null);
            Object proxiesConfiguration = global.getClass().getDeclaredField("proxies").get(global);

            Field velocitySectionField = proxiesConfiguration.getClass().getDeclaredField("velocity");
            Object velocityConfig = velocitySectionField.get(proxiesConfiguration);

            return velocityConfig.getClass().getDeclaredField("enabled").getBoolean(velocityConfig);
        } catch (ClassNotFoundException classNotFoundException) {
            // try again using the older Paper configuration, because the old class file still exists in newer versions
            if (isProxySupported("com.destroystokyo.paper.PaperConfig", "velocitySupport")) {
                return true;
            }
        }

        return false;
    }
}
