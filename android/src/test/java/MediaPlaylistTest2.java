import com.hoccer.talk.client.IXoClientDatabaseBackend;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientMediaCollection;
import com.hoccer.xo.android.content.MediaCollectionPlaylist;
import com.hoccer.xo.android.content.audio.MediaPlaylist;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;

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
        TalkClientDownload item3 = new TalkClientDownload();
        try {
            collection = mDatabase.createMediaCollection(collectionName);

            // create some items and addItem to collection
            mDatabase.saveClientDownload(item0);
            mDatabase.saveClientDownload(item1);
            mDatabase.saveClientDownload(item2);
            mDatabase.saveClientDownload(item3);

            collection.addItem(item0);
            collection.addItem(item1);
            collection.addItem(item2);
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            fail();
        }

        // create MediaCollectionPlaylist
        MediaCollectionPlaylist playlist = new MediaCollectionPlaylist(collection);

        assertEquals(collection.size(), playlist.size());
        for(int i = 0; i < collection.size(); i++) {
            assertEquals(collection.getItem(i), playlist.getItem(i));
        }

        // set listener
        final ValueContainer<Boolean> onItemOrderChangedCalled = new ValueContainer<Boolean>(false);
        final ValueContainer<Boolean> onItemRemovedCalled = new ValueContainer<Boolean>(false);
        final ValueContainer<Boolean> onItemAddedCalled = new ValueContainer<Boolean>(false);
        final ValueContainer<Boolean> onPlaylistClearedCalled = new ValueContainer<Boolean>(false);

        final TalkClientDownload expectedItemAdded = item3;
        final int expectedItemAddedIndex = 0;

        final TalkClientDownload expectedItemRemoved = item1;
        final int expectedItemRemovedIndex = 1;

        final int expectedFromIndex = 0;
        final int expectedToIndex = 1;

        // register Playlist listener
        com.hoccer.xo.android.content.MediaPlaylist.Listener listener = new com.hoccer.xo.android.content.MediaPlaylist.Listener() {
            @Override
            public void onItemOrderChanged(com.hoccer.xo.android.content.MediaPlaylist playlist, int fromIndex, int toIndex) {
                assertEquals(expectedFromIndex, fromIndex);
                assertEquals(expectedToIndex, toIndex);
                onItemOrderChangedCalled.value = true;
            }

            @Override
            public void onItemRemoved(com.hoccer.xo.android.content.MediaPlaylist playlist, int indexRemoved, TalkClientDownload itemRemoved) {
                assertEquals(expectedItemRemovedIndex, indexRemoved);
                assertEquals(expectedItemRemoved, itemRemoved);
                onItemRemovedCalled.value = true;
            }

            @Override
            public void onItemAdded(com.hoccer.xo.android.content.MediaPlaylist playlist, int indexAdded, TalkClientDownload itemAdded) {
                assertEquals(expectedItemAddedIndex, indexAdded);
                assertEquals(expectedItemAdded, itemAdded);
                onItemAddedCalled.value = true;
            }

            @Override
            public void onPlaylistCleared(com.hoccer.xo.android.content.MediaPlaylist playlist) {
                onPlaylistClearedCalled.value = true;
            }
        };
        playlist.registerListener(listener);

        // remove item
        collection.removeItem(1);

        assertFalse(onItemOrderChangedCalled.value);
        assertTrue(onItemRemovedCalled.value);
        assertFalse(onItemAddedCalled.value);
        assertFalse(onPlaylistClearedCalled.value);

        onItemOrderChangedCalled.value = false;
        onItemRemovedCalled.value = false;
        onItemAddedCalled.value = false;
        onPlaylistClearedCalled.value = false;

        // change item order
        collection.reorderItemIndex(expectedFromIndex, expectedToIndex);

        assertTrue(onItemOrderChangedCalled.value);
        assertFalse(onItemRemovedCalled.value);
        assertFalse(onItemAddedCalled.value);
        assertFalse(onPlaylistClearedCalled.value);

        onItemOrderChangedCalled.value = false;
        onItemRemovedCalled.value = false;
        onItemAddedCalled.value = false;
        onPlaylistClearedCalled.value = false;

        // clear collection
        collection.clear();

        assertFalse(onItemOrderChangedCalled.value);
        assertFalse(onItemRemovedCalled.value);
        assertFalse(onItemAddedCalled.value);
        assertTrue(onPlaylistClearedCalled.value);

        onItemOrderChangedCalled.value = false;
        onItemRemovedCalled.value = false;
        onItemAddedCalled.value = false;
        onPlaylistClearedCalled.value = false;

        // add item
        collection.addItem(expectedItemAddedIndex, expectedItemAdded);

        assertFalse(onItemOrderChangedCalled.value);
        assertFalse(onItemRemovedCalled.value);
        assertTrue(onItemAddedCalled.value);
        assertFalse(onPlaylistClearedCalled.value);

        onItemOrderChangedCalled.value = false;
        onItemRemovedCalled.value = false;
        onItemAddedCalled.value = false;
        onPlaylistClearedCalled.value = false;

        // delete collection
        try {
            mDatabase.deleteMediaCollection(collection);
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            fail();
        }

        assertFalse(onItemOrderChangedCalled.value);
        assertFalse(onItemRemovedCalled.value);
        assertFalse(onItemAddedCalled.value);
        assertTrue(onPlaylistClearedCalled.value);
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

    private class ValueContainer<T> {
        public T value;
        public ValueContainer(T initValue) {
            value = initValue;
        }
    }
}
