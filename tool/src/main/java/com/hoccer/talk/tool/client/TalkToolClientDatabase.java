package com.hoccer.talk.tool.client;

import com.hoccer.talk.client.IXoClientDatabaseBackend;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.model.*;
import com.hoccer.talk.model.*;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import org.apache.log4j.Logger;

import java.sql.SQLException;

public class TalkToolClientDatabase implements IXoClientDatabaseBackend {

    Logger LOG = Logger.getLogger(TalkToolClientDatabase.class);
    TalkToolClient mClient;
    JdbcConnectionSource mCs;

    public TalkToolClientDatabase(TalkToolClient client) {
        mClient = client;
    }

    public void initializeDb() throws SQLException {
        ConnectionSource cs = getConnectionSource();
        XoClientDatabase.createTables(cs);
    }

    @Override
    public ConnectionSource getConnectionSource() {
        if (mCs == null) {
            String url;
            if (mClient.getContext().getApplication().isDbModeFile()) {
                url = "jdbc:h2:file:talk-client-" + mClient.getId();
            } else {
                url = "jdbc:h2:mem:talk-client-" + mClient.getId();
            }
            LOG.info("Creating connection source for '" + url + "'");

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
        LOG.debug("Creating dao for " + clazz.getSimpleName());
        return DaoManager.createDao(getConnectionSource(), clazz);
    }

}
