package com.hoccer.xo.android.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import com.hoccer.talk.client.IXoClientDatabaseBackend;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.model.TalkGroupMembership;
import com.hoccer.talk.model.TalkGroupPresence;
import com.hoccer.talk.model.TalkPresence;
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

    private static final int DATABASE_VERSION = 28;

    public static final String DATABASE_NAME_DEFAULT = "hoccer-talk.db";

    private static AndroidTalkDatabase sInstance;

    private final Context mContext;

    public static AndroidTalkDatabase getInstance(Context applicationContext) {
        if (sInstance == null) {
            sInstance = new AndroidTalkDatabase(applicationContext);
        }
        return sInstance;
    }

    private AndroidTalkDatabase(Context context) {
        super(context, PreferenceManager.getDefaultSharedPreferences(context).getString("preference_database", DATABASE_NAME_DEFAULT), null, DATABASE_VERSION);
        mContext = context;
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
                makeTransferDataFileRelative(db);
                removeContentUriFromClientUploadDataFileColumn(db);
                replaceFileUriFromImageUploadContentUrlColumn(mContext, db);
            }

            if (oldVersion < 25) {
                db.execSQL("ALTER TABLE 'clientUpload' ADD COLUMN 'tempCompressedDataFile' TEXT");
            }

            if (oldVersion < 26) {
                db.execSQL("ALTER TABLE 'presence' ADD COLUMN 'isKept' SMALLINT");
                db.execSQL("ALTER TABLE 'presence' ADD COLUMN 'isNearbyAcquaintance' SMALLINT");
                db.execSQL("ALTER TABLE 'groupPresence' ADD COLUMN 'isKept' SMALLINT");
            }

            if (oldVersion < 27) {
                db.execSQL("ALTER TABLE 'groupMembership' ADD COLUMN 'notificationPreference' VARCHAR");
                db.execSQL("ALTER TABLE 'relationship' ADD COLUMN 'notificationPreference' VARCHAR");
                db.execSQL("ALTER TABLE 'clientContact' ADD COLUMN 'worldwide' SMALLINT");
                updateAcquaintanceTypeColumn(db);
            }

            if (oldVersion < 28) {
                updateDeliveryDirection(db);
            }
        } catch (android.database.SQLException e) {
            LOG.error("Android SQL error upgrading database", e);
        } catch (SQLException e) {
            LOG.error("OrmLite SQL error upgrading database", e);
        }
    }

    private void updateDeliveryDirection(SQLiteDatabase db) throws SQLException {
        db.execSQL("ALTER TABLE 'clientMessage' ADD COLUMN 'direction' VARCHAR");
        db.execSQL("ALTER TABLE 'clientMessage' ADD COLUMN 'delivery_id' INTEGER");

        Cursor cursor = db.rawQuery("SELECT * FROM clientMessage", null);
        while (cursor.moveToNext()) {
            int deliveryId = cursor.getInt(cursor.getColumnIndex("incomingDelivery_id"));
            if (deliveryId == 0) {
                deliveryId = cursor.getInt(cursor.getColumnIndex("outgoingDelivery_id"));
            }

            db.execSQL("UPDATE clientMessage SET delivery_id = '" + deliveryId + "' WHERE incomingDelivery_id = '" + deliveryId + "' OR outgoingDelivery_id = '" + deliveryId + "'");
            db.execSQL("UPDATE clientMessage SET direction = '" + TalkClientMessage.TYPE_INCOMING + "' WHERE incomingDelivery_id = '" + deliveryId + "'");
            db.execSQL("UPDATE clientMessage SET direction = '" + TalkClientMessage.TYPE_OUTGOING + "' WHERE outgoingDelivery_id = '" + deliveryId + "'");
        }
    }

    private void updateAcquaintanceTypeColumn(SQLiteDatabase db) throws SQLException {
        db.execSQL("ALTER TABLE 'presence' ADD COLUMN 'acquaintanceType' VARCHAR");

        Cursor cursor = db.rawQuery("SELECT * FROM presence WHERE isNearbyAcquaintance = '1'", null);
        while (cursor.moveToNext()) {
            db.execSQL("UPDATE presence SET acquaintanceType = '" + TalkPresence.TYPE_ACQUAINTANCE_NEARBY + "'");
        }

        db.execSQL("ALTER TABLE 'presence' DROP COLUMN 'isNearbyAcquaintance' SMALLINT");
    }

    private static void makeTransferDataFileRelative(SQLiteDatabase db) {
        updateTransferDataFile(db, "clientUpload", UriUtils.FILE_URI_PREFIX + XoApplication.getExternalStorage().getAbsolutePath() + "/");
        updateTransferDataFile(db, "clientDownload", XoApplication.getExternalStorage().getAbsolutePath() + "/");
    }

    private static void updateTransferDataFile(SQLiteDatabase db, String table, String prefixToRemove) {
        int begin = prefixToRemove.length() + 1;
        String pattern = prefixToRemove + "%";
        db.execSQL("UPDATE " + table + " SET dataFile = substr(dataFile, " + begin + ") WHERE dataFile LIKE '" + pattern + "'");
    }

    private static void removeContentUriFromClientUploadDataFileColumn(SQLiteDatabase db) {
        db.execSQL("UPDATE clientUpload SET dataFile = null WHERE dataFile LIKE 'content://%'");
    }

    private static void replaceFileUriFromImageUploadContentUrlColumn(Context context, SQLiteDatabase db) {
        Cursor cursor = db.rawQuery("SELECT dataFile FROM clientUpload WHERE contentUrl LIKE 'file:///%' AND mediaType = 'image'", null);
        while (cursor.moveToNext()) {
            String dataFile = cursor.getString(cursor.getColumnIndex("dataFile"));
            Uri contentUri = UriUtils.getContentUriByDataPath(context, MediaStore.Images.Media.getContentUri("external"), UriUtils.getAbsoluteFileUri(dataFile).getPath());
            if (contentUri != null) {
                db.execSQL("UPDATE clientUpload SET contentUrl = '" + contentUri + "' WHERE dataFile = '" + dataFile + "'");
            } else {
                db.execSQL("UPDATE clientUpload SET contentUrl = null WHERE dataFile = '" + dataFile + "'");
            }
        }
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
