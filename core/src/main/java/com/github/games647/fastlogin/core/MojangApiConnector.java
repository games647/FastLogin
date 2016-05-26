package com.github.games647.fastlogin.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Pattern;

public abstract class MojangApiConnector {

    //http connection, read timeout and user agent for a connection to mojang api servers
    private static final int TIMEOUT = 1 * 1_000;
    private static final String USER_AGENT = "Premium-Checker";

    //only premium (paid account) users have a uuid from here
    private static final String UUID_LINK = "https://api.mojang.com/users/profiles/minecraft/";
    //this includes a-zA-Z1-9_
    private static final String VALID_PLAYERNAME = "^\\w{2,16}$";

    //compile the pattern only on plugin enable -> and this have to be threadsafe
    private final Pattern playernameMatcher = Pattern.compile(VALID_PLAYERNAME);

    protected final FastLoginCore plugin;

    public MojangApiConnector(FastLoginCore plugin) {
        this.plugin = plugin;
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
            try {
                HttpURLConnection connection = getConnection(UUID_LINK + playerName);
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String line = reader.readLine();
                    if (line != null && !line.equals("null")) {
                        return getUUIDFromJson(line);
                    }
                }
                //204 - no content for not found
            } catch (Exception ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to check if player has a paid account", ex);
            }
            //this connection doesn't need to be closed. So can make use of keep alive in java
        }

        return null;
    }

    public abstract boolean hasJoinedServer(Object session, String serverId);

    protected abstract UUID getUUIDFromJson(String json);

    protected HttpURLConnection getConnection(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(TIMEOUT);
        connection.setReadTimeout(2 * TIMEOUT);
        //the new Mojang API just uses json as response
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", USER_AGENT);

        return connection;
    }
}
