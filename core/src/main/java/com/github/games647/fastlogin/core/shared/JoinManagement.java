package com.github.games647.fastlogin.core.shared;

import com.github.games647.fastlogin.core.PlayerProfile;
import com.github.games647.fastlogin.core.SharedConfig;
import com.github.games647.fastlogin.core.hooks.AuthPlugin;

import java.util.UUID;
import java.util.logging.Level;

public abstract class JoinManagement<P extends C, C, S extends LoginSource> {

    protected final FastLoginCore<P, C, ?> core;
    protected final AuthPlugin<P> authHook;

    public JoinManagement(FastLoginCore<P, C, ?> core, AuthPlugin<P> authHook) {
        this.core = core;
        this.authHook = authHook;
    }

    public void onLogin(String username, S source) {
        PlayerProfile profile = core.getStorage().loadProfile(username);
        if (profile == null) {
            return;
        }

        SharedConfig config = core.getConfig();

        String ip = source.getAddress().getAddress().getHostAddress();
        profile.setLastIp(ip);
        try {
            if (profile.getUserId() == -1) {
                if (core.getPendingLogin().remove(ip + username) != null && config.get("secondAttemptCracked", false)) {
                    core.getPlugin().getLogger().log(Level.INFO, "Second attempt login -> cracked {0}", username);

                    //first login request failed so make a cracked session
                    startCrackedSession(source, profile, username);
                    return;
                }

                UUID premiumUUID = null;
                if (config.get("nameChangeCheck", false) || config.get("autoRegister", false)) {
                    premiumUUID = core.getApiConnector().getPremiumUUID(username);
                }

                if (premiumUUID == null
                        || (!checkNameChange(source, username, premiumUUID)
                        && !checkPremiumName(source, username, profile))) {
                    //nothing detected the player as premium -> start a cracked session
                    if (core.getConfig().get("switchMode", false)) {
                        source.kick(core.getMessage("switch-kick-message"));
                        return;
                    }

                    startCrackedSession(source, profile, username);
                }
            } else if (profile.isPremium()) {
                requestPremiumLogin(source, profile, username, true);
            } else {
                startCrackedSession(source, profile, username);
            }
        } catch (Exception ex) {
            core.getPlugin().getLogger().log(Level.SEVERE, "Failed to check premium state", ex);
        }
    }

    private boolean checkPremiumName(S source, String username, PlayerProfile profile) throws Exception {
        core.getPlugin().getLogger().log(Level.FINER, "Player {0} uses a premium username", username);
        if (core.getConfig().get("autoRegister", false) && (authHook == null || !authHook.isRegistered(username))) {
            requestPremiumLogin(source, profile, username, false);
            return true;
        }

        return false;
    }

    private boolean checkNameChange(S source, String username, UUID premiumUUID) {
        //user not exists in the db
        if (core.getConfig().get("nameChangeCheck", false)) {
            PlayerProfile profile = core.getStorage().loadProfile(premiumUUID);
            if (profile != null) {
                //uuid exists in the database
                core.getPlugin().getLogger().log(Level.FINER, "Player {0} changed it's username", premiumUUID);

                //update the username to the new one in the database
                profile.setPlayerName(username);

                requestPremiumLogin(source, profile, username, false);
                return true;
            }
        }

        return false;
    }

    public abstract void requestPremiumLogin(S source, PlayerProfile profile, String username, boolean registered);

    public abstract void startCrackedSession(S source, PlayerProfile profile, String username);
}
