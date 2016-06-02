package com.github.games647.fastlogin.core.importer;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

public class BPAImporter extends Importer {

    private static final String DEFAULT_TABLE_NAME = "users";

    @Override
    public int importData(DataSource source, DataSource target, String targetTable) throws SQLException {
        Connection con = null;
        Statement stmt = null;
        try {
            con = source.getConnection();
            stmt = con.createStatement();
            int importedRows = stmt.executeUpdate("INSERT INTO " + targetTable + " SELECT"
                    + " nick AS Name,"
                    + " NULL AS UUID,"
                    + " checked AS Premium,"
                    + " lastIP AS LastIp,"
                    + " FROM_UNIXTIME(lastJoined * 0.001) AS LastLogin"
                    + " FROM " + DEFAULT_TABLE_NAME);
            return importedRows;
        } finally {
            closeQuietly(stmt);
            closeQuietly(con);
        }
    }
}
