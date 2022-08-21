/*
 * SPDX-License-Identifier: MIT
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2022 games647 and contributors
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
package com.github.games647.fastlogin.bukkit.hook;

import com.github.games647.fastlogin.bukkit.BukkitLoginSession;
import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.core.hooks.AuthPlugin;

import fr.xephi.authme.api.v3.AuthMeApi;
import fr.xephi.authme.events.RestoreSessionEvent;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.process.register.executors.ApiPasswordRegisterParams;
import fr.xephi.authme.process.register.executors.RegistrationMethod;

import java.lang.reflect.Field;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * GitHub: <a href="https://github.com/Xephi/AuthMeReloaded/">...</a>
 * <p>
 * Project page:
 * <p>
 * Bukkit: <a href="https://dev.bukkit.org/bukkit-plugins/authme-reloaded/">...</a>
 * <p>
 * Spigot: <a href="https://www.spigotmc.org/resources/authme-reloaded.6269/">...</a>
 */
public class AuthMeHook implements AuthPlugin<Player>, Listener {

    private final FastLoginBukkit plugin;

    private final AuthMeApi authmeAPI;
    private Management authmeManagement;

    public AuthMeHook(FastLoginBukkit plugin) {
        this.plugin = plugin;
        this.authmeAPI = AuthMeApi.getInstance();

        if (plugin.getCore().getConfig().getBoolean("respectIpLimit", false)) {
            try {
                Field managementField = this.authmeAPI.getClass().getDeclaredField("management");
                managementField.setAccessible(true);
                this.authmeManagement = (Management) managementField.get(this.authmeAPI);
            } catch (NoSuchFieldException | IllegalAccessException exception) {
                this.authmeManagement = null;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSessionRestore(RestoreSessionEvent restoreSessionEvent) {
        Player player = restoreSessionEvent.getPlayer();

        BukkitLoginSession session = plugin.getSession(player.spigot().getRawAddress());
        if (session != null && session.isVerified()) {
            restoreSessionEvent.setCancelled(true);
        }
    }

    @Override
    public boolean forceLogin(Player player) {
        if (authmeAPI.isAuthenticated(player)) {
            plugin.getLog().warn(ALREADY_AUTHENTICATED, player);
            return false;
        }

        //skips registration and login
        authmeAPI.forceLogin(player);
        return true;
    }

    @Override
    public boolean isRegistered(String playerName) {
        return authmeAPI.isRegistered(playerName);
    }

    @Override
    //this automatically login the player too
    public boolean forceRegister(Player player, String password) {
        //if we have the management - we can trigger register with IP limit checks
        if (authmeManagement != null) {
            authmeManagement.performRegister(RegistrationMethod.PASSWORD_REGISTRATION,
                    ApiPasswordRegisterParams.of(player, password, true));
        } else {
            authmeAPI.forceRegister(player, password);
        }

        return true;
    }
}
