package com.github.games647.fastlogin.core.importer;

import com.github.games647.fastlogin.core.AuthStorage;
import com.github.games647.fastlogin.core.PlayerProfile;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.UUID;

import javax.sql.DataSource;

public class ElDziAuthImporter extends Importer {

    private static final String TABLE_NAME = "accounts";

    @Override
    public int importData(Connection source, DataSource target, AuthStorage storage) throws SQLException {
        Statement stmt = null;
        ResultSet resultSet = null;
        try {
            stmt = source.createStatement();
            resultSet = stmt.executeQuery("SELECT "
                    + "nick, "
                    + "premium, "
                    + "lastIP, "
                    + "FROM_UNIXTIME(lastPlayed * 0.001) AS LastLogin "
                    + "FROM " + TABLE_NAME);

            int rows = 0;
            while (resultSet.next()) {
                String name = resultSet.getString(1);
                boolean premium = resultSet.getBoolean(2);
                String lastIP = resultSet.getString(3);
                Timestamp lastLogin = resultSet.getTimestamp(4);

                String uuid = resultSet.getString(5);

                PlayerProfile profile;
                if (premium) {
                    profile = new PlayerProfile(UUID.fromString(uuid), name, premium, lastIP);
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
