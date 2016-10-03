package com.github.games647.fastlogin.core.shared;

import com.github.games647.fastlogin.core.AuthStorage;
import com.github.games647.fastlogin.core.PlayerProfile;
import com.github.games647.fastlogin.core.hooks.AuthPlugin;

import java.util.logging.Level;

public abstract class ForceLoginMangement<P extends C, C, L extends LoginSession, T extends PlatformPlugin<C>>
        implements Runnable {

    protected final FastLoginCore<P, C, T> core;
    protected final P player;

    protected L session;

    public ForceLoginMangement(FastLoginCore<P, C, T> core, P player) {
        this.core = core;
        this.player = player;
    }

    @Override
    public void run() {
        if (!isOnline(player) || session == null) {
            return;
        }

        AuthStorage storage = core.getStorage();

        PlayerProfile playerProfile = session.getProfile();
        try {
            if (isOnlineMode()) {
                //premium player
                AuthPlugin<P> authPlugin = core.getAuthPluginHook();
                if (authPlugin == null) {
                    //maybe only bungeecord plugin
                    onForceActionSuccess(session);
                } else {
                    boolean success = true;
                    String playerName = getName(player);
                    if (core.getConfig().get("autoLogin", true)) {
                        if (session.needsRegistration()
                                || (core.getConfig().get("auto-register-unknown", false)
                                && !authPlugin.isRegistered(playerName))) {
                            success = forceRegister(player);
                        } else {
                            success = forceLogin(player);
                        }
                    }

                    if (success) {
                        //update only on success to prevent corrupt data
                        if (playerProfile != null) {
                            playerProfile.setUuid(session.getUuid());
                            //save cracked players too
                            playerProfile.setPremium(true);
                            storage.save(playerProfile);
                        }

                        onForceActionSuccess(session);
                    }
                }
            } else if (playerProfile != null) {
                //cracked player
                playerProfile.setUuid(null);
                playerProfile.setPremium(false);
                storage.save(playerProfile);
            }
        } catch (Exception ex) {
            core.getPlugin().getLogger().log(Level.INFO, "ERROR ON FORCE LOGIN", ex);
        }
    }

    public boolean forceRegister(P player) {
        core.getPlugin().getLogger().log(Level.FINE, "Register player {0}", getName(player));

        String generatedPassword = core.getPasswordGenerator().getRandomPassword(player);
        boolean success = core.getAuthPluginHook().forceRegister(player, generatedPassword);

        String message = core.getMessage("auto-register");
        if (success && message != null) {
            message = message.replace("%password", generatedPassword);
            core.getPlugin().sendMessage(player, message);
        }

        return success;
    }

    public boolean forceLogin(P player) {
        core.getPlugin().getLogger().log(Level.FINE, "Logging player {0} in", getName(player));
        boolean success = core.getAuthPluginHook().forceLogin(player);

        if (success) {
            core.sendLocaleMessage("auto-login", player);
        }

        return success;
    }

    public abstract void onForceActionSuccess(LoginSession session);

    public abstract String getName(P player);

    public abstract boolean isOnline(P player);

    public abstract boolean isOnlineMode();
}
