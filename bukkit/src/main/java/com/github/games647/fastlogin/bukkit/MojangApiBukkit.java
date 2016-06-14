package com.github.games647.fastlogin.bukkit;

import com.github.games647.fastlogin.core.FastLoginCore;
import com.github.games647.fastlogin.core.MojangApiConnector;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.UUID;
import java.util.logging.Level;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class MojangApiBukkit extends MojangApiConnector {

    //mojang api check to prove a player is logged in minecraft and made a join server request
    private static final String HAS_JOINED_URL = "https://sessionserver.mojang.com/session/minecraft/hasJoined?";

    public MojangApiBukkit(FastLoginCore plugin) {
        super(plugin);
    }

    @Override
    public boolean hasJoinedServer(Object session, String serverId) {
        if (!(session instanceof BukkitLoginSession)) {
            return false;
        }

        BukkitLoginSession playerSession = (BukkitLoginSession) session;
        try {
            String url = HAS_JOINED_URL + "username=" + playerSession.getUsername() + "&serverId=" + serverId;
            HttpURLConnection conn = getConnection(url);

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = reader.readLine();
            if (line != null && !line.equals("null")) {
                //validate parsing
                //http://wiki.vg/Protocol_Encryption#Server
                JSONObject userData = (JSONObject) JSONValue.parseWithException(line);
                String uuid = (String) userData.get("id");
                playerSession.setUuid(FastLoginCore.parseId(uuid));

                JSONArray properties = (JSONArray) userData.get("properties");
                JSONObject skinProperty = (JSONObject) properties.get(0);

                String propertyName = (String) skinProperty.get("name");
                if (propertyName.equals("textures")) {
                    String skinValue = (String) skinProperty.get("value");
                    String signature = (String) skinProperty.get("signature");
                    playerSession.setSkin(skinValue, signature);
                }

                return true;
            }
        } catch (Exception ex) {
            //catch not only ioexceptions also parse and NPE on unexpected json format
            plugin.getLogger().log(Level.WARNING, "Failed to verify session", ex);
        }

        //this connection doesn't need to be closed. So can make use of keep alive in java
        return false;
    }

    @Override
    protected UUID getUUIDFromJson(String json) {
        JSONObject userData = (JSONObject) JSONValue.parse(json);
        String uuid = (String) userData.get("id");
        return FastLoginCore.parseId(uuid);
    }
}
