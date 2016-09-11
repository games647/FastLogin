package com.github.games647.fastlogin.core;

import java.util.UUID;
import java.util.logging.Level;

public abstract class JoinManagement<T, S extends LoginSource> {

    protected final FastLoginCore core;
    protected final AuthPlugin<T> authHook;

    public JoinManagement(FastLoginCore core, AuthPlugin<T> authHook) {
        this.core = core;
        this.authHook = authHook;
    }

    public void onLogin(String username, S source) {
        PlayerProfile profile = core.getStorage().loadProfile(username);
        if (profile == null) {
            return;
        }

        SharedConfig sharedConfig = core.getSharedConfig();

        String ip = source.getAddress().getAddress().getHostAddress();
        try {
            if (profile.getUserId() == -1) {
                if (core.getPendingLogins().containsKey(ip + username)
                        && sharedConfig.get("secondAttemptCracked", false)) {
                    core.getLogger().log(Level.INFO, "Second attempt login -> cracked {0}", username);

                    //first login request failed so make a cracked session
                    startCrackedSession(source, profile, username);
                    return;
                }

                UUID premiumUUID = null;
                if (sharedConfig.get("nameChangeCheck", false) || sharedConfig.get("autoRegister", false)) {
                    premiumUUID = core.getMojangApiConnector().getPremiumUUID(username);
                }

                if (premiumUUID == null
                        || (!checkNameChange(premiumUUID, source, username)
                        && !checkPremiumName(username, source, profile))) {
                    //nothing detected the player as premium -> start a cracked session
                    startCrackedSession(source, profile, username);
                }
            } else if (profile.isPremium()) {
                requestPremiumLogin(source, profile, username, true);
            } else {
                if (core.getSharedConfig().get("switchMode", false)) {
                    source.kick(core.getMessage("switch-kick-message"));
                    return;
                }

                startCrackedSession(source, profile, username);
            }
        } catch (Exception ex) {
            core.getLogger().log(Level.SEVERE, "Failed to check premium state", ex);
        }
    }

    private boolean checkPremiumName(String username, S source, PlayerProfile profile)
            throws Exception {
        if (core.getSharedConfig().get("autoRegister", false)
                && (authHook == null || !authHook.isRegistered(username))) {
            core.getLogger().log(Level.FINER, "Player {0} uses a premium username", username);
            requestPremiumLogin(source, profile, username, false);
            return true;
        }

        return false;
    }

    private boolean checkNameChange(UUID premiumUUID, S source, String username) {
        //user not exists in the db
        if (core.getSharedConfig().get("nameChangeCheck", false)) {
            PlayerProfile profile = core.getStorage().loadProfile(premiumUUID);
            if (profile != null) {
                //uuid exists in the database
                core.getLogger().log(Level.FINER, "Player {0} changed it's username", premiumUUID);

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
