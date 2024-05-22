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
package com.github.games647.fastlogin.bukkit.auth;

import java.net.InetAddress;

public final class InetUtils {

    private InetUtils() {
        // Utility
    }

    /**
     * Verifies if the given IP address is from the local network
     *
     * @param address IP address
     * @return true if address is from local network or even from the device itself (loopback)
     */
    public static boolean isLocalAddress(InetAddress address) {
        // Loopback addresses like 127.0.* (IPv4) or [::1] (IPv6)
        return address.isLoopbackAddress()
                // Example: 10.0.0.0, 172.16.0.0, 192.168.0.0, fec0::/10 (deprecated)
                // Ref: https://en.wikipedia.org/wiki/IP_address#Private_addresses
                || address.isSiteLocalAddress()
                // Example: 169.254.0.0/16, fe80::/10
                // Ref: https://en.wikipedia.org/wiki/IP_address#Address_autoconfiguration
                || address.isLinkLocalAddress()
                // non deprecated unique site-local that java doesn't check yet -> fc00::/7
                || isIPv6UniqueSiteLocal(address);
    }

    private static boolean isIPv6UniqueSiteLocal(InetAddress address) {
        // ref: https://en.wikipedia.org/wiki/Unique_local_address

        // currently undefined but could be used in the near future fc00::/8
        return (address.getAddress()[0] & 0xFF) == 0xFC
                // in use for unique site-local fd00::/8
                || (address.getAddress()[0] & 0xFF) == 0xFD;
    }
}
