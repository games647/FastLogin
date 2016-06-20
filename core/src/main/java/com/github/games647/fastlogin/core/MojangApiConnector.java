package com.github.games647.fastlogin.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

public abstract class MojangApiConnector {

    //http connection, read timeout and user agent for a connection to mojang api servers
    private static final int TIMEOUT = 1 * 1_000;
    private static final String USER_AGENT = "Premium-Checker";

    private static final String MCAPI_UUID_URL = "https://us.mc-api.net/v3/uuid/";

    //only premium (paid account) users have a uuid from here
    private static final String UUID_LINK = "https://api.mojang.com/users/profiles/minecraft/";
    //this includes a-zA-Z1-9_
    private static final String VALID_PLAYERNAME = "^\\w{2,16}$";

    private static final int RATE_LIMIT_CODE = 429;

    //compile the pattern only on plugin enable -> and this have to be threadsafe
    private final Pattern playernameMatcher = Pattern.compile(VALID_PLAYERNAME);

    private final BalancedSSLFactory sslFactory;
    private final boolean apiLookup;
    private long lastRateLimit;

    protected final FastLoginCore plugin;

    public MojangApiConnector(FastLoginCore plugin, List<String> localAddresses, boolean apiLookup) {
        this.plugin = plugin;
        this.apiLookup = apiLookup;

        if (localAddresses.isEmpty()) {
            this.sslFactory = null;
        } else {
            Set<InetAddress> addresses = new HashSet<>();
            for (String localAddress : localAddresses) {
                try {
                    InetAddress address = InetAddress.getByName(localAddress);
                    if (!address.isAnyLocalAddress()) {
                        plugin.getLogger().log(Level.WARNING, "Submitted IP-Address is not local", address);
                        continue;
                    }

                    addresses.add(address);
                } catch (UnknownHostException ex) {
                    plugin.getLogger().log(Level.SEVERE, "IP-Address is unknown to us", ex);
                }
            }

            this.sslFactory = new BalancedSSLFactory(HttpsURLConnection.getDefaultSSLSocketFactory(), addresses);
        }
    }

    /**
     *
     * @param playerName
     * @return null on non-premium
     */
    public UUID getPremiumUUID(String playerName) {
        //check if it's a valid playername
        if (playernameMatcher.matcher(playerName).matches()) {
            //only make a API call if the name is valid existing mojang account

            if (System.currentTimeMillis() - lastRateLimit < 1_000 * 60 * 10) {
                plugin.getLogger().fine("STILL WAITING FOR RATE_LIMIT - TRYING Third-party API");
                return getUUIDFromAPI(playerName);
            }

            try {
                HttpsURLConnection connection = getConnection(UUID_LINK + playerName);
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String line = reader.readLine();
                    if (line != null && !line.equals("null")) {
                        return getUUIDFromJson(line);
                    }
                } else if (connection.getResponseCode() == RATE_LIMIT_CODE) {
                    plugin.getLogger().info("RATE_LIMIT REACHED - TRYING SECOND API");
                    lastRateLimit = System.currentTimeMillis();
                    return getUUIDFromAPI(playerName);
                }
                //204 - no content for not found
            } catch (Exception ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to check if player has a paid account", ex);
            }
            //this connection doesn't need to be closed. So can make use of keep alive in java
        }

        return null;
    }

    public UUID getUUIDFromAPI(String playerName) {
        if (!playernameMatcher.matcher(playerName).matches()) {
            return null;
        }

        try {
            HttpURLConnection httpConnection = (HttpURLConnection) new URL(MCAPI_UUID_URL + playerName).openConnection();
            httpConnection.addRequestProperty("Content-Type", "application/json");

            if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                //cracked
                return null;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));
            String line = reader.readLine();
            if (line != null && !line.equals("null")) {
                return getUUIDFromJsonAPI(line);
            }
        } catch (IOException iOException) {
            plugin.getLogger().log(Level.SEVERE, "Tried converting name->uuid from third-party api", iOException);
        }

        return null;
    }

    public abstract boolean hasJoinedServer(Object session, String serverId);

    protected abstract UUID getUUIDFromJson(String json);

    protected abstract UUID getUUIDFromJsonAPI(String json);

    protected HttpsURLConnection getConnection(String url) throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(TIMEOUT);
        connection.setReadTimeout(2 * TIMEOUT);
        //the new Mojang API just uses json as response
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", USER_AGENT);

        if (sslFactory != null) {
            connection.setSSLSocketFactory(sslFactory);
        }

        return connection;
    }
}
