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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.UUID;

import org.geysermc.floodgate.api.player.FloodgatePlayer;

import com.github.games647.craftapi.model.Profile;
import com.github.games647.craftapi.resolver.RateLimitException;
import com.github.games647.fastlogin.core.StoredProfile;
import com.github.games647.fastlogin.core.hooks.AuthPlugin;

public abstract class FloodgateManagement<P extends C, C, L extends LoginSession, T extends PlatformPlugin<C>>
        implements Runnable {

    protected final FastLoginCore<P, C, T> core;
    protected final P player;
    private final FloodgatePlayer floodgatePlayer;
    private final String username;

    //variables initialized through run() and accesses by subclasss
    protected boolean isRegistered;
    protected StoredProfile profile;
    protected String autoLoginFloodgate;
    protected boolean isLinked;
    protected boolean performLogin; //will be set to ture if core#run() wasn't interrupted by a return;

    public FloodgateManagement(FastLoginCore<P, C, T> core, P player, FloodgatePlayer floodgatePlayer) {
        this.core = core;
        this.player = player;
        this.floodgatePlayer = floodgatePlayer;
        this.username = getName(player);
    }

    @Override
    public void run() {
        core.getPlugin().getLog().info(
                "Player {} is connecting through Geyser Floodgate.",
                username);

        // check if the Bedrock player is linked to a Java account 
        isLinked = floodgatePlayer.getLinkedPlayer() != null;
        AuthPlugin<P> authPlugin = core.getAuthPluginHook();

        autoLoginFloodgate = core.getConfig().get("autoLoginFloodgate").toString().toLowerCase();
        String autoRegisterFloodgate = core.getConfig().get("autoRegisterFloodgate").toString().toLowerCase();
        String allowNameConflict = core.getConfig().get("allowFloodgateNameConflict").toString().toLowerCase();
        
        try {
            isRegistered = authPlugin.isRegistered(username);
        } catch (Exception e) {
            core.getPlugin().getLog().error(
                    "An error has occured while checking if player {} is registered",
                    username);
            return;
        }

        //decide if checks should be made for conflicting Java player names
        if (!isLinked //linked players have the same name as their Java profile
                // if allowNameConflict is 'false' or 'linked' and the player had a conflicting
                // name, than they would have been kicked in FloodgateHook#checkNameConflict
                && allowNameConflict.equals("true") &&
                (
                        autoLoginFloodgate.equals("no-conflict")
                        || !isRegistered && autoRegisterFloodgate.equals("no-conflict"))
                ) {
            // check for conflicting Premium Java name
            Optional<Profile> premiumUUID = Optional.empty();
            try {
                premiumUUID = core.getResolver().findProfile(username);
            } catch (IOException | RateLimitException e) {
                core.getPlugin().getLog().error(
                        "Could not check wether Floodgate Player {}'s name conflits a premium Java player's name.",
                        username);
                return;
            }

            //stop execution if player's name is conflicting
            if (premiumUUID.isPresent()) {
                return;
            }
        }

        if (!isRegistered && autoRegisterFloodgate.equals("false")) {
            return;
        }

        // logging in from bedrock for a second time threw an error with UUID
        profile = core.getStorage().loadProfile(username);
        if (profile == null) {
            profile = new StoredProfile(getUUID(player), username, true, getAddress(player).toString());
        }

        performLogin = true;

    }

    protected abstract String getName(P player);
    protected abstract UUID getUUID(P player);
    protected abstract InetSocketAddress getAddress(P player);

}
