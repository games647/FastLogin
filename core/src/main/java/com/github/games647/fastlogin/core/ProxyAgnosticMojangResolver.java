/*
 * SPDX-License-Identifier: MIT
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2024 games647 and contributors
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
package com.github.games647.fastlogin.core;

import com.github.games647.craftapi.model.auth.Verification;
import com.github.games647.craftapi.resolver.MojangResolver;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.util.Optional;

/**
 * An extension to {@link MojangResolver} which allows connection using transparent reverse proxies.
 * The significant difference is that unlike MojangResolver from the CraftAPI implementation, which sends the
 * "ip" parameter when the hostIp parameter is an IPv4 address, but skips it for IPv6, this implementation leaves out
 * the "ip" parameter also for IPv4, effectively enabling transparent proxies to work.
 *
 * @author games647, Enginecrafter77
 */
public class ProxyAgnosticMojangResolver extends MojangResolver {

    private static final String HOST = "sessionserver.mojang.com";

    /**
     * A formatting string containing a URL used to call the {@code hasJoined} method on mojang session servers.
     * <p>
     * Formatting parameters:
     * 1. The username of the player in question
     * 2. The serverId of this server
     */
    public static final String ENDPOINT = "https://" + HOST + "/session/minecraft/hasJoined?username=%s&serverId=%s";

    @Override
    public Optional<Verification> hasJoined(String username, String serverHash, InetAddress hostIp)
        throws IOException {
        String url = String.format(ENDPOINT, username, serverHash);

        HttpURLConnection conn = this.getConnection(url);
        int responseCode = conn.getResponseCode();

        Verification verification = null;

        // Mojang session servers send HTTP 204 (NO CONTENT) when the authentication seems invalid
        // If that's not our case, the authentication is valid, and so we can parse the response.
        if (responseCode != HttpURLConnection.HTTP_NO_CONTENT) {
            verification = this.parseRequest(conn, this::parseVerification);
        }

        return Optional.ofNullable(verification);
    }

    // Functional implementation of InputStreamAction, used in hasJoined method in parseRequest call
    protected Verification parseVerification(InputStream input) throws IOException {
        return this.readJson(input, Verification.class);
    }
}
