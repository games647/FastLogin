/*
 * SPDX-License-Identifier: MIT
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2021 <Your name and contributors>
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
package com.github.games647.fastlogin.core.shared;

import com.github.games647.craftapi.model.Profile;
import com.github.games647.craftapi.resolver.RateLimitException;
import com.github.games647.fastlogin.core.StoredProfile;
import com.github.games647.fastlogin.core.hooks.AuthPlugin;
import com.github.games647.fastlogin.core.hooks.FloodgateService;
import com.github.games647.fastlogin.core.shared.event.FastLoginPreLoginEvent;

import java.util.Optional;

import net.md_5.bungee.config.Configuration;

public abstract class JoinManagement<P extends C, C, S extends LoginSource> {

    protected final FastLoginCore<P, C, ?> core;
    protected final AuthPlugin<P> authHook;
    private final FloodgateService floodgateService;

    public JoinManagement(FastLoginCore<P, C, ?> core, AuthPlugin<P> authHook, FloodgateService floodService) {
        this.core = core;
        this.authHook = authHook;
        this.floodgateService = floodService;
    }

    public void onLogin(String username, S source) {
        core.getPlugin().getLog().info("Handling player {}", username);
        StoredProfile profile = core.getStorage().loadProfile(username);
        if (profile == null) {
            return;
        }

        //check if the player is connecting through Floodgate
        if (floodgateService != null) {
            if (floodgateService.isFloodgateConnection(username)) {
                floodgateService.checkNameConflict(username, source);
                // skip flow for any floodgate player
                return;
            }
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
                    if (isValidUsername(source, profile)) {
                        startCrackedSession(source, profile, username);
                    }
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

    protected boolean isValidUsername(LoginSource source, StoredProfile profile) throws Exception {
        if (floodgateService != null && floodgateService.isUsernameForbidden(profile)) {
            core.getPlugin().getLog().info("Floodgate Prefix detected on cracked player");
            source.kick("Your username contains illegal characters");
            return false;
        }

        return true;
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
