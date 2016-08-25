package com.github.games647.fastlogin.core.importer;

import com.github.games647.fastlogin.core.AuthStorage;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

public abstract class Importer {

    public abstract int importData(Connection source, DataSource target, AuthStorage storage) throws SQLException;

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
