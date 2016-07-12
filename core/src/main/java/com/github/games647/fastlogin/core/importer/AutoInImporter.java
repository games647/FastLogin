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
            int importedRows = stmt.executeUpdate("INSERT INTO " + targetTable + " (Name, Premium, LastIp, UUID) SELECT"
                    + " name AS Name,"
                    /* Enable premium authentication only for those who want to be auto logged in, so
                    they have their cracked protection disabled */
                    + " !protection AND premium AS Premium,"
                    + " '' AS LastIp,"
                    /* Remove the dashes - returns null if puuid is null too */
                    + " REPLACE(puuid, '-', '') AS UUID"
                    + " FROM " + USER_TABLE
                    /* Get the premium uuid */
                    + " LEFT JOIN " + " ("
                    /* Prevent duplicates */
                    + "SELECT * FROM " + UUID_TABLE + " GROUP BY nickname_id"
                    + ") uuids"
                    + " ON " + USER_TABLE + ".id = uuids.nickname_id");

            /* FastLogin will also make lookups on the uuid column for name changes
            the old 1.6.2 version won't check if those user have premium enabled

            so it could happen that a premium could steal the account if we don't do this

            It seems the uuid is saved on autoin too if the player is cracked */
            stmt.executeUpdate("UPDATE `premium` SET `UUID`=NULL WHERE PREMIUM=0");
            return importedRows;
        } finally {
            closeQuietly(stmt);
            closeQuietly(con);
        }
    }
}
