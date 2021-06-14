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
package com.github.games647.fastlogin.core.hooks;

import java.io.IOException;
import java.util.Optional;

import com.github.games647.craftapi.model.Profile;
import com.github.games647.craftapi.resolver.RateLimitException;
import com.github.games647.fastlogin.core.shared.FastLoginCore;
import com.github.games647.fastlogin.core.shared.LoginSource;

import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

public class FloodgateHook<P extends C, C, S extends LoginSource> {

    private final FastLoginCore<P, C, ?> core;

    public FloodgateHook(FastLoginCore<P, C, ?> core) {
        this.core = core;
    }

    /**
     * Check if the player's name conflicts an existing Java player's name, and
     * kick them if it does
     *
     * @param username the name of the player
     * @param source   an instance of LoginSource
     */
    public void checkFloodgateNameConflict(String username, LoginSource source, FloodgatePlayer floodgatePlayer) {
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
     * The FloodgateApi does not support querying players by name, so this function
     * iterates over every online FloodgatePlayer and checks if the requested
     * username can be found
     * 
     * @param username the name of the player
     * @return FloodgatePlayer if found, null otherwise
     */
    public FloodgatePlayer getFloodgatePlayer(String username) {
        if (core.getPlugin().isPluginInstalled("floodgate")) {
            for (FloodgatePlayer floodgatePlayer : FloodgateApi.getInstance().getPlayers()) {
                if (floodgatePlayer.getUsername().equals(username)) {
                    return floodgatePlayer;
                }
            }
        }
        return null;
    }

}
