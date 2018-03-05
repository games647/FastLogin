package com.github.games647.fastlogin.core.shared;

import com.github.games647.fastlogin.core.AuthStorage;
import com.github.games647.fastlogin.core.PlayerProfile;
import com.github.games647.fastlogin.core.hooks.AuthPlugin;

public abstract class ForceLoginManagement<P extends C, C, L extends LoginSession, T extends PlatformPlugin<C>>
        implements Runnable {

    protected final FastLoginCore<P, C, T> core;
    protected final P player;
    protected final L session;

    public ForceLoginManagement(FastLoginCore<P, C, T> core, P player, L session) {
        this.core = core;
        this.player = player;
        this.session = session;
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
                            playerProfile.setId(session.getUuid());
                            playerProfile.setPremium(true);
                            storage.save(playerProfile);
                        }

                        onForceActionSuccess(session);
                    }
                }
            } else if (playerProfile != null) {
                //cracked player
                playerProfile.setId(null);
                playerProfile.setPremium(false);
                storage.save(playerProfile);
            }
        } catch (Exception ex) {
            core.getPlugin().getLog().warn("ERROR ON FORCE LOGIN", ex);
        }
    }

    public boolean forceRegister(P player) {
        core.getPlugin().getLog().info("Register player {}", getName(player));

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
        core.getPlugin().getLog().info("Logging player {} in", getName(player));
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
