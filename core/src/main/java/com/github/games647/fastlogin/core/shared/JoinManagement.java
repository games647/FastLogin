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
import com.github.games647.fastlogin.core.shared.event.FastLoginPreLoginEvent;

import java.io.IOException;
import java.util.Optional;

import org.geysermc.floodgate.api.player.FloodgatePlayer;

import net.md_5.bungee.config.Configuration;

public abstract class JoinManagement<P extends C, C, S extends LoginSource> {

    protected final FastLoginCore<P, C, ?> core;
    protected final AuthPlugin<P> authHook;

    public JoinManagement(FastLoginCore<P, C, ?> core, AuthPlugin<P> authHook) {
        this.core = core;
        this.authHook = authHook;
    }

    public void onLogin(String username, S source) {
        core.getPlugin().getLog().info("Handling player {}", username);
        StoredProfile profile = core.getStorage().loadProfile(username);
        if (profile == null) {
            return;
        }

        //check if the player is connecting through Floodgate
        Object floodgatePlayer = getFloodgatePlayer(username);

        if (floodgatePlayer != null) {
            checkFloodgateNameConflict(username, source, floodgatePlayer);
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

    /**
     * Check if the player's name conflicts an existing Java player's name, and
     * kick them if it does
     *
     * @param core     the FastLoginCore
     * @param username the name of the player
     * @param source   an instance of LoginSource
     */
    public void checkFloodgateNameConflict(String username, LoginSource source, Object floodgatePlayer) {
        String allowConflict = core.getConfig().get("allowFloodgateNameConflict").toString().toLowerCase();

        // check if the Bedrock player is linked to a Java account
        boolean isLinked = ((FloodgatePlayer) floodgatePlayer).getLinkedPlayer() != null;

        if (allowConflict.equals("false")
                || allowConflict.equals("linked") && !isLinked) {

            // check for conflicting Premium Java name
            Optional<Profile> premiumUUID = Optional.empty();
            try {
                premiumUUID = core.getResolver().findProfile(username);
            } catch (IOException | RateLimitException e) {
                core.getPlugin().getLog().error(
                        "Could not check wether Floodgate Player {}'s name conflicts a premium Java player's name.",
                        username);
                try {
                    source.kick("Could not check if your name conflicts an existing Java Premium Player's name");
                } catch (Exception e1) {
                    core.getPlugin().getLog().error("Could not kick Player {}", username);
                }
            }

            if (premiumUUID.isPresent()) {
                core.getPlugin().getLog().info("Bedrock Player {}'s name conflicts an existing Java Premium Player's name",
                        username);
                try {
                    source.kick("Your name conflicts an existing Java Premium Player's name");
                } catch (Exception e) {
                    core.getPlugin().getLog().error("Could not kick Player {}", username);
                }
            }
        } else {
            core.getPlugin().getLog().info("Skipping name conflict checking for player {}", username);
        }
    }

    /**
     * Gets a FloodgatePlayer based on name or UUID Note: Don't change the return
     * type from Object to FloodgatePlayer, unless you want ProtocolSupport to throw
     * an error if Floodgate is not installed
     * 
     * @param id UUID for BungeeCord, username for Bukkit
     * @return an instance of FloodgatePlayer, if Floodgate is installed and a
     *         player is found <br>
     *         null if Floodgate is unavailable
     */
    protected abstract Object getFloodgatePlayer(Object id);

    public abstract FastLoginPreLoginEvent callFastLoginPreLoginEvent(String username, S source, StoredProfile profile);

    public abstract void requestPremiumLogin(S source, StoredProfile profile, String username, boolean registered);

    public abstract void startCrackedSession(S source, StoredProfile profile, String username);
}
