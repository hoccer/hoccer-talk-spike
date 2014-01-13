package com.hoccer.talk.tool.client;

import com.hoccer.talk.client.IXoClientDatabaseBackend;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientMembership;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.client.model.TalkClientSelf;
import com.hoccer.talk.client.model.TalkClientSmsToken;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.model.TalkDelivery;
import com.hoccer.talk.model.TalkGroup;
import com.hoccer.talk.model.TalkGroupMember;
import com.hoccer.talk.model.TalkKey;
import com.hoccer.talk.model.TalkMessage;
import com.hoccer.talk.model.TalkPresence;
import com.hoccer.talk.model.TalkPrivateKey;
import com.hoccer.talk.model.TalkRelationship;
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

        TableUtils.createTable(cs, TalkClientContact.class);
        TableUtils.createTable(cs, TalkClientSelf.class);
        TableUtils.createTable(cs, TalkPresence.class);
        TableUtils.createTable(cs, TalkRelationship.class);
        TableUtils.createTable(cs, TalkGroup.class);
        TableUtils.createTable(cs, TalkGroupMember.class);

        TableUtils.createTable(cs, TalkClientMembership.class);

        TableUtils.createTable(cs, TalkClientMessage.class);
        TableUtils.createTable(cs, TalkMessage.class);
        TableUtils.createTable(cs, TalkDelivery.class);

        TableUtils.createTable(cs, TalkKey.class);
        TableUtils.createTable(cs, TalkPrivateKey.class);

        TableUtils.createTable(cs, TalkClientDownload.class);
        TableUtils.createTable(cs, TalkClientUpload.class);

        TableUtils.createTable(cs, TalkClientSmsToken.class);
    }

    @Override
    public ConnectionSource getConnectionSource() {
        if(mCs == null) {
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
