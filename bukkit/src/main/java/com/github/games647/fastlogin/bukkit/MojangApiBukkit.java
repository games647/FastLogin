package com.github.games647.fastlogin.bukkit;

import com.github.games647.fastlogin.core.mojang.MojangApiConnector;
import com.github.games647.fastlogin.core.mojang.SkinProperties;
import com.github.games647.fastlogin.core.mojang.VerificationReply;
import com.github.games647.fastlogin.core.shared.LoginSession;
import com.google.common.net.HostAndPort;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MojangApiBukkit extends MojangApiConnector {

    //mojang api check to prove a player is logged in minecraft and made a join server request
    private static final String HAS_JOINED_URL = "https://sessionserver.mojang.com/session/minecraft/hasJoined?" +
            "username=%s&serverId=%s";

    public MojangApiBukkit(Logger logger, Collection<String> localAddresses, int rateLimit
            , List<HostAndPort> proxies) {
        super(logger, localAddresses, rateLimit, proxies);
    }

    @Override
    public boolean hasJoinedServer(LoginSession session, String serverId, InetSocketAddress ip) {
        BukkitLoginSession playerSession = (BukkitLoginSession) session;

        String url = String.format(HAS_JOINED_URL, playerSession.getUsername(), serverId);
        try {
            if (ip != null) {
                url += "&ip=" + URLEncoder.encode(ip.getAddress().getHostAddress(), "UTF-8");
            }

            HttpURLConnection conn = getConnection(url);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                //validate parsing
                //http://wiki.vg/Protocol_Encryption#Server
                VerificationReply verification = gson.fromJson(reader, VerificationReply.class);
                playerSession.setUuid(verification.getId());

                SkinProperties[] properties = verification.getProperties();
                if (properties != null && properties.length > 0) {
                    SkinProperties skinProperty = properties[0];
                    playerSession.setSkinProperty(skinProperty);
                }

                return true;
            }
        } catch (Exception ex) {
            //catch not only io-exceptions also parse and NPE on unexpected json format
            logger.log(Level.WARNING, "Failed to verify session", ex);
        }

        //this connection doesn't need to be closed. So can make use of keep alive in java
        return false;
    }
}
