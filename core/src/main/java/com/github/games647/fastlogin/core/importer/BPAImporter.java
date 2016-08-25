package com.github.games647.fastlogin.core.importer;

import com.github.games647.fastlogin.core.AuthStorage;
import com.github.games647.fastlogin.core.PlayerProfile;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import javax.sql.DataSource;

public class BPAImporter extends Importer {

    private static final String DEFAULT_TABLE_NAME = "users";

    @Override
    public int importData(Connection source, DataSource target, AuthStorage storage) throws SQLException {
        Statement stmt = null;
        ResultSet resultSet = null;
        try {
            stmt = source.createStatement();
            resultSet = stmt.executeQuery("SELECT "
                    + "nick, "
                    + "checked, "
                    + "lastIP, "
                    + "FROM_UNIXTIME(lastJoined * 0.001) AS LastLogin "
                    + "FROM " + DEFAULT_TABLE_NAME);

            int rows = 0;
            while (resultSet.next()) {
                String name = resultSet.getString(1);
                boolean premium = resultSet.getBoolean(2);
                String lastIP = resultSet.getString(3);
                Timestamp lastLogin = resultSet.getTimestamp(4);

                //uuid doesn't exist here
                PlayerProfile profile = new PlayerProfile(null, name, premium, lastIP);
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
