package com.hoccer.xo.android.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import com.hoccer.talk.client.IXoClientDatabaseBackend;
import com.hoccer.talk.client.XoClientDatabase;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import org.apache.log4j.Logger;

import java.sql.SQLException;

public class AndroidTalkDatabase extends OrmLiteSqliteOpenHelper implements IXoClientDatabaseBackend {

    private static final Logger LOG = Logger.getLogger(AndroidTalkDatabase.class);

    private static final int DATABASE_VERSION = 22;

    private static final String DATABASE_NAME_DEFAULT = "hoccer-talk.db";

    private static AndroidTalkDatabase sInstance;

    public static AndroidTalkDatabase getInstance(Context applicationContext) {
        if (sInstance == null) {
            sInstance = new AndroidTalkDatabase(applicationContext);
        }
        return sInstance;
    }

    private AndroidTalkDatabase(Context context) {
        super(context, PreferenceManager.getDefaultSharedPreferences(context).getString("preference_database", DATABASE_NAME_DEFAULT), null, DATABASE_VERSION);
    }

    @Override
    public <D extends Dao<T, ?>, T> D getDao(Class<T> clazz) throws SQLException {
        D dao = super.getDao(clazz);
        dao.setObjectCache(true);
        return dao;
    }

    @Override
    public void onCreate(SQLiteDatabase db, ConnectionSource cs) {
        LOG.info("creating database at schema version " + DATABASE_VERSION);
        try {
            XoClientDatabase.createTables(cs);
        } catch (SQLException e) {
            LOG.error("sql error creating database", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource cs, int oldVersion, int newVersion) {
        LOG.info("upgrading database from schema version " + oldVersion + " to schema version " + newVersion);
        try {
            // the first database version for "Hoccer" is 21, earlier migrations have been removed
            if (oldVersion < 22) {
                db.execSQL("DROP TABLE clientSmsToken");
                db.execSQL("DROP TABLE clientMembership");
            }
        } catch (android.database.SQLException e) {
            LOG.error("sql error upgrading database", e);
        }
    }
}
