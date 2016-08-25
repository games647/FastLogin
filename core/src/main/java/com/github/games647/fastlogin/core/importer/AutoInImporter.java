package com.github.games647.fastlogin.core.importer;

import com.github.games647.fastlogin.core.AuthStorage;
import com.github.games647.fastlogin.core.PlayerProfile;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import javax.sql.DataSource;

public class AutoInImporter extends Importer {

    private static final String PLUGIN_NAME = "AutoIn";

    private static final String SQLITE_FILE = "plugins/" + PLUGIN_NAME + "/AutoIn_PlayerOptions.db";

    private static final String USER_TABLE = "nicknames";
    private static final String UUID_TABLE = "uuids";
    private static final String SESSION_TABLE = "sessions";

    public static String getSQLitePath() {
        return SQLITE_FILE;
    }

    @Override
    public int importData(Connection source, DataSource target, AuthStorage storage) throws SQLException {
        Statement stmt = null;
        ResultSet resultSet = null;
        try {
            stmt = source.createStatement();
            resultSet = stmt.executeQuery("SELECT name, protection, premium, puuid FROM " + USER_TABLE
                    + " LEFT JOIN " + " ("
                    /* Prevent duplicates */
                    + "SELECT * FROM " + UUID_TABLE + " GROUP BY nickname_id"
                    + ") uuids"
                    + " ON " + USER_TABLE + ".id = uuids.nickname_id");

            int rows = 0;
            while (resultSet.next()) {
                String name = resultSet.getString(1);
                boolean protection = resultSet.getBoolean(2);
                /* Enable premium authentication only for those who want to be auto logged in,
                so they have their cracked protection disabled */
                boolean premium = !protection && resultSet.getBoolean(3);
                String puuid = resultSet.getString(4);

                /* FastLogin will also make lookups on the uuid column for name changes
                the old 1.6.2 version won't check if those user have premium enabled

                so it could happen that a premium could steal the account if we don't do this

                It seems the uuid is saved on autoin too if the player is cracked */
                PlayerProfile profile;
                if (premium) {
                    profile = new PlayerProfile(UUID.fromString(puuid), name, premium, "");
                } else {
                    profile = new PlayerProfile(null, name, premium, "");
                }

                storage.save(profile);
                rows++;
            }

            return rows;
        } finally {
            closeQuietly(stmt);
            closeQuietly(resultSet);
        }
    }
}
