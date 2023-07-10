package com.github.games647.fastlogin.bukkit;

import java.net.InetAddress;

public class InetUtils {

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
