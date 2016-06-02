package com.github.games647.fastlogin.core.importer;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

public class ElDziAuthImporter extends Importer {

    private static final String TABLE_NAME = "accounts";

    @Override
    public int importData(DataSource source, DataSource target, String targetTable) throws SQLException {
        Connection con = null;
        Statement stmt = null;
        try {
            con = source.getConnection();
            stmt = con.createStatement();
            int importedRows = stmt.executeUpdate("INSERT INTO " + targetTable + " SELECT"
                    + " nick AS Name,"
                    + " uuid AS UUID,"
                    + " premium AS Premium,"
                    + " lastIp AS LastIp,"
                    + " FROM_UNIXTIME(lastPlayed * 0.001) AS LastLogin"
                    + " FROM " + TABLE_NAME);
            return importedRows;
        } finally {
            closeQuietly(stmt);
            closeQuietly(con);
        }
    }
}
