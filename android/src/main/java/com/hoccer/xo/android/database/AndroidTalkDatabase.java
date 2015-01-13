package com.hoccer.xo.android.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import com.hoccer.talk.client.IXoClientDatabaseBackend;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.model.TalkGroupMembership;
import com.hoccer.talk.model.TalkGroupPresence;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.util.UriUtils;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.support.ConnectionSource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

public class AndroidTalkDatabase extends OrmLiteSqliteOpenHelper implements IXoClientDatabaseBackend {

    private static final Logger LOG = Logger.getLogger(AndroidTalkDatabase.class);

    private static final int DATABASE_VERSION = 24;

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
                db.execSQL("ALTER TABLE 'group' RENAME TO 'groupPresence'");
                db.execSQL("ALTER TABLE 'groupMember' RENAME TO 'groupMembership'");
            }

            if (oldVersion < 23) {
                deleteDuplicateGroupContacts();

                Collection<String> deletedGroupIds = getDeletedGroupIds();
                deleteGroupMembershipsForDeletedGroups(deletedGroupIds);
                deleteGroupContactsForDeletedGroups(deletedGroupIds);
                deleteGroupPresencesForDeletedGroups(deletedGroupIds);
            }

            if (oldVersion < 24) {
                updateUploadDataFile(db);
                updateDownloadDataFile(db);
            }

        } catch (android.database.SQLException e) {
            LOG.error("Android SQL error upgrading database", e);
        } catch (SQLException e) {
            LOG.error("OrmLite SQL error upgrading database", e);
        }
    }

    private static void updateUploadDataFile(SQLiteDatabase db) {
        updateTransferDataFile(db, "clientUpload", UriUtils.FILE_URI_PREFIX + XoApplication.getExternalStorage().getAbsolutePath() + "/");
    }

    private static void updateDownloadDataFile(SQLiteDatabase db) throws SQLException {
        updateTransferDataFile(db, "clientDownload", XoApplication.getExternalStorage().getAbsolutePath() + "/");
    }

    private static void updateTransferDataFile(SQLiteDatabase db, String table, String prefixToRemove) {
        int begin = prefixToRemove.length() + 1;
        String pattern = prefixToRemove + "%";
        db.execSQL("UPDATE " + table + " SET dataFile = substr(dataFile, " + begin + ") WHERE dataFile LIKE '" + pattern + "'");
    }

    private void deleteDuplicateGroupContacts() throws SQLException {
        Dao<TalkClientContact, ?> contacts = getDao(TalkClientContact.class);
        DeleteBuilder<TalkClientContact, ?> deleteContacts = contacts.deleteBuilder();

        deleteContacts.where()
                .eq("contactType", TalkClientContact.TYPE_GROUP)
                .and()
                .isNull("groupTag")
                .and()
                .isNull("groupPresence_id");

        deleteContacts.delete();
    }

    private Collection<String> getDeletedGroupIds() throws SQLException {
        Dao<TalkGroupPresence, ?> groupPresences = getDao(TalkGroupPresence.class);
        List<TalkGroupPresence> deletedGroupPresences = groupPresences.queryForEq("state", TalkGroupPresence.STATE_DELETED);

        return CollectionUtils.collect(deletedGroupPresences, new Transformer<TalkGroupPresence, String>() {
            @Override
            public String transform(TalkGroupPresence groupPresence) {
                return groupPresence.getGroupId();
            }
        });
    }

    private void deleteGroupMembershipsForDeletedGroups(Collection<String> deletedGroupIds) throws SQLException {
        Dao<TalkGroupMembership, ?> memberships = getDao(TalkGroupMembership.class);
        DeleteBuilder<TalkGroupMembership, ?> deleteMemberships = memberships.deleteBuilder();
        deleteMemberships.where().in("groupId", deletedGroupIds);
        deleteMemberships.delete();
    }

    private void deleteGroupContactsForDeletedGroups(Collection<String> deletedGroupIds) throws SQLException {
        Dao<TalkClientContact, ?> contacts = getDao(TalkClientContact.class);
        DeleteBuilder<TalkClientContact, ?> deleteContacts = contacts.deleteBuilder();
        deleteContacts.where().in("groupId", deletedGroupIds);
        deleteContacts.delete();
    }

    private void deleteGroupPresencesForDeletedGroups(Collection<String> deletedGroupIds) throws SQLException {
        Dao<TalkGroupPresence, ?> groupPresences = getDao(TalkGroupPresence.class);
        DeleteBuilder<TalkGroupPresence, ?> deleteGroupPresences = groupPresences.deleteBuilder();
        deleteGroupPresences.where().in("groupId", deletedGroupIds);
        deleteGroupPresences.delete();
    }
}
