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
package com.github.games647.fastlogin.core.hooks.bedrock;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import com.github.games647.craftapi.model.Profile;
import com.github.games647.craftapi.resolver.RateLimitException;
import com.github.games647.fastlogin.core.StoredProfile;
import com.github.games647.fastlogin.core.shared.FastLoginCore;
import com.github.games647.fastlogin.core.shared.LoginSource;

/**
 * @param B is an instance of either FloodgatePlayer or GeyserSession
 */
public abstract class BedrockService<B> {
    
    protected final FastLoginCore<?, ?, ?> core;
    protected final String allowConflict;

    public BedrockService(FastLoginCore<?, ?, ?> core) {
        this.core = core;
        this.allowConflict = core.getConfig().get("allowFloodgateNameConflict").toString().toLowerCase();
    }

    /**
     * Check if the player's name conflicts an existing Java player's name, and kick
     * them if it does
     *
     * @param username the name of the player
     * @param source   an instance of LoginSource
     */
    public void checkNameConflict(String username, LoginSource source) {
        // check for conflicting Premium Java name
        Optional<Profile> premiumUUID = Optional.empty();
        try {
            premiumUUID = core.getResolver().findProfile(username);
        } catch (IOException | RateLimitException e) {
            core.getPlugin().getLog().error(
                    "Could not check whether Bedrock Player {}'s name conflicts a premium Java player's name.",
                    username);
            try {
                source.kick("Could not check if your name conflicts an existing premium Java account's name.\n"
                        + "This is usually a serverside error.");
            } catch (Exception ex) {
                core.getPlugin().getLog().error("Could not kick Player {}", username, ex);
            }
        }

        if (premiumUUID.isPresent()) {
            core.getPlugin().getLog().info("Bedrock Player {}'s name conflicts an existing premium Java account's name",
                    username);
            try {
                source.kick("Your name conflicts an existing premium Java account's name");
            } catch (Exception ex) {
                core.getPlugin().getLog().error("Could not kick Player {}", username, ex);
            }
        }

    }

    /**
     * The Floodgate / Geyser API does not support querying players by name, so this function
     * iterates over every online Bedrock Player and checks if the requested
     * username can be found
     * <br>
     * <i>Falls back to non-prefixed name checks, if ProtocolLib is installed</i>
     * 
     * @param prefixedUsername the name of the player with the prefix appended
     * @return Bedrock Player if found, null otherwise
     */
    public B getBedrockPlayer(String prefixedUsername) {
        return null;
    }

    public B getBedrockPlayer(UUID uuid) {
        return null;
    }

    public boolean isBedrockPlayer(UUID uuid) {
        return getBedrockPlayer(uuid) != null;
    }

    public boolean isBedrockConnection(String username) {
        return getBedrockPlayer(username) != null;
    }

    /**
     * Checks if a profile's name starts with the Floodgate prefix, if it's available
     * @param profile profile of the conecting player
     * @return true if the username is forbidden
     */
    public boolean isUsernameForbidden(StoredProfile profile) {
        return false;
    }

}
