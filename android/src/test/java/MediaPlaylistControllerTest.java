import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.xo.android.content.AudioAttachmentItem;
import com.hoccer.xo.android.content.audio.MediaPlaylistController;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.table.TableUtils;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

public class MediaPlaylistControllerTest {

    private static final Logger LOG = Logger.getLogger(MediaPlaylistControllerTest.class);

    private MediaPlaylistController mPlaylistController;
    List<AudioAttachmentItem> mItems = new ArrayList<AudioAttachmentItem>();
    private String mDatabaseUrl = "jdbc:h2:mem:account";

    @Before
    public void testSetup() throws Exception {

        JdbcConnectionSource connectionSource = new JdbcConnectionSource(mDatabaseUrl);
        Dao<TalkClientDownload, Integer> clientDownloadsDao = DaoManager.createDao(connectionSource, TalkClientDownload.class);
        TableUtils.createTable(connectionSource, TalkClientDownload.class);

        mPlaylistController = new MediaPlaylistController();
        mItems = new ArrayList<AudioAttachmentItem>();
        for (int i = 0; i < 4; i++) {
            TalkClientDownload tcd = new TalkClientDownload();
            clientDownloadsDao.create(tcd);
            AudioAttachmentItem item = AudioAttachmentItem.create("test_dummy_path", tcd, false);
            mItems.add(item);
        }
        mPlaylistController.setTrackList(mItems);

        connectionSource.close();
    }

    @After
    public void testCleanup() throws SQLException {
        mPlaylistController = null;

        JdbcConnectionSource connectionSource = new JdbcConnectionSource(mDatabaseUrl);
        TableUtils.dropTable(connectionSource, TalkClientDownload.class, true);
        connectionSource.close();
    }

    @Test
    public void testRemoveAfterCurrent() {

        mPlaylistController.setCurrentIndex(1);
        mPlaylistController.remove(mItems.get(2));

        List<Integer> expectedOrder = new ArrayList<Integer>() {
            {
                add(0);
                add(1);
                add(2);
            }
        };

        mItems.remove(2);

        assertEquals(mItems, mPlaylistController.getAudioAttachmentItems());
        assertEquals(1, mPlaylistController.getCurrentIndex());
        assertEquals(expectedOrder, mPlaylistController.getPlaylistOrder());
    }

    @Test
    public void testRemoveBeforeCurrent() {

        mPlaylistController.setCurrentIndex(2);
        mPlaylistController.remove(mItems.get(1));

        List<Integer> expectedOrder = new ArrayList<Integer>() {
            {
                add(0);
                add(1);
                add(2);
            }
        };

        mItems.remove(1);

        assertEquals(mItems, mPlaylistController.getAudioAttachmentItems());
        assertEquals(1, mPlaylistController.getCurrentIndex());
        assertEquals(expectedOrder, mPlaylistController.getPlaylistOrder());
    }

    @Test(expected = IllegalStateException.class)
    public void testRemoveCurrent() {

        mPlaylistController.setCurrentIndex(0);
        mPlaylistController.remove(mItems.get(0));
    }

    @Test
    public void testRemoveBeforeCurrentFromShuffledList() {

        mPlaylistController.setShuffleActive(true);
        mPlaylistController.setPlaylistOrder(new ArrayList<Integer>() {
            {
                add(3);
                add(2);
                add(0);
                add(1);
            }
        });
        mPlaylistController.setCurrentIndex(2);
        mPlaylistController.remove(mItems.get(2));

        List<Integer> expectedOrder = new ArrayList<Integer>() {
            {
                add(2);
                add(0);
                add(1);
            }
        };

        mItems.remove(2);

        assertEquals(mItems, mPlaylistController.getAudioAttachmentItems());
        assertEquals(1, mPlaylistController.getCurrentIndex());
        assertEquals(expectedOrder, mPlaylistController.getPlaylistOrder());
    }

    @Test
    public void testRemoveAfterCurrentFromShuffledList() {

        mPlaylistController.setShuffleActive(true);
        mPlaylistController.setPlaylistOrder(new ArrayList<Integer>() {
            {
                add(3);
                add(2);
                add(0);
                add(1);
            }
        });
        mPlaylistController.setCurrentIndex(0);
        mPlaylistController.remove(mItems.get(0));

        List<Integer> expectedOrder = new ArrayList<Integer>() {
            {
                add(2);
                add(1);
                add(0);
            }
        };

        mItems.remove(0);

        assertEquals(mItems, mPlaylistController.getAudioAttachmentItems());
        assertEquals(0, mPlaylistController.getCurrentIndex());
        assertEquals(expectedOrder, mPlaylistController.getPlaylistOrder());
    }

    @Test(expected = IllegalStateException.class)
    public void testRemoveCurrentFromShuffledList() {

        mPlaylistController.setShuffleActive(true);
        mPlaylistController.setPlaylistOrder(new ArrayList<Integer>() {
            {
                add(3);
                add(2);
                add(0);
                add(1);
            }
        });
        mPlaylistController.setCurrentIndex(2);
        mPlaylistController.remove(mItems.get(0));
    }
}
