package com.hoccer.talk.tool.client;

import com.hoccer.talk.client.IXoClientDatabaseBackend;
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

        TableUtils.createTableIfNotExists(cs, TalkClientContact.class);
        TableUtils.createTableIfNotExists(cs, TalkClientSelf.class);
        TableUtils.createTableIfNotExists(cs, TalkPresence.class);
        TableUtils.createTableIfNotExists(cs, TalkRelationship.class);
        TableUtils.createTableIfNotExists(cs, TalkGroup.class);
        TableUtils.createTableIfNotExists(cs, TalkGroupMember.class);

        TableUtils.createTableIfNotExists(cs, TalkClientMembership.class);

        TableUtils.createTableIfNotExists(cs, TalkClientMessage.class);
        TableUtils.createTableIfNotExists(cs, TalkMessage.class);
        TableUtils.createTableIfNotExists(cs, TalkDelivery.class);

        TableUtils.createTableIfNotExists(cs, TalkKey.class);
        TableUtils.createTableIfNotExists(cs, TalkPrivateKey.class);

        TableUtils.createTableIfNotExists(cs, TalkClientDownload.class);
        TableUtils.createTableIfNotExists(cs, TalkClientUpload.class);

        TableUtils.createTableIfNotExists(cs, TalkClientSmsToken.class);
    }

    @Override
    public ConnectionSource getConnectionSource() {
        if (mCs == null) {
            String url = "jdbc:sqlite:talk-client-" + mClient.getId() + ".db";
            LOG.debug("Creating connsource for " + url);

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
