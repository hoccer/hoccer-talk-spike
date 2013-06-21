package com.hoccer.talk.android.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import com.hoccer.talk.client.ITalkClientDatabaseBackend;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientSelf;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.model.*;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import org.apache.log4j.Logger;

import java.sql.SQLException;

public class AndroidTalkDatabase extends OrmLiteSqliteOpenHelper implements ITalkClientDatabaseBackend {

    private static final Logger LOG = Logger.getLogger(AndroidTalkDatabase.class);

    private static final String DATABASE_NAME    = "hoccer-talk.db";

    private static final int    DATABASE_VERSION = 2;

    private static AndroidTalkDatabase INSTANCE = null;

    public static AndroidTalkDatabase getInstance(Context applicationContext) {
        if(INSTANCE == null) {
            INSTANCE = new AndroidTalkDatabase(applicationContext);
        }
        return INSTANCE;
    }

    Dao<TalkClient, String> mClientDao;
    Dao<TalkMessage, String> mMessageDao;
    Dao<TalkDelivery, String> mDeliveryDao;

    private AndroidTalkDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db, ConnectionSource cs) {
        LOG.info("creating database at schema version " + DATABASE_VERSION);
        try {
            TableUtils.createTable(cs, TalkClientContact.class);
            TableUtils.createTable(cs, TalkClientSelf.class);
            TableUtils.createTable(cs, TalkPresence.class);
            TableUtils.createTable(cs, TalkRelationship.class);
            TableUtils.createTable(cs, TalkGroup.class);
            TableUtils.createTable(cs, TalkGroupMember.class);

            TableUtils.createTable(cs, TalkClientMessage.class);
            TableUtils.createTable(cs, TalkMessage.class);
            TableUtils.createTable(cs, TalkDelivery.class);
        } catch (SQLException e) {
            e.printStackTrace();
            // XXX app must fail or something
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource cs, int oldVersion, int newVersion) {
        LOG.info("upgrading database from schema version "
                + oldVersion + " to schema version " + newVersion);
        try {
            if(oldVersion < 2) {
                TableUtils.createTable(cs, TalkGroup.class);
                TableUtils.createTable(cs, TalkGroupMember.class);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // XXX app must fail or something
        }
    }

}
