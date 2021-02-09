package com.github.games647.fastlogin.bukkit.hook;

import com.github.games647.fastlogin.bukkit.BukkitLoginSession;
import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.core.hooks.AuthPlugin;
import fr.xephi.authme.api.v3.AuthMeApi;
import fr.xephi.authme.events.RestoreSessionEvent;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.process.register.executors.ApiPasswordRegisterParams;
import fr.xephi.authme.process.register.executors.RegistrationMethod;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.lang.reflect.Field;

/**
 * GitHub: https://github.com/Xephi/AuthMeReloaded/
 * <p>
 * Project page:
 * <p>
 * Bukkit: https://dev.bukkit.org/bukkit-plugins/authme-reloaded/
 * <p>
 * Spigot: https://www.spigotmc.org/resources/authme-reloaded.6269/
 */
public class AuthMeHook implements AuthPlugin<Player>, Listener {

    private final FastLoginBukkit plugin;

    private final AuthMeApi authmeAPI;
    private Management authmeManagement;

    public AuthMeHook(FastLoginBukkit plugin) {
        this.plugin = plugin;
        this.authmeAPI = AuthMeApi.getInstance();

        if (plugin.getConfig().getBoolean("respectIpLimit", false)) {
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

        BukkitLoginSession session = plugin.getSession(player.getAddress());
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
