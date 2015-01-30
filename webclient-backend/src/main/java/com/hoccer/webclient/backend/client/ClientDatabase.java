package com.hoccer.webclient.backend.client;

import com.hoccer.talk.client.IXoClientDatabaseBackend;
import com.hoccer.talk.client.XoClientDatabase;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import org.apache.log4j.Logger;

import java.sql.SQLException;

/**
 * Implements the client side h2 database.
 */
public class ClientDatabase implements IXoClientDatabaseBackend {

    private static final Logger LOG = Logger.getLogger(ClientDatabase.class);

    ConnectionSource mConnectionSource;

    public ClientDatabase(String fileName) {
        ConnectionSourceCreationResult result = createConnectionSource(fileName);
        mConnectionSource = result.connectionSource;

        if (!result.existed) {
            initializeDb(mConnectionSource);
        }
    }

    @Override
    public ConnectionSource getConnectionSource() {
        return mConnectionSource;
    }

    class ConnectionSourceCreationResult {
        public ConnectionSource connectionSource;
        public boolean existed;
    }

    private ConnectionSourceCreationResult createConnectionSource(String fileName) {
        ConnectionSourceCreationResult result = new ConnectionSourceCreationResult();

        result.connectionSource = createConnectionSource(fileName, true);
        result.existed = true;

        try {
            result.connectionSource.getReadOnlyConnection();
        } catch (SQLException e) {
            result.connectionSource = createConnectionSource(fileName, false);
            result.existed = false;
        }

        return result;
    }

    private ConnectionSource createConnectionSource(String fileName, boolean mustExist) {
        String url = "jdbc:h2:file:" + fileName;

        if (mustExist) {
            url += ";IFEXISTS=TRUE";
        }

        try {
            return new JdbcConnectionSource(url);
        } catch (SQLException e) {
            LOG.error("Error creating JdbcConnectionSource for H2 database", e);
            return null;
        }
    }

    private void initializeDb(ConnectionSource connectionSource) {
        try {
            XoClientDatabase.createTables(connectionSource);
        } catch (SQLException e) {
            LOG.error("Error while creating tables in client database'", e);
        }
    }

    @Override
    public <D extends Dao<T, ?>, T> D getDao(Class<T> clazz) throws SQLException {
        LOG.debug("Creating dao for " + clazz.getSimpleName());
        return DaoManager.createDao(getConnectionSource(), clazz);
    }
}
