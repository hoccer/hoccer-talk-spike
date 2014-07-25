import com.hoccer.talk.client.IXoClientDatabaseBackend;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientMediaCollection;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.xo.android.content.MediaCollectionPlaylist;
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

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.fail;

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
        IContentObject expectedCurrentItem = mPlaylistController.getCurrentItem();

        mCollection.removeItem(0);

        int expectedCurrentIndex = 0;
        assertEquals(expectedCurrentIndex, mPlaylistController.getCurrentIndex());
        assertEquals(expectedCurrentItem, mPlaylistController.getCurrentItem());
    }

    @Test
    public void testRemoveAtCurrentItem() {
        mPlaylistController.setCurrentIndex(2);
        IContentObject expectedCurrentItem = mCollection.getItem(3);

        mCollection.removeItem(2);

        int expectedCurrentIndex = 2;
        assertEquals(expectedCurrentIndex, mPlaylistController.getCurrentIndex());
        assertEquals(expectedCurrentItem, mPlaylistController.getCurrentItem());
    }

    @Test
    public void testRemoveAfterCurrentItem() {
        mPlaylistController.setCurrentIndex(1);
        IContentObject expectedCurrentItem = mPlaylistController.getCurrentItem();

        mCollection.removeItem(2);

        int expectedCurrentIndex = 1;
        assertEquals(expectedCurrentIndex, mPlaylistController.getCurrentIndex());
        assertEquals(expectedCurrentItem, mPlaylistController.getCurrentItem());
    }

    @Test
    public void testAddBeforeCurrentItem() {
        mPlaylistController.setCurrentIndex(2);
        IContentObject expectedCurrentItem = mPlaylistController.getCurrentItem();

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
    }

    @Test
    public void testAddAtCurrentItem() {
        mPlaylistController.setCurrentIndex(1);
        IContentObject expectedCurrentItem = mPlaylistController.getCurrentItem();

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
    }

    @Test
    public void testAddAfterCurrentItem() {
        mPlaylistController.setCurrentIndex(1);
        IContentObject expectedCurrentItem = mPlaylistController.getCurrentItem();

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
    }


    @Test
    public void testClear() {
        mCollection.clear();

        int expectedCurrentIndex = -1;
        assertEquals(expectedCurrentIndex, mPlaylistController.getCurrentIndex());
        assertNull(mPlaylistController.getCurrentItem());
    }

    @Test
    public void testReorderPlaylistFromCurrent() {
        mPlaylistController.setCurrentIndex(1);
        IContentObject expectedCurrentItem = mPlaylistController.getCurrentItem();

        mCollection.reorderItemIndex(1, 3);

        int expectedCurrentIndex = 3;
        assertEquals(expectedCurrentIndex, mPlaylistController.getCurrentIndex());
        assertEquals(expectedCurrentItem, mPlaylistController.getCurrentItem());
    }

    @Test
    public void testReorderPlaylistToCurrent() {
        mPlaylistController.setCurrentIndex(1);
        IContentObject expectedCurrentItem = mPlaylistController.getCurrentItem();

        mCollection.reorderItemIndex(3, 1);

        int expectedCurrentIndex = 2;
        assertEquals(expectedCurrentIndex, mPlaylistController.getCurrentIndex());
        assertEquals(expectedCurrentItem, mPlaylistController.getCurrentItem());
    }

    @Test
    public void testReorderPlaylistOthers() {
        mPlaylistController.setCurrentIndex(1);
        IContentObject expectedCurrentItem = mPlaylistController.getCurrentItem();

        mCollection.reorderItemIndex(0, 2);

        int expectedCurrentIndex = 0;
        assertEquals(expectedCurrentIndex, mPlaylistController.getCurrentIndex());
        assertEquals(expectedCurrentItem, mPlaylistController.getCurrentItem());
    }

    @Test
    public void testNextWithNoRepeat() {
        mPlaylistController.setCurrentIndex(1);
        IContentObject expectedCurrentItem = mCollection.getItem(2);

        mPlaylistController.setRepeatMode(MediaPlaylistController.RepeatMode.NO_REPEAT);
        IContentObject actualCurrentItem = mPlaylistController.forward();
        assertEquals(expectedCurrentItem, actualCurrentItem);

        int expectedCurrentIndex = 2;
        assertEquals(expectedCurrentIndex, mPlaylistController.getCurrentIndex());
        assertEquals(expectedCurrentItem, mPlaylistController.getCurrentItem());
    }

    @Test
    public void testNextWithNoRepeatAtEnd() {
        mPlaylistController.setCurrentIndex(3);
        IContentObject expectedCurrentItem = mPlaylistController.getCurrentItem();

        mPlaylistController.setRepeatMode(MediaPlaylistController.RepeatMode.NO_REPEAT);
        IContentObject actualCurrentItem = mPlaylistController.forward();
        assertEquals(null, actualCurrentItem);

        int expectedCurrentIndex = 3;
        assertEquals(expectedCurrentIndex, mPlaylistController.getCurrentIndex());
        assertEquals(expectedCurrentItem, mPlaylistController.getCurrentItem());
    }

    @Test
    public void testNextWithRepeatAll() {
        mPlaylistController.setCurrentIndex(1);
        IContentObject expectedCurrentItem = mCollection.getItem(2);

        mPlaylistController.setRepeatMode(MediaPlaylistController.RepeatMode.REPEAT_ALL);
        IContentObject actualCurrentItem = mPlaylistController.forward();
        assertEquals(expectedCurrentItem, actualCurrentItem);

        int expectedCurrentIndex = 2;
        assertEquals(expectedCurrentIndex, mPlaylistController.getCurrentIndex());
        assertEquals(expectedCurrentItem, mPlaylistController.getCurrentItem());
    }

    @Test
    public void testNextWithRepeatAllAtEnd() {
        mPlaylistController.setCurrentIndex(3);
        IContentObject expectedCurrentItem = mCollection.getItem(0);

        mPlaylistController.setRepeatMode(MediaPlaylistController.RepeatMode.REPEAT_ALL);
        IContentObject actualCurrentItem = mPlaylistController.forward();
        assertEquals(expectedCurrentItem, actualCurrentItem);

        int expectedCurrentIndex = 0;
        assertEquals(expectedCurrentIndex, mPlaylistController.getCurrentIndex());
        assertEquals(expectedCurrentItem, mPlaylistController.getCurrentItem());
    }

    @Test
    public void testNextWithRepeatItem() {
        mPlaylistController.setCurrentIndex(1);
        IContentObject expectedCurrentItem = mPlaylistController.getCurrentItem();

        mPlaylistController.setRepeatMode(MediaPlaylistController.RepeatMode.REPEAT_ITEM);
        IContentObject actualCurrentItem = mPlaylistController.forward();
        assertEquals(expectedCurrentItem, actualCurrentItem);

        int expectedCurrentIndex = 1;
        assertEquals(expectedCurrentIndex, mPlaylistController.getCurrentIndex());
        assertEquals(expectedCurrentItem, mPlaylistController.getCurrentItem());
    }

    @Test
    public void testPreviousWithNoRepeat() {
        mPlaylistController.setCurrentIndex(2);
        IContentObject expectedCurrentItem = mCollection.getItem(1);

        mPlaylistController.setRepeatMode(MediaPlaylistController.RepeatMode.NO_REPEAT);
        IContentObject actualCurrentItem = mPlaylistController.backward();
        assertEquals(expectedCurrentItem, actualCurrentItem);

        int expectedCurrentIndex = 1;
        assertEquals(expectedCurrentIndex, mPlaylistController.getCurrentIndex());
        assertEquals(expectedCurrentItem, mPlaylistController.getCurrentItem());
    }

    @Test
    public void testPreviousWithNoRepeatAtBeginning() {
        mPlaylistController.setCurrentIndex(0);
        IContentObject expectedCurrentItem = mPlaylistController.getCurrentItem();

        mPlaylistController.setRepeatMode(MediaPlaylistController.RepeatMode.NO_REPEAT);
        IContentObject actualCurrentItem = mPlaylistController.backward();
        assertEquals(null, actualCurrentItem);

        int expectedCurrentIndex = 0;
        assertEquals(expectedCurrentIndex, mPlaylistController.getCurrentIndex());
        assertEquals(expectedCurrentItem, mPlaylistController.getCurrentItem());
    }

    @Test
    public void testPreviousWithRepeatAll() {
        mPlaylistController.setCurrentIndex(2);
        IContentObject expectedCurrentItem = mCollection.getItem(1);

        mPlaylistController.setRepeatMode(MediaPlaylistController.RepeatMode.REPEAT_ALL);
        IContentObject actualCurrentItem = mPlaylistController.backward();
        assertEquals(expectedCurrentItem, actualCurrentItem);

        int expectedCurrentIndex = 1;
        assertEquals(expectedCurrentIndex, mPlaylistController.getCurrentIndex());
        assertEquals(expectedCurrentItem, mPlaylistController.getCurrentItem());
    }

    @Test
    public void testPreviousWithRepeatAllAtBeginning() {
        mPlaylistController.setCurrentIndex(0);
        IContentObject expectedCurrentItem = mCollection.getItem(3);

        mPlaylistController.setRepeatMode(MediaPlaylistController.RepeatMode.REPEAT_ALL);
        IContentObject actualCurrentItem = mPlaylistController.backward();
        assertEquals(expectedCurrentItem, actualCurrentItem);

        int expectedCurrentIndex = 3;
        assertEquals(expectedCurrentIndex, mPlaylistController.getCurrentIndex());
        assertEquals(expectedCurrentItem, mPlaylistController.getCurrentItem());
    }

    @Test
    public void testPreviousWithRepeatItem() {
        mPlaylistController.setCurrentIndex(1);
        IContentObject expectedCurrentItem = mPlaylistController.getCurrentItem();

        mPlaylistController.setRepeatMode(MediaPlaylistController.RepeatMode.REPEAT_ITEM);
        IContentObject actualCurrentItem = mPlaylistController.backward();
        assertEquals(expectedCurrentItem, actualCurrentItem);

        int expectedCurrentIndex = 1;
        assertEquals(expectedCurrentIndex, mPlaylistController.getCurrentIndex());
        assertEquals(expectedCurrentItem, mPlaylistController.getCurrentItem());
    }

    //////// Helpers ////////

    private TalkClientMediaCollection createMediaCollection() {
        TalkClientMediaCollection collection = null;
        try {
            collection = mDatabase.createMediaCollection("test_collection");

            int itemCount = 4; // the tests rely on the given count here!
            for(int i = 0; i < itemCount; i++) {
                TalkClientDownload item = new TalkClientDownload();
                mDatabase.saveClientDownload(item);
                collection.addItem(item);
            }
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
        }

        return collection;
    }

    private class ValueContainer<T> {
        public T value;
        public ValueContainer(T initValue) {
            value = initValue;
        }
    }
}
