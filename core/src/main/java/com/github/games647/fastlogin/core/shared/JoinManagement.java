package com.github.games647.fastlogin.core.shared;

import com.github.games647.craftapi.model.Profile;
import com.github.games647.craftapi.resolver.RateLimitException;
import com.github.games647.fastlogin.core.StoredProfile;
import com.github.games647.fastlogin.core.hooks.AuthPlugin;
import com.github.games647.fastlogin.core.shared.event.FastLoginPreLoginEvent;

import java.util.Optional;

import net.md_5.bungee.config.Configuration;

public abstract class JoinManagement<P extends C, C, S extends LoginSource> {

    protected final FastLoginCore<P, C, ?> core;
    protected final AuthPlugin<P> authHook;

    public JoinManagement(FastLoginCore<P, C, ?> core, AuthPlugin<P> authHook) {
        this.core = core;
        this.authHook = authHook;
    }

    public void onLogin(String username, S source) {
        StoredProfile profile = core.getStorage().loadProfile(username);
        if (profile == null) {
            return;
        }

        callFastLoginPreLoginEvent(username, source, profile);

        Configuration config = core.getConfig();

        String ip = source.getAddress().getAddress().getHostAddress();
        profile.setLastIp(ip);
        try {
            if (profile.isSaved()) {
                if (profile.isPremium()) {
                    core.getPlugin().getLog().info("Requesting premium login for registered player: {}", username);
                    requestPremiumLogin(source, profile, username, true);
                } else {
                    startCrackedSession(source, profile, username);
                }
            } else {
                if (core.getPendingLogin().remove(ip + username) != null && config.get("secondAttemptCracked", false)) {
                    core.getPlugin().getLog().info("Second attempt login -> cracked {}", username);

                    //first login request failed so make a cracked session
                    startCrackedSession(source, profile, username);
                    return;
                }

                Optional<Profile> premiumUUID = Optional.empty();
                if (config.get("nameChangeCheck", false) || config.get("autoRegister", false)) {
                    premiumUUID = core.getResolver().findProfile(username);
                }

                if (!premiumUUID.isPresent()
                        || (!checkNameChange(source, username, premiumUUID.get())
                        && !checkPremiumName(source, username, profile))) {
                    //nothing detected the player as premium -> start a cracked session
                    if (core.getConfig().get("switchMode", false)) {
                        source.kick(core.getMessage("switch-kick-message"));
                        return;
                    }

                    startCrackedSession(source, profile, username);
                }
            }
        } catch (RateLimitException rateLimitEx) {
            core.getPlugin().getLog().error("Mojang's rate limit reached for {}. The public IPv4 address of this" +
                    " server issued more than 600 Name -> UUID requests within 10 minutes. After those 10" +
                    " minutes we can make requests again.", username);
        } catch (Exception ex) {
            core.getPlugin().getLog().error("Failed to check premium state for {}", username, ex);
            core.getPlugin().getLog().error("Failed to check premium state of {}", username, ex);
        }
    }

    private boolean checkPremiumName(S source, String username, StoredProfile profile) throws Exception {
        core.getPlugin().getLog().info("GameProfile {} uses a premium username", username);
        if (core.getConfig().get("autoRegister", false) && (authHook == null || !authHook.isRegistered(username))) {
            requestPremiumLogin(source, profile, username, false);
            return true;
        }

        return false;
    }

    private boolean checkNameChange(S source, String username, Profile profile) {
        //user not exists in the db
        if (core.getConfig().get("nameChangeCheck", false)) {
            StoredProfile storedProfile = core.getStorage().loadProfile(profile.getId());
            if (storedProfile != null) {
                //uuid exists in the database
                core.getPlugin().getLog().info("GameProfile {} changed it's username", profile);

                //update the username to the new one in the database
                storedProfile.setPlayerName(username);

                requestPremiumLogin(source, storedProfile, username, false);
                return true;
            }
        }

        return false;
    }

    public abstract FastLoginPreLoginEvent callFastLoginPreLoginEvent(String username, S source, StoredProfile profile);

    public abstract void requestPremiumLogin(S source, StoredProfile profile, String username, boolean registered);

    public abstract void startCrackedSession(S source, StoredProfile profile, String username);
}
