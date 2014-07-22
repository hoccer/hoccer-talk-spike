import com.hoccer.talk.client.IXoClientDatabaseBackend;
import com.hoccer.talk.client.IXoMediaCollectionListener;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientMediaCollection;
import com.hoccer.talk.client.model.TalkClientMediaCollectionRelation;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static junit.framework. TestCase.assertTrue;
import static junit.framework. TestCase.assertFalse;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.fail;

public class MediaPlaylistTest2 {

    private static final Logger LOG = Logger.getLogger(MediaPlaylistTest2.class);

    private XoClientDatabase mDatabase;

    private JdbcConnectionSource mConnectionSource;

    @Before
    public void testSetup() throws Exception {
        mConnectionSource = new JdbcConnectionSource("jdbc:h2:mem:account");
        mDatabase = new XoClientDatabase(new IXoClientDatabaseBackend() {

            @Override
            public ConnectionSource getConnectionSource() {
                return mConnectionSource;
            }

            @Override
            public <D extends Dao<T, ?>, T> D getDao(Class<T> clazz) throws SQLException {
                D dao = DaoManager.createDao(mConnectionSource, clazz);
                dao.setObjectCache(true);
                return dao;
            }
        });

        mDatabase.createTables(mConnectionSource);
        mDatabase.initialize();
    }

    @After
    public void testCleanup() throws SQLException {
        mConnectionSource.close();
    }

    @Test
    public void testMediaCollectionPlaylist() {
        LOG.info("testMediaCollectionPlaylist");

        // create MediaCollection
        String collectionName = "testMediaCollectionPlaylist_collection";

        TalkClientMediaCollection collection = null;
        TalkClientDownload item0 = new TalkClientDownload();
        TalkClientDownload item1 = new TalkClientDownload();
        TalkClientDownload item2 = new TalkClientDownload();
        try {
            collection = mDatabase.createMediaCollection(collectionName);

            // create some items and addItem to collection
            mDatabase.saveClientDownload(item0);
            mDatabase.saveClientDownload(item1);
            mDatabase.saveClientDownload(item2);
            collection.addItem(item0);
            collection.addItem(item1);
            collection.addItem(item2);
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            fail();
        }

        // create MediaCollectionPlaylist

        // add item

        // remove item

        // delete MediaCollection
    }

    @Test
    public void testUserPlaylist() {
        LOG.info("testUserPlaylist");

        // create user playlist

        // add item

        // remove item
    }

    @Test
    public void testSingleTrackPlaylist() {
        LOG.info("testSingleTrackPlaylist");

        // create single track playlist

        // add item

        // remove item
    }
}
