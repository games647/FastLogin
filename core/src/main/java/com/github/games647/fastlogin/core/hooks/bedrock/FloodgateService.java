/*
 * SPDX-License-Identifier: MIT
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2022 games647 and contributors
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

import com.github.games647.fastlogin.core.StoredProfile;
import com.github.games647.fastlogin.core.shared.FastLoginCore;
import com.github.games647.fastlogin.core.shared.LoginSource;

import java.util.Locale;
import java.util.UUID;

import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

public class FloodgateService extends BedrockService<FloodgatePlayer> {

    private final FloodgateApi floodgate;

    public FloodgateService(FloodgateApi floodgate, FastLoginCore<?, ?, ?> core) {
        super(core);
        this.floodgate = floodgate;
    }

    /**
     * Checks if a config entry (related to Floodgate) is valid. <br>
     * Writes to Log if the value is invalid.
     * <p>
     * This should be used for:
     * <ul>
     * <li>allowFloodgateNameConflict
     * <li>autoLoginFloodgate
     * <li>autoRegisterFloodgate
     * </ul>
     * </p>
     *
     * @param key the key of the entry in config.yml
     * @return <b>true</b> if the entry's value is "true", "false", or "linked"
     */
    public boolean isValidFloodgateConfigString(String key) {
        String value = core.getConfig().get(key).toString().toLowerCase(Locale.ENGLISH);
        if (!value.equals("true") && !value.equals("linked") && !value.equals("false") && !value.equals("no-conflict")) {
            core.getPlugin().getLog().error("Invalid value detected for {} in FastLogin/config.yml.", key);
            return false;
        }

        return true;
    }

    @Override
    public boolean isUsernameForbidden(StoredProfile profile) {
        String playerPrefix = floodgate.getPlayerPrefix();
        return profile.getName().startsWith(playerPrefix) && !playerPrefix.isEmpty();
    }

    @Override
    public boolean performChecks(String username, LoginSource source) {
        // check if the Bedrock player is linked to a Java account
        FloodgatePlayer floodgatePlayer = getBedrockPlayer(username);
        boolean isLinked = floodgatePlayer.getLinkedPlayer() != null;

        if ("false".equals(allowConflict)
            || "linked".equals(allowConflict) && !isLinked) {
                super.checkNameConflict(username, source);
        } else {
            core.getPlugin().getLog().info("Skipping name conflict checking for player {}", username);
        }
        
        //Floodgate users don't need Java specific checks
        return true;
    }

    /**
     * The FloodgateApi does not support querying players by name, so this function
     * iterates over every online FloodgatePlayer and checks if the requested
     * username can be found
     * <br>
     * <i>Falls back to non-prefixed name checks, if ProtocolLib is installed</i>
     * 
     * @param prefixedUsername the name of the player with the prefix appended
     * @return FloodgatePlayer if found, null otherwise
     */
    public FloodgatePlayer getBedrockPlayer(String prefixedUsername) {
        //prefixes are broken with ProtocolLib, so fall back to name checks without prefixes
        //this should be removed if #493 gets fixed
        if (core.getPlugin().isPluginInstalled("ProtocolLib")) {
            for (FloodgatePlayer floodgatePlayer : floodgate.getPlayers()) {
                if (floodgatePlayer.getUsername().equals(prefixedUsername)) {
                    return floodgatePlayer;
                }
            }
            return null;
        }
        for (FloodgatePlayer floodgatePlayer : floodgate.getPlayers()) {
            if (floodgatePlayer.getCorrectUsername().equals(prefixedUsername)) {
                return floodgatePlayer;
            }
        }

        return null;
    }

    public FloodgatePlayer getBedrockPlayer(UUID uuid) {
        return floodgate.getPlayer(uuid);
    }

    public boolean isBedrockPlayer(UUID uuid) {
        return getBedrockPlayer(uuid) != null;
    }

    public boolean isBedrockConnection(String username) {
        return getBedrockPlayer(username) != null;
    }
}
