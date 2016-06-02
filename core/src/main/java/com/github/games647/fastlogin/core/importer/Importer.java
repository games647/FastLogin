package com.github.games647.fastlogin.core.importer;

import java.sql.SQLException;

import javax.sql.DataSource;

public abstract class Importer {

    public abstract int importData(DataSource source, DataSource target, String targetTable) throws SQLException;

    protected void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignore) {
                //ignore
            }
        }
    }
}
