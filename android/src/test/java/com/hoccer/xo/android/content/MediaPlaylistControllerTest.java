package com.hoccer.xo.android.content;

import com.hoccer.talk.client.IXoClientDatabaseBackend;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.XoTransfer;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientMediaCollection;
import com.hoccer.xo.android.content.audio.MediaPlaylistController;
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

import static junit.framework.TestCase.*;

public class MediaPlaylistControllerTest {

    private static final Logger LOG = Logger.getLogger(MediaPlaylistControllerTest.class);

    private XoClientDatabase mDatabase;

    private JdbcConnectionSource mConnectionSource;

    private MediaPlaylistController mPlaylistController;

    private TalkClientMediaCollection mCollection;

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

        mPlaylistController = new MediaPlaylistController();
        mCollection = createMediaCollection();
        mPlaylistController.setPlaylist(new MediaCollectionPlaylist(mCollection));
    }

    @After
    public void testCleanup() throws SQLException {
        mConnectionSource.close();
    }

    @Test
    public void testRemoveBeforeCurrentItem() {
        mPlaylistController.setCurrentIndex(1);
        XoTransfer expectedCurrentItem = mPlaylistController.getCurrentItem();
        ListenerTester listenerTest = new ListenerTester(mPlaylistController);

        mCollection.removeItem(0);

        int expectedCurrentIndex = 0;
        assertEquals(expectedCurrentIndex, mPlaylistController.getCurrentIndex());
        assertEquals(expectedCurrentItem, mPlaylistController.getCurrentItem());

        // listener test
        assertEquals(0, listenerTest.currentItemChangedCalls.size());
        assertEquals(1, listenerTest.playlistChangedCalled.size());
        assertEquals(0, listenerTest.repeatModeChangedCalled.size());
        assertEquals(0, listenerTest.shuffleChangedCalled.size());
    }

    @Test
    public void testRemoveAtCurrentItem() {
        mPlaylistController.setCurrentIndex(2);
        XoTransfer expectedCurrentItem = mCollection.getItem(3);
        ListenerTester listenerTest = new ListenerTester(mPlaylistController);

        mCollection.removeItem(2);

        int expectedCurrentIndex = 2;
        assertEquals(expectedCurrentIndex, mPlaylistController.getCurrentIndex());
        assertEquals(expectedCurrentItem, mPlaylistController.getCurrentItem());

        // listener test
        assertEquals(1, listenerTest.currentItemChangedCalls.size());
        assertEquals(expectedCurrentItem, listenerTest.currentItemChangedCalls.get(0).args[0]);
        assertEquals(1, listenerTest.playlistChangedCalled.size());
        assertEquals(0, listenerTest.repeatModeChangedCalled.size());
        assertEquals(0, listenerTest.shuffleChangedCalled.size());
    }

    @Test
    public void testRemoveAfterCurrentItem() {
        mPlaylistController.setCurrentIndex(1);
        XoTransfer expectedCurrentItem = mPlaylistController.getCurrentItem();
        ListenerTester listenerTest = new ListenerTester(mPlaylistController);

        mCollection.removeItem(2);

        int expectedCurrentIndex = 1;
        assertEquals(expectedCurrentIndex, mPlaylistController.getCurrentIndex());
        assertEquals(expectedCurrentItem, mPlaylistController.getCurrentItem());

        // listener test
        assertEquals(0, listenerTest.currentItemChangedCalls.size());
        assertEquals(1, listenerTest.playlistChangedCalled.size());
        assertEquals(0, listenerTest.repeatModeChangedCalled.size());
        assertEquals(0, listenerTest.shuffleChangedCalled.size());
    }

    @Test
    public void testAddBeforeCurrentItem() {
        mPlaylistController.setCurrentIndex(2);
        XoTransfer expectedCurrentItem = mPlaylistController.getCurrentItem();
        ListenerTester listenerTest = new ListenerTester(mPlaylistController);

        TalkClientDownload item = null;
        try {
            item = new TalkClientDownload();
            mDatabase.saveClientDownload(item);
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            fail();
        }
        mCollection.addItem(1, item);

        int expectedCurrentIndex = 3;
        assertEquals(expectedCurrentIndex, mPlaylistController.getCurrentIndex());
        assertEquals(expectedCurrentItem, mPlaylistController.getCurrentItem());

        // listener test
        assertEquals(0, listenerTest.currentItemChangedCalls.size());
        assertEquals(1, listenerTest.playlistChangedCalled.size());
        assertEquals(0, listenerTest.repeatModeChangedCalled.size());
        assertEquals(0, listenerTest.shuffleChangedCalled.size());
    }

    @Test
    public void testAddAtCurrentItem() {
        mPlaylistController.setCurrentIndex(1);
        XoTransfer expectedCurrentItem = mPlaylistController.getCurrentItem();
        ListenerTester listenerTest = new ListenerTester(mPlaylistController);

        TalkClientDownload item = null;
        try {
            item = new TalkClientDownload();
            mDatabase.saveClientDownload(item);
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            fail();
        }
        mCollection.addItem(1, item);

        int expectedCurrentIndex = 2;
        assertEquals(expectedCurrentIndex, mPlaylistController.getCurrentIndex());
        assertEquals(expectedCurrentItem, mPlaylistController.getCurrentItem());

        // listener test
        assertEquals(0, listenerTest.currentItemChangedCalls.size());
        assertEquals(1, listenerTest.playlistChangedCalled.size());
        assertEquals(0, listenerTest.repeatModeChangedCalled.size());
        assertEquals(0, listenerTest.shuffleChangedCalled.size());
    }

    @Test
    public void testAddAfterCurrentItem() {
        mPlaylistController.setCurrentIndex(1);
        XoTransfer expectedCurrentItem = mPlaylistController.getCurrentItem();
        ListenerTester listenerTest = new ListenerTester(mPlaylistController);

        TalkClientDownload item = null;
        try {
            item = new TalkClientDownload();
            mDatabase.saveClientDownload(item);
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            fail();
        }
        mCollection.addItem(2, item);

        int expectedCurrentIndex = 1;
        assertEquals(expectedCurrentIndex, mPlaylistController.getCurrentIndex());
        assertEquals(expectedCurrentItem, mPlaylistController.getCurrentItem());

        // listener test
        assertEquals(0, listenerTest.currentItemChangedCalls.size());
        assertEquals(1, listenerTest.playlistChangedCalled.size());
        assertEquals(0, listenerTest.repeatModeChangedCalled.size());
        assertEquals(0, listenerTest.shuffleChangedCalled.size());
    }

    @Test
    public void testClear() {
        ListenerTester listenerTest = new ListenerTester(mPlaylistController);

        mCollection.clear();

        int expectedCurrentIndex = -1;
        assertEquals(expectedCurrentIndex, mPlaylistController.getCurrentIndex());
        assertNull(mPlaylistController.getCurrentItem());

        // listener test
        assertEquals(1, listenerTest.currentItemChangedCalls.size());
        assertEquals(null, listenerTest.currentItemChangedCalls.get(0).args[0]);
        assertEquals(0, listenerTest.playlistChangedCalled.size());
        assertEquals(0, listenerTest.repeatModeChangedCalled.size());
        assertEquals(0, listenerTest.shuffleChangedCalled.size());
    }

    @Test
    public void testReorderPlaylistFromCurrent() {
        mPlaylistController.setCurrentIndex(1);
        XoTransfer expectedCurrentItem = mPlaylistController.getCurrentItem();
        ListenerTester listenerTest = new ListenerTester(mPlaylistController);

        mCollection.reorderItemIndex(1, 3);

        int expectedCurrentIndex = 3;
        assertEquals(expectedCurrentIndex, mPlaylistController.getCurrentIndex());
        assertEquals(expectedCurrentItem, mPlaylistController.getCurrentItem());

        // listener test
        assertEquals(0, listenerTest.currentItemChangedCalls.size());
        assertEquals(0, listenerTest.playlistChangedCalled.size());
        assertEquals(0, listenerTest.repeatModeChangedCalled.size());
        assertEquals(0, listenerTest.shuffleChangedCalled.size());
    }

    @Test
    public void testReorderPlaylistToCurrent() {
        mPlaylistController.setCurrentIndex(1);
        XoTransfer expectedCurrentItem = mPlaylistController.getCurrentItem();
        ListenerTester listenerTest = new ListenerTester(mPlaylistController);

        mCollection.reorderItemIndex(3, 1);

        int expectedCurrentIndex = 2;
        assertEquals(expectedCurrentIndex, mPlaylistController.getCurrentIndex());
        assertEquals(expectedCurrentItem, mPlaylistController.getCurrentItem());

        // listener test
        assertEquals(0, listenerTest.currentItemChangedCalls.size());
        assertEquals(0, listenerTest.playlistChangedCalled.size());
        assertEquals(0, listenerTest.repeatModeChangedCalled.size());
        assertEquals(0, listenerTest.shuffleChangedCalled.size());
    }

    @Test
    public void testReorderPlaylistOthers() {
        mPlaylistController.setCurrentIndex(1);
        XoTransfer expectedCurrentItem = mPlaylistController.getCurrentItem();
        ListenerTester listenerTest = new ListenerTester(mPlaylistController);

        mCollection.reorderItemIndex(0, 2);

        int expectedCurrentIndex = 0;
        assertEquals(expectedCurrentIndex, mPlaylistController.getCurrentIndex());
        assertEquals(expectedCurrentItem, mPlaylistController.getCurrentItem());

        // listener test
        assertEquals(0, listenerTest.currentItemChangedCalls.size());
        assertEquals(0, listenerTest.playlistChangedCalled.size());
        assertEquals(0, listenerTest.repeatModeChangedCalled.size());
        assertEquals(0, listenerTest.shuffleChangedCalled.size());
    }

    @Test
    public void testNextWithNoRepeat() {
        mPlaylistController.setCurrentIndex(1);
        XoTransfer expectedCurrentItem = mCollection.getItem(2);
        ListenerTester listenerTest = new ListenerTester(mPlaylistController);

        mPlaylistController.setRepeatMode(MediaPlaylistController.RepeatMode.NO_REPEAT);
        XoTransfer actualCurrentItem = mPlaylistController.forward();
        assertEquals(expectedCurrentItem, actualCurrentItem);

        int expectedCurrentIndex = 2;
        assertEquals(expectedCurrentIndex, mPlaylistController.getCurrentIndex());
        assertEquals(expectedCurrentItem, mPlaylistController.getCurrentItem());

        // listener test
        assertEquals(1, listenerTest.currentItemChangedCalls.size());
        assertEquals(expectedCurrentItem, listenerTest.currentItemChangedCalls.get(0).args[0]);
        assertEquals(0, listenerTest.playlistChangedCalled.size());
        assertEquals(0, listenerTest.repeatModeChangedCalled.size());
        assertEquals(0, listenerTest.shuffleChangedCalled.size());
    }

    @Test
    public void testNextWithNoRepeatAtEnd() {
        mPlaylistController.setCurrentIndex(3);
        ListenerTester listenerTest = new ListenerTester(mPlaylistController);

        mPlaylistController.setRepeatMode(MediaPlaylistController.RepeatMode.NO_REPEAT);
        XoTransfer actualCurrentItem = mPlaylistController.forward();
        assertEquals(null, actualCurrentItem);

        int expectedCurrentIndex = -1;
        assertEquals(expectedCurrentIndex, mPlaylistController.getCurrentIndex());
        assertEquals(null, mPlaylistController.getCurrentItem());

        // listener test
        assertEquals(1, listenerTest.currentItemChangedCalls.size());
        assertEquals(null, listenerTest.currentItemChangedCalls.get(0).args[0]);
        assertEquals(0, listenerTest.playlistChangedCalled.size());
        assertEquals(0, listenerTest.repeatModeChangedCalled.size());
        assertEquals(0, listenerTest.shuffleChangedCalled.size());
    }

    @Test
    public void testNextWithRepeatAll() {
        mPlaylistController.setCurrentIndex(1);
        XoTransfer expectedCurrentItem = mCollection.getItem(2);
        ListenerTester listenerTest = new ListenerTester(mPlaylistController);

        mPlaylistController.setRepeatMode(MediaPlaylistController.RepeatMode.REPEAT_ALL);
        XoTransfer actualCurrentItem = mPlaylistController.forward();
        assertEquals(expectedCurrentItem, actualCurrentItem);

        int expectedCurrentIndex = 2;
        assertEquals(expectedCurrentIndex, mPlaylistController.getCurrentIndex());
        assertEquals(expectedCurrentItem, mPlaylistController.getCurrentItem());

        // listener test
        assertEquals(1, listenerTest.currentItemChangedCalls.size());
        assertEquals(expectedCurrentItem, listenerTest.currentItemChangedCalls.get(0).args[0]);
        assertEquals(0, listenerTest.playlistChangedCalled.size());
        assertEquals(1, listenerTest.repeatModeChangedCalled.size());
        assertEquals(MediaPlaylistController.RepeatMode.REPEAT_ALL, listenerTest.repeatModeChangedCalled.get(0).args[0]);
        assertEquals(0, listenerTest.shuffleChangedCalled.size());
    }

    @Test
    public void testNextWithRepeatAllAtEnd() {
        mPlaylistController.setCurrentIndex(3);
        XoTransfer expectedCurrentItem = mCollection.getItem(0);
        ListenerTester listenerTest = new ListenerTester(mPlaylistController);

        mPlaylistController.setRepeatMode(MediaPlaylistController.RepeatMode.REPEAT_ALL);
        XoTransfer actualCurrentItem = mPlaylistController.forward();
        assertEquals(expectedCurrentItem, actualCurrentItem);

        int expectedCurrentIndex = 0;
        assertEquals(expectedCurrentIndex, mPlaylistController.getCurrentIndex());
        assertEquals(expectedCurrentItem, mPlaylistController.getCurrentItem());

        // listener test
        assertEquals(1, listenerTest.currentItemChangedCalls.size());
        assertEquals(expectedCurrentItem, listenerTest.currentItemChangedCalls.get(0).args[0]);
        assertEquals(0, listenerTest.playlistChangedCalled.size());
        assertEquals(MediaPlaylistController.RepeatMode.REPEAT_ALL, listenerTest.repeatModeChangedCalled.get(0).args[0]);
        assertEquals(0, listenerTest.shuffleChangedCalled.size());
    }

    @Test
    public void testNextWithRepeatItem() {
        mPlaylistController.setCurrentIndex(1);
        XoTransfer expectedCurrentItem = mPlaylistController.getCurrentItem();
        ListenerTester listenerTest = new ListenerTester(mPlaylistController);

        mPlaylistController.setRepeatMode(MediaPlaylistController.RepeatMode.REPEAT_ITEM);
        XoTransfer actualCurrentItem = mPlaylistController.forward();
        assertEquals(expectedCurrentItem, actualCurrentItem);

        int expectedCurrentIndex = 1;
        assertEquals(expectedCurrentIndex, mPlaylistController.getCurrentIndex());
        assertEquals(expectedCurrentItem, mPlaylistController.getCurrentItem());

        // listener test
        assertEquals(0, listenerTest.currentItemChangedCalls.size());
        assertEquals(0, listenerTest.playlistChangedCalled.size());
        assertEquals(MediaPlaylistController.RepeatMode.REPEAT_ITEM, listenerTest.repeatModeChangedCalled.get(0).args[0]);
        assertEquals(0, listenerTest.shuffleChangedCalled.size());
    }

    @Test
    public void testPreviousWithNoRepeat() {
        mPlaylistController.setCurrentIndex(2);
        XoTransfer expectedCurrentItem = mCollection.getItem(1);
        ListenerTester listenerTest = new ListenerTester(mPlaylistController);

        mPlaylistController.setRepeatMode(MediaPlaylistController.RepeatMode.NO_REPEAT);
        XoTransfer actualCurrentItem = mPlaylistController.backward();
        assertEquals(expectedCurrentItem, actualCurrentItem);

        int expectedCurrentIndex = 1;
        assertEquals(expectedCurrentIndex, mPlaylistController.getCurrentIndex());
        assertEquals(expectedCurrentItem, mPlaylistController.getCurrentItem());

        // listener test
        assertEquals(1, listenerTest.currentItemChangedCalls.size());
        assertEquals(expectedCurrentItem, listenerTest.currentItemChangedCalls.get(0).args[0]);
        assertEquals(0, listenerTest.playlistChangedCalled.size());
        assertEquals(0, listenerTest.repeatModeChangedCalled.size());
        assertEquals(0, listenerTest.shuffleChangedCalled.size());
    }

    @Test
    public void testPreviousWithNoRepeatAtBeginning() {
        mPlaylistController.setCurrentIndex(0);
        ListenerTester listenerTest = new ListenerTester(mPlaylistController);

        mPlaylistController.setRepeatMode(MediaPlaylistController.RepeatMode.NO_REPEAT);
        XoTransfer actualCurrentItem = mPlaylistController.backward();
        assertEquals(null, actualCurrentItem);

        int expectedCurrentIndex = -1;
        assertEquals(expectedCurrentIndex, mPlaylistController.getCurrentIndex());
        assertEquals(null, mPlaylistController.getCurrentItem());

        // listener test
        assertEquals(1, listenerTest.currentItemChangedCalls.size());
        assertEquals(null, listenerTest.currentItemChangedCalls.get(0).args[0]);
        assertEquals(0, listenerTest.playlistChangedCalled.size());
        assertEquals(0, listenerTest.repeatModeChangedCalled.size());
        assertEquals(0, listenerTest.shuffleChangedCalled.size());
    }

    @Test
    public void testPreviousWithRepeatAll() {
        mPlaylistController.setCurrentIndex(2);
        XoTransfer expectedCurrentItem = mCollection.getItem(1);
        ListenerTester listenerTest = new ListenerTester(mPlaylistController);

        mPlaylistController.setRepeatMode(MediaPlaylistController.RepeatMode.REPEAT_ALL);
        XoTransfer actualCurrentItem = mPlaylistController.backward();
        assertEquals(expectedCurrentItem, actualCurrentItem);

        int expectedCurrentIndex = 1;
        assertEquals(expectedCurrentIndex, mPlaylistController.getCurrentIndex());
        assertEquals(expectedCurrentItem, mPlaylistController.getCurrentItem());

        // listener test
        assertEquals(1, listenerTest.currentItemChangedCalls.size());
        assertEquals(expectedCurrentItem, listenerTest.currentItemChangedCalls.get(0).args[0]);
        assertEquals(0, listenerTest.playlistChangedCalled.size());
        assertEquals(MediaPlaylistController.RepeatMode.REPEAT_ALL, listenerTest.repeatModeChangedCalled.get(0).args[0]);
        assertEquals(0, listenerTest.shuffleChangedCalled.size());
    }

    @Test
    public void testPreviousWithRepeatAllAtBeginning() {
        mPlaylistController.setCurrentIndex(0);
        XoTransfer expectedCurrentItem = mCollection.getItem(3);
        ListenerTester listenerTest = new ListenerTester(mPlaylistController);

        mPlaylistController.setRepeatMode(MediaPlaylistController.RepeatMode.REPEAT_ALL);
        XoTransfer actualCurrentItem = mPlaylistController.backward();
        assertEquals(expectedCurrentItem, actualCurrentItem);

        int expectedCurrentIndex = 3;
        assertEquals(expectedCurrentIndex, mPlaylistController.getCurrentIndex());
        assertEquals(expectedCurrentItem, mPlaylistController.getCurrentItem());

        // listener test
        assertEquals(1, listenerTest.currentItemChangedCalls.size());
        assertEquals(expectedCurrentItem, listenerTest.currentItemChangedCalls.get(0).args[0]);
        assertEquals(0, listenerTest.playlistChangedCalled.size());
        assertEquals(MediaPlaylistController.RepeatMode.REPEAT_ALL, listenerTest.repeatModeChangedCalled.get(0).args[0]);
        assertEquals(0, listenerTest.shuffleChangedCalled.size());
    }

    @Test
    public void testPreviousWithRepeatItem() {
        mPlaylistController.setCurrentIndex(1);
        XoTransfer expectedCurrentItem = mPlaylistController.getCurrentItem();
        ListenerTester listenerTest = new ListenerTester(mPlaylistController);

        mPlaylistController.setRepeatMode(MediaPlaylistController.RepeatMode.REPEAT_ITEM);
        XoTransfer actualCurrentItem = mPlaylistController.backward();
        assertEquals(expectedCurrentItem, actualCurrentItem);

        int expectedCurrentIndex = 1;
        assertEquals(expectedCurrentIndex, mPlaylistController.getCurrentIndex());
        assertEquals(expectedCurrentItem, mPlaylistController.getCurrentItem());

        // listener test
        assertEquals(0, listenerTest.currentItemChangedCalls.size());
        assertEquals(0, listenerTest.playlistChangedCalled.size());
        assertEquals(MediaPlaylistController.RepeatMode.REPEAT_ITEM, listenerTest.repeatModeChangedCalled.get(0).args[0]);
        assertEquals(0, listenerTest.shuffleChangedCalled.size());
    }

    @Test
    public void testAddShuffled() {
        mPlaylistController.setCurrentIndex(1);
        XoTransfer expectedCurrentItem = mPlaylistController.getCurrentItem();
        ListenerTester listenerTest = new ListenerTester(mPlaylistController);

        mPlaylistController.setShuffleActive(true);

        TalkClientDownload item = null;
        try {
            item = new TalkClientDownload();
            mDatabase.saveClientDownload(item);
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            fail();
        }
        mCollection.addItem(3, item);

        int expectedCurrentIndex = 1;
        assertEquals(expectedCurrentIndex, mPlaylistController.getCurrentIndex());
        assertEquals(expectedCurrentItem, mPlaylistController.getCurrentItem());

        // listener test
        assertEquals(0, listenerTest.currentItemChangedCalls.size());
        assertEquals(1, listenerTest.playlistChangedCalled.size());
        assertEquals(0, listenerTest.repeatModeChangedCalled.size());
        assertEquals(1, listenerTest.shuffleChangedCalled.size());
        assertEquals(true, listenerTest.shuffleChangedCalled.get(0).args[0]);
    }

    @Test
    public void testRemoveShuffled() {
        mPlaylistController.setCurrentIndex(1);
        XoTransfer expectedCurrentItem = mPlaylistController.getCurrentItem();
        ListenerTester listenerTest = new ListenerTester(mPlaylistController);

        mPlaylistController.setShuffleActive(true);

        mCollection.removeItem(2);

        int expectedCurrentIndex = 1;
        assertEquals(expectedCurrentIndex, mPlaylistController.getCurrentIndex());
        assertEquals(expectedCurrentItem, mPlaylistController.getCurrentItem());

        // listener test
        assertEquals(0, listenerTest.currentItemChangedCalls.size());
        assertEquals(1, listenerTest.playlistChangedCalled.size());
        assertEquals(0, listenerTest.repeatModeChangedCalled.size());
        assertEquals(1, listenerTest.shuffleChangedCalled.size());
        assertEquals(true, listenerTest.shuffleChangedCalled.get(0).args[0]);
    }

    @Test
    public void testReset() {
        mPlaylistController.setCurrentIndex(1);
        ListenerTester listenerTest = new ListenerTester(mPlaylistController);

        mPlaylistController.reset();

        int expectedCurrentIndex = -1;
        assertEquals(expectedCurrentIndex, mPlaylistController.getCurrentIndex());
        assertEquals(null, mPlaylistController.getCurrentItem());

        // listener test
        assertEquals(1, listenerTest.currentItemChangedCalls.size());
        assertEquals(null, listenerTest.currentItemChangedCalls.get(0).args[0]);
        assertEquals(1, listenerTest.playlistChangedCalled.size());
        assertEquals(0, listenerTest.repeatModeChangedCalled.size());
        assertEquals(0, listenerTest.shuffleChangedCalled.size());
    }

    ////////////////////////////////
    //////// Helper methods ////////
    ////////////////////////////////

    public class Call {
        public Object[] args;

        public Call(Object... args) {
            this.args = args;
        }
    }

    // logs listener calls
    private class ListenerTester {
        public ArrayList<Call> currentItemChangedCalls = new ArrayList<Call>();
        public ArrayList<Call> playlistChangedCalled = new ArrayList<Call>();
        public ArrayList<Call> repeatModeChangedCalled = new ArrayList<Call>();
        public ArrayList<Call> shuffleChangedCalled = new ArrayList<Call>();

        public ListenerTester(MediaPlaylistController playlist) {
            MediaPlaylistController.Listener listener = new MediaPlaylistController.Listener() {
                @Override
                public void onCurrentItemChanged(XoTransfer newItem) {
                    currentItemChangedCalls.add(new Call(newItem));
                }

                @Override
                public void onPlaylistChanged(MediaPlaylist newPlaylist) {
                    playlistChangedCalled.add(new Call(newPlaylist));
                }

                @Override
                public void onRepeatModeChanged(MediaPlaylistController.RepeatMode newMode) {
                    repeatModeChangedCalled.add(new Call(newMode));
                }

                @Override
                public void onShuffleChanged(boolean isShuffled) {
                    shuffleChangedCalled.add(new Call(isShuffled));
                }
            };
            playlist.registerListener(listener);
        }
    }

    private TalkClientMediaCollection createMediaCollection() {
        TalkClientMediaCollection collection = null;
        try {
            collection = mDatabase.createMediaCollection("test_collection");

            int itemCount = 4; // the tests rely on the given count here!
            for (int i = 0; i < itemCount; i++) {
                TalkClientDownload item = new TalkClientDownload();
                mDatabase.saveClientDownload(item);
                collection.addItem(item);
            }
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
        }

        return collection;
    }
}
