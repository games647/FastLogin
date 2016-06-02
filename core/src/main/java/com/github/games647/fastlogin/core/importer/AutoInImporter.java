package com.github.games647.fastlogin.core.importer;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

public class AutoInImporter extends Importer {

    private static final String USER_TABLE = "nicknames";
    private static final String UUID_TABLE = "uuids";
    private static final String SESSION_TABLE = "sessions";

    @Override
    public int importData(DataSource source, DataSource target, String targetTable) throws SQLException {
        Connection con = null;
        Statement stmt = null;
        try {
            con = source.getConnection();
            stmt = con.createStatement();
            int importedRows = stmt.executeUpdate("INSERT INTO " + targetTable + " SELECT"
                    + " name AS Name,"
                    + " enabledLogin AS Premium,"
                    + " '' AS LastIp,"
                    + " REPLACE(puuid, '-', '') AS UUID"
                    + " FROM " + USER_TABLE
                    + " JOIN " + UUID_TABLE
                    + " ON " + UUID_TABLE + ".id = " + UUID_TABLE + ".nickname_id");
            return importedRows;
        } finally {
            closeQuietly(stmt);
            closeQuietly(con);
        }
    }
}
