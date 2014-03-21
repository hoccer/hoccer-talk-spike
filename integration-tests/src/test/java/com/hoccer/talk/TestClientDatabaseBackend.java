package com.hoccer.talk;

import com.hoccer.talk.client.IXoClientDatabaseBackend;
import com.hoccer.talk.client.XoClientDatabase;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import org.junit.Assert;


import java.sql.SQLException;

public class TestClientDatabaseBackend implements IXoClientDatabaseBackend {
    private JdbcConnectionSource mCs;

    public TestClientDatabaseBackend() {
        try {
            XoClientDatabase.createTables(getConnectionSource());
        } catch (SQLException e) {
            Assert.fail(e.toString());
        }
    }

    @Override
    public ConnectionSource getConnectionSource() {
        if (mCs == null) {
            String url = "jdbc:h2:mem:";
            try {
                mCs = new JdbcConnectionSource(url);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return mCs;
    }

    @Override
    public <D extends Dao<T, ?>, T> D getDao(Class<T> clazz) throws SQLException {
        return DaoManager.createDao(getConnectionSource(), clazz);
    }
}
