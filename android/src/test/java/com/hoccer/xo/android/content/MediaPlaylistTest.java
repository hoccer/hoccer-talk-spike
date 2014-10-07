package com.hoccer.xo.android.content;

import com.hoccer.talk.client.IXoClientDatabaseBackend;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.XoTransfer;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientMediaCollection;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.talk.content.IContentObject;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.sql.SQLException;

import static junit.framework.TestCase.*;

public class MediaPlaylistTest {

    private static final Logger LOG = Logger.getLogger(MediaPlaylistTest.class);

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
        for (int i = 0; i < collection.size(); i++) {
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

        final int expectedFromIndex = 0;
        final int expectedToIndex = 1;

        // register Playlist listener
        com.hoccer.xo.android.content.MediaPlaylist.Listener listener = new com.hoccer.xo.android.content.MediaPlaylist.Listener() {
            @Override
            public void onItemOrderChanged(com.hoccer.xo.android.content.MediaPlaylist playlist) {
                onItemOrderChangedCalled.value = true;
            }

            @Override
            public void onItemRemoved(com.hoccer.xo.android.content.MediaPlaylist playlist, IContentObject itemRemoved) {
                assertEquals(expectedItemRemoved, itemRemoved);
                onItemRemovedCalled.value = true;
            }

            @Override
            public void onItemAdded(com.hoccer.xo.android.content.MediaPlaylist playlist, IContentObject itemAdded) {
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

        TalkClientContact user1 = new TalkClientContact(TalkClientContact.TYPE_CLIENT);
        TalkClientContact user2 = new TalkClientContact(TalkClientContact.TYPE_CLIENT);
        try {
            mDatabase.saveContact(user1);
            mDatabase.saveContact(user2);
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            fail();
        }

        // create downloads for user1
        int expectedItemCount1 = 3;
        for (int i = 0; i < expectedItemCount1; i++) {
            createAudioDownloadWithUser(user1);
        }

        // create downloads for user2
        int expectedItemCount2 = 4;
        for (int i = 0; i < expectedItemCount2; i++) {
            createAudioDownloadWithUser(user2);
        }

        // create user1 playlist
        UserPlaylist playlist1 = new UserPlaylist(mDatabase, user1);
        assertEquals(expectedItemCount1, playlist1.size());

        // create user2 playlist
        UserPlaylist playlist2 = new UserPlaylist(mDatabase, user2);
        assertEquals(expectedItemCount2, playlist2.size());

        // create unfiltered playlist
        UserPlaylist playlist3 = new UserPlaylist(mDatabase, null);
        assertEquals(expectedItemCount1 + expectedItemCount2, playlist3.size());

        // set listener
        final ValueContainer<Boolean> onItemOrderChangedCalled = new ValueContainer<Boolean>(false);
        final ValueContainer<Boolean> onItemRemovedCalled = new ValueContainer<Boolean>(false);
        final ValueContainer<Boolean> onItemAddedCalled = new ValueContainer<Boolean>(false);
        final ValueContainer<Boolean> onPlaylistClearedCalled = new ValueContainer<Boolean>(false);

        // register Playlist listener
        com.hoccer.xo.android.content.MediaPlaylist.Listener listener = new com.hoccer.xo.android.content.MediaPlaylist.Listener() {
            @Override
            public void onItemOrderChanged(com.hoccer.xo.android.content.MediaPlaylist playlist) {
                onItemOrderChangedCalled.value = true;
            }

            @Override
            public void onItemRemoved(com.hoccer.xo.android.content.MediaPlaylist playlist, IContentObject itemRemoved) {
                onItemRemovedCalled.value = true;
            }

            @Override
            public void onItemAdded(com.hoccer.xo.android.content.MediaPlaylist playlist, IContentObject itemAdded) {
                onItemAddedCalled.value = true;
            }

            @Override
            public void onPlaylistCleared(com.hoccer.xo.android.content.MediaPlaylist playlist) {
                onPlaylistClearedCalled.value = true;
            }
        };
        playlist1.registerListener(listener);

        // add download to user1
        createAudioDownloadWithUser(user1);

        assertFalse(onItemOrderChangedCalled.value);
        assertFalse(onItemRemovedCalled.value);
        assertTrue(onItemAddedCalled.value);
        assertFalse(onPlaylistClearedCalled.value);

        assertEquals(expectedItemCount1 + 1, playlist1.size());
        assertEquals(expectedItemCount1 + expectedItemCount2 + 1, playlist3.size());

        onItemOrderChangedCalled.value = false;
        onItemRemovedCalled.value = false;
        onItemAddedCalled.value = false;
        onPlaylistClearedCalled.value = false;

        // delete download of user1
        try {
            mDatabase.deleteTransferAndMessage(playlist1.getItem(1));
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            fail();
        }

        assertFalse(onItemOrderChangedCalled.value);
        assertTrue(onItemRemovedCalled.value);
        assertFalse(onItemAddedCalled.value);
        assertFalse(onPlaylistClearedCalled.value);

        assertEquals(expectedItemCount1, playlist1.size());
        assertEquals(expectedItemCount1 + expectedItemCount2, playlist3.size());
    }

    @Test
    public void testSingleItemPlaylist() {
        LOG.info("testSingleItemPlaylist");

        TalkClientContact user = new TalkClientContact(TalkClientContact.TYPE_CLIENT);
        TalkClientDownload item = createAudioDownloadWithUser(user);
        TalkClientDownload otherItem = createAudioDownloadWithUser(user);

        try {
            mDatabase.saveClientDownload(item);
            mDatabase.saveClientDownload(otherItem);
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            fail();
        }

        // create single item playlist
        SingleItemPlaylist playlist = new SingleItemPlaylist(mDatabase, item);

        assertEquals(1, playlist.size());

        XoTransfer expectedItem = item;
        IContentObject actualItem = playlist.getItem(0);
        assertTrue(expectedItem.equals(actualItem));

        // test iterator
        int expectedItemCount = 1;
        int actualItemCount = 0;
        for (IContentObject playlistItem : playlist) {
            actualItemCount++;
            assertTrue(expectedItem.equals(playlistItem));
        }
        assertEquals(expectedItemCount, actualItemCount);

        // test indexOf
        assertEquals(0, playlist.indexOf(item));
        assertEquals(-1, playlist.indexOf(otherItem));

        // test hasItem
        assertTrue(playlist.hasItem(item));
        assertFalse(playlist.hasItem(otherItem));

        // set listener
        final ValueContainer<Boolean> onItemOrderChangedCalled = new ValueContainer<Boolean>(false);
        final ValueContainer<Boolean> onItemRemovedCalled = new ValueContainer<Boolean>(false);
        final ValueContainer<Boolean> onItemAddedCalled = new ValueContainer<Boolean>(false);
        final ValueContainer<Boolean> onPlaylistClearedCalled = new ValueContainer<Boolean>(false);

        // register Playlist listener
        com.hoccer.xo.android.content.MediaPlaylist.Listener listener = new com.hoccer.xo.android.content.MediaPlaylist.Listener() {
            @Override
            public void onItemOrderChanged(com.hoccer.xo.android.content.MediaPlaylist playlist) {
                onItemOrderChangedCalled.value = true;
            }

            @Override
            public void onItemRemoved(com.hoccer.xo.android.content.MediaPlaylist playlist, IContentObject itemRemoved) {
                onItemRemovedCalled.value = true;
            }

            @Override
            public void onItemAdded(com.hoccer.xo.android.content.MediaPlaylist playlist, IContentObject itemAdded) {
                onItemAddedCalled.value = true;
            }

            @Override
            public void onPlaylistCleared(com.hoccer.xo.android.content.MediaPlaylist playlist) {
                onPlaylistClearedCalled.value = true;
            }
        };
        playlist.registerListener(listener);

        // remove other item (should not bother playlist)
        try {
            mDatabase.deleteTransferAndMessage(otherItem);
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            fail();
        }

        assertFalse(onItemOrderChangedCalled.value);
        assertFalse(onItemRemovedCalled.value);
        assertFalse(onItemAddedCalled.value);
        assertFalse(onPlaylistClearedCalled.value);

        // remove item
        try {
            mDatabase.deleteTransferAndMessage(item);
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            fail();
        }

        assertFalse(onItemOrderChangedCalled.value);
        assertTrue(onItemRemovedCalled.value);
        assertFalse(onItemAddedCalled.value);
        assertFalse(onPlaylistClearedCalled.value);
    }

    @Test
    public void testEmptyPlaylist() {
        LOG.info("testEmptyPlaylist");

        // create empty playlist
        EmptyPlaylist playlist = new EmptyPlaylist();

        assertEquals(0, playlist.size());

        // test iterator
        for (IContentObject item : playlist) {
            fail();
        }
    }

    //////// Helpers ////////

    private TalkClientDownload createAudioDownloadWithUser(TalkClientContact user) {
        // create download
        TalkClientDownload result = new TalkClientDownload();

        // set private fields via reflection to avoid a dozen of objects which usually make up the download
        try {
            Class<?> downloadClass = result.getClass();
            Field mediaTypeField = downloadClass.getDeclaredField("mediaType");
            mediaTypeField.setAccessible(true);
            mediaTypeField.set(result, ContentMediaType.AUDIO);

            // save first to set valid downloadId
            mDatabase.saveClientDownload(result);

            // create message for user and link download
            TalkClientMessage message = new TalkClientMessage();
            message.setAttachmentDownload(result);
            message.setSenderContact(user);
            message.setConversationContact(user);

            mDatabase.saveClientMessage(message);

            Field stateField = downloadClass.getDeclaredField("state");
            stateField.setAccessible(true);
            stateField.set(result, TalkClientDownload.State.COMPLETE);

            // resave download with COMPLETE state
            mDatabase.saveClientDownload(result);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            fail();
        }

        return result;
    }

    private class ValueContainer<T> {
        public T value;

        public ValueContainer(T initValue) {
            value = initValue;
        }
    }
}
