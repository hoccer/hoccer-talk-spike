import com.hoccer.talk.client.IXoClientDatabaseBackend;
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
import java.util.List;

import static junit.framework. TestCase.assertTrue;
import static junit.framework. TestCase.assertFalse;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.fail;

public class MediaCollectionTest {

    private static final Logger LOG = Logger.getLogger(MediaCollectionTest.class);

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
                return DaoManager.createDao(mConnectionSource, clazz);
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
    public void testCreateCollection() {
        LOG.info("testCreateCollection");

        String collectionName = "testCreateCollection_collection";
        TalkClientMediaCollection collection = null;
        try {
            collection = mDatabase.createMediaCollection(collectionName);
        } catch(SQLException e) {
            e.printStackTrace();
            fail();
        }

        assertNotNull(collection);
        assertEquals(collectionName, collection.getName());
        assertEquals(0, collection.size());

        TalkClientMediaCollection collectionCopy = null;
        try {
            collectionCopy = mDatabase.findMediaCollectionById(collection.getId());
        } catch(SQLException e) {
            e.printStackTrace();
            fail();
        }

        assertNotNull(collectionCopy);
        assertEquals(collectionName, collectionCopy.getName());
        assertEquals(0, collectionCopy.size());

        // check database directly
        List<TalkClientMediaCollection> collections = null;
        try {
        collections = mDatabase.findAllMediaCollections();
        } catch (SQLException e) {
            e.printStackTrace();
            fail();
        }

        assertNotNull(collections);
        assertEquals(1, collections.size());
        assertEquals(collection.getId(), collections.get(0).getId());
    }

    @Test
    public void testDeleteCollectionByReference() {
        LOG.info("testDeleteCollectionByReference");

        String collectionName = "testDeleteCollectionByReference_collection";
        TalkClientMediaCollection collection = null;
        try {
            collection = mDatabase.createMediaCollection(collectionName);
        } catch(SQLException e) {
            e.printStackTrace();
            fail();
        }

        try {
            mDatabase.deleteMediaCollection(collection);
        } catch (SQLException e) {
            e.printStackTrace();
            fail();
        }

        // check database directly
        {
            List<TalkClientMediaCollection> collections = null;
            try {
                collections = mDatabase.findAllMediaCollections();
            } catch (SQLException e) {
                e.printStackTrace();
                fail();
            }

            assertNotNull(collections);
            assertEquals(0, collections.size());
        }
    }

    @Test
    public void testDeleteCollectionById() {
        LOG.info("testDeleteCollectionById");

        String collectionName = "testDeleteCollectionById_collection";
        TalkClientMediaCollection collection = null;
        try {
            collection = mDatabase.createMediaCollection(collectionName);
        } catch(SQLException e) {
            e.printStackTrace();
            fail();
        }

        try {
            mDatabase.deleteMediaCollectionById(collection.getId());
        } catch (SQLException e) {
            e.printStackTrace();
            fail();
        }

        // check database directly
        {
            List<TalkClientMediaCollection> collections = null;
            try {
                collections = mDatabase.findAllMediaCollections();
            } catch (SQLException e) {
                e.printStackTrace();
                fail();
            }

            assertNotNull(collections);
            assertEquals(0, collections.size());
        }
    }

    @Test
    public void testAddItems() {
        LOG.info("testAddItems");

        String collectionName = "testAddItems_collection";

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
            LOG.error(e.getMessage());
            e.printStackTrace();
            fail();
        }

        assertEquals(3, collection.size());
        assertEquals(item0.getClientDownloadId(), collection.getItem(0).getClientDownloadId());
        assertEquals(item1.getClientDownloadId(), collection.getItem(1).getClientDownloadId());
        assertEquals(item2.getClientDownloadId(), collection.getItem(2).getClientDownloadId());

        // check database directly
        List<TalkClientMediaCollectionRelation> relations = findMediaCollectionRelationsOrderedByIndex(collection.getId());
        assertNotNull(relations);
        assertEquals(3, relations.size());
        assertEquals(0, relations.get(0).getIndex());
        assertEquals(item0.getClientDownloadId(), relations.get(0).getItem().getClientDownloadId());
        assertEquals(1, relations.get(1).getIndex());
        assertEquals(item1.getClientDownloadId(), relations.get(1).getItem().getClientDownloadId());
        assertEquals(2, relations.get(2).getIndex());
        assertEquals(item2.getClientDownloadId(), relations.get(2).getItem().getClientDownloadId());
    }

    @Test
    public void testInsertItems() {
        LOG.info("testInsertItems");

        String collectionName = "testInsertItems_collection";

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

            // insert items at "random" positions
            collection.addItem(5, item0); // order: 0
            collection.addItem(1, item1); // order: 0 1
            collection.addItem(0, item2); // order: 2 0 1
            collection.addItem(1, item3); // order: 2 3 0 1
        } catch (SQLException e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
            fail();
        }

        assertEquals(4, collection.size());
        assertEquals(item2.getClientDownloadId(), collection.getItem(0).getClientDownloadId());
        assertEquals(item3.getClientDownloadId(), collection.getItem(1).getClientDownloadId());
        assertEquals(item0.getClientDownloadId(), collection.getItem(2).getClientDownloadId());
        assertEquals(item1.getClientDownloadId(), collection.getItem(3).getClientDownloadId());

        // check database directly
        List<TalkClientMediaCollectionRelation> relations = findMediaCollectionRelationsOrderedByIndex(collection.getId());
        assertNotNull(relations);
        assertEquals(4, relations.size());
        assertEquals(0, relations.get(0).getIndex());
        assertEquals(item2.getClientDownloadId(), relations.get(0).getItem().getClientDownloadId());
        assertEquals(1, relations.get(1).getIndex());
        assertEquals(item3.getClientDownloadId(), relations.get(1).getItem().getClientDownloadId());
        assertEquals(2, relations.get(2).getIndex());
        assertEquals(item0.getClientDownloadId(), relations.get(2).getItem().getClientDownloadId());
        assertEquals(3, relations.get(3).getIndex());
        assertEquals(item1.getClientDownloadId(), relations.get(3).getItem().getClientDownloadId());
    }

    @Test
    public void testRemoveItems() {
        LOG.info("testRemoveItems");

        String collectionName = "testRemoveItems_collection";

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
            collection.addItem(item3);
        } catch (SQLException e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
            fail();
        }

        assertEquals(4, collection.size());
        assertEquals(item0.getClientDownloadId(), collection.getItem(0).getClientDownloadId());
        assertEquals(item1.getClientDownloadId(), collection.getItem(1).getClientDownloadId());
        assertEquals(item2.getClientDownloadId(), collection.getItem(2).getClientDownloadId());
        assertEquals(item3.getClientDownloadId(), collection.getItem(3).getClientDownloadId());

        // check database directly
        {
            List<TalkClientMediaCollectionRelation> relations = findMediaCollectionRelationsOrderedByIndex(collection.getId());
            assertNotNull(relations);
            assertEquals(4, relations.size());
            assertEquals(0, relations.get(0).getIndex());
            assertEquals(item0.getClientDownloadId(), relations.get(0).getItem().getClientDownloadId());
            assertEquals(1, relations.get(1).getIndex());
            assertEquals(item1.getClientDownloadId(), relations.get(1).getItem().getClientDownloadId());
            assertEquals(2, relations.get(2).getIndex());
            assertEquals(item2.getClientDownloadId(), relations.get(2).getItem().getClientDownloadId());
            assertEquals(3, relations.get(3).getIndex());
            assertEquals(item3.getClientDownloadId(), relations.get(3).getItem().getClientDownloadId());
        }

        collection.removeItem(1);

        assertEquals(3, collection.size());
        assertEquals(item0.getClientDownloadId(), collection.getItem(0).getClientDownloadId());
        assertEquals(item2.getClientDownloadId(), collection.getItem(1).getClientDownloadId());
        assertEquals(item3.getClientDownloadId(), collection.getItem(2).getClientDownloadId());

        // check database directly
        {
            List<TalkClientMediaCollectionRelation> relations = findMediaCollectionRelationsOrderedByIndex(collection.getId());
            assertNotNull(relations);
            assertEquals(3, relations.size());
            assertEquals(0, relations.get(0).getIndex());
            assertEquals(item0.getClientDownloadId(), relations.get(0).getItem().getClientDownloadId());
            assertEquals(1, relations.get(1).getIndex());
            assertEquals(item2.getClientDownloadId(), relations.get(1).getItem().getClientDownloadId());
            assertEquals(2, relations.get(2).getIndex());
            assertEquals(item3.getClientDownloadId(), relations.get(2).getItem().getClientDownloadId());
        }

        collection.removeItem(item3);

        assertEquals(2, collection.size());
        assertEquals(item0.getClientDownloadId(), collection.getItem(0).getClientDownloadId());
        assertEquals(item2.getClientDownloadId(), collection.getItem(1).getClientDownloadId());

        // check database directly
        {
            List<TalkClientMediaCollectionRelation> relations = findMediaCollectionRelationsOrderedByIndex(collection.getId());
            assertNotNull(relations);
            assertEquals(2, relations.size());
            assertEquals(0, relations.get(0).getIndex());
            assertEquals(item0.getClientDownloadId(), relations.get(0).getItem().getClientDownloadId());
            assertEquals(1, relations.get(1).getIndex());
            assertEquals(item2.getClientDownloadId(), relations.get(1).getItem().getClientDownloadId());
        }

        // removeItem it again, nothing should change
        collection.removeItem(item3);

        assertEquals(2, collection.size());
        assertEquals(item0.getClientDownloadId(), collection.getItem(0).getClientDownloadId());
        assertEquals(item2.getClientDownloadId(), collection.getItem(1).getClientDownloadId());

        // check database directly
        {
            List<TalkClientMediaCollectionRelation> relations = findMediaCollectionRelationsOrderedByIndex(collection.getId());
            assertNotNull(relations);
            assertEquals(2, relations.size());
            assertEquals(0, relations.get(0).getIndex());
            assertEquals(item0.getClientDownloadId(), relations.get(0).getItem().getClientDownloadId());
            assertEquals(1, relations.get(1).getIndex());
            assertEquals(item2.getClientDownloadId(), relations.get(1).getItem().getClientDownloadId());
        }
    }

    @Test
    public void testReorderItems() {
        LOG.info("testReorderItems");

        String collectionName = "testReorderItems_collection";

        TalkClientMediaCollection collection = null;
        TalkClientDownload item0 = new TalkClientDownload();
        TalkClientDownload item1 = new TalkClientDownload();
        TalkClientDownload item2 = new TalkClientDownload();
        TalkClientDownload item3 = new TalkClientDownload();
        TalkClientDownload item4 = new TalkClientDownload();
        try {
            collection = mDatabase.createMediaCollection(collectionName);

            // create some items and addItem to collection
            mDatabase.saveClientDownload(item0);
            mDatabase.saveClientDownload(item1);
            mDatabase.saveClientDownload(item2);
            mDatabase.saveClientDownload(item3);
            mDatabase.saveClientDownload(item4);

            collection.addItem(item0);
            collection.addItem(item1);
            collection.addItem(item2);
            collection.addItem(item3);
            collection.addItem(item4);
        } catch (SQLException e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
            fail();
        }

        assertEquals(5, collection.size());
        assertEquals(item0.getClientDownloadId(), collection.getItem(0).getClientDownloadId());
        assertEquals(item1.getClientDownloadId(), collection.getItem(1).getClientDownloadId());
        assertEquals(item2.getClientDownloadId(), collection.getItem(2).getClientDownloadId());
        assertEquals(item3.getClientDownloadId(), collection.getItem(3).getClientDownloadId());
        assertEquals(item4.getClientDownloadId(), collection.getItem(4).getClientDownloadId());

        // check database directly
        {
            List<TalkClientMediaCollectionRelation> relations = findMediaCollectionRelationsOrderedByIndex(collection.getId());
            assertNotNull(relations);
            assertEquals(5, relations.size());
            assertEquals(0, relations.get(0).getIndex());
            assertEquals(item0.getClientDownloadId(), relations.get(0).getItem().getClientDownloadId());
            assertEquals(1, relations.get(1).getIndex());
            assertEquals(item1.getClientDownloadId(), relations.get(1).getItem().getClientDownloadId());
            assertEquals(2, relations.get(2).getIndex());
            assertEquals(item2.getClientDownloadId(), relations.get(2).getItem().getClientDownloadId());
            assertEquals(3, relations.get(3).getIndex());
            assertEquals(item3.getClientDownloadId(), relations.get(3).getItem().getClientDownloadId());
            assertEquals(4, relations.get(4).getIndex());
            assertEquals(item4.getClientDownloadId(), relations.get(4).getItem().getClientDownloadId());
        }

        // last to middle
        collection.reorderItemIndex(4, 3);

        assertEquals(5, collection.size());
        assertEquals(item0.getClientDownloadId(), collection.getItem(0).getClientDownloadId());
        assertEquals(item1.getClientDownloadId(), collection.getItem(1).getClientDownloadId());
        assertEquals(item2.getClientDownloadId(), collection.getItem(2).getClientDownloadId());
        assertEquals(item4.getClientDownloadId(), collection.getItem(3).getClientDownloadId());
        assertEquals(item3.getClientDownloadId(), collection.getItem(4).getClientDownloadId());

        // check database directly
        {
            List<TalkClientMediaCollectionRelation> relations = findMediaCollectionRelationsOrderedByIndex(collection.getId());
            assertNotNull(relations);
            assertEquals(5, relations.size());
            assertEquals(0, relations.get(0).getIndex());
            assertEquals(item0.getClientDownloadId(), relations.get(0).getItem().getClientDownloadId());
            assertEquals(1, relations.get(1).getIndex());
            assertEquals(item1.getClientDownloadId(), relations.get(1).getItem().getClientDownloadId());
            assertEquals(2, relations.get(2).getIndex());
            assertEquals(item2.getClientDownloadId(), relations.get(2).getItem().getClientDownloadId());
            assertEquals(3, relations.get(3).getIndex());
            assertEquals(item4.getClientDownloadId(), relations.get(3).getItem().getClientDownloadId());
            assertEquals(4, relations.get(4).getIndex());
            assertEquals(item3.getClientDownloadId(), relations.get(4).getItem().getClientDownloadId());
        }

        // first to middle
        collection.reorderItemIndex(0, 1);

        assertEquals(5, collection.size());
        assertEquals(item1.getClientDownloadId(), collection.getItem(0).getClientDownloadId());
        assertEquals(item0.getClientDownloadId(), collection.getItem(1).getClientDownloadId());
        assertEquals(item2.getClientDownloadId(), collection.getItem(2).getClientDownloadId());
        assertEquals(item4.getClientDownloadId(), collection.getItem(3).getClientDownloadId());
        assertEquals(item3.getClientDownloadId(), collection.getItem(4).getClientDownloadId());

        // check database directly
        {
            List<TalkClientMediaCollectionRelation> relations = findMediaCollectionRelationsOrderedByIndex(collection.getId());
            assertNotNull(relations);
            assertEquals(5, relations.size());
            assertEquals(0, relations.get(0).getIndex());
            assertEquals(item1.getClientDownloadId(), relations.get(0).getItem().getClientDownloadId());
            assertEquals(1, relations.get(1).getIndex());
            assertEquals(item0.getClientDownloadId(), relations.get(1).getItem().getClientDownloadId());
            assertEquals(2, relations.get(2).getIndex());
            assertEquals(item2.getClientDownloadId(), relations.get(2).getItem().getClientDownloadId());
            assertEquals(3, relations.get(3).getIndex());
            assertEquals(item4.getClientDownloadId(), relations.get(3).getItem().getClientDownloadId());
            assertEquals(4, relations.get(4).getIndex());
            assertEquals(item3.getClientDownloadId(), relations.get(4).getItem().getClientDownloadId());
        }

        // middle to last
        collection.reorderItemIndex(1, 4);

        assertEquals(5, collection.size());
        assertEquals(item1.getClientDownloadId(), collection.getItem(0).getClientDownloadId());
        assertEquals(item2.getClientDownloadId(), collection.getItem(1).getClientDownloadId());
        assertEquals(item4.getClientDownloadId(), collection.getItem(2).getClientDownloadId());
        assertEquals(item3.getClientDownloadId(), collection.getItem(3).getClientDownloadId());
        assertEquals(item0.getClientDownloadId(), collection.getItem(4).getClientDownloadId());

        // check database directly
        {
            List<TalkClientMediaCollectionRelation> relations = findMediaCollectionRelationsOrderedByIndex(collection.getId());
            assertNotNull(relations);
            assertEquals(5, relations.size());
            assertEquals(0, relations.get(0).getIndex());
            assertEquals(item1.getClientDownloadId(), relations.get(0).getItem().getClientDownloadId());
            assertEquals(1, relations.get(1).getIndex());
            assertEquals(item2.getClientDownloadId(), relations.get(1).getItem().getClientDownloadId());
            assertEquals(2, relations.get(2).getIndex());
            assertEquals(item4.getClientDownloadId(), relations.get(2).getItem().getClientDownloadId());
            assertEquals(3, relations.get(3).getIndex());
            assertEquals(item3.getClientDownloadId(), relations.get(3).getItem().getClientDownloadId());
            assertEquals(4, relations.get(4).getIndex());
            assertEquals(item0.getClientDownloadId(), relations.get(4).getItem().getClientDownloadId());
        }

        // middle to first
        collection.reorderItemIndex(2, 0);

        assertEquals(5, collection.size());
        assertEquals(item4.getClientDownloadId(), collection.getItem(0).getClientDownloadId());
        assertEquals(item1.getClientDownloadId(), collection.getItem(1).getClientDownloadId());
        assertEquals(item2.getClientDownloadId(), collection.getItem(2).getClientDownloadId());
        assertEquals(item3.getClientDownloadId(), collection.getItem(3).getClientDownloadId());
        assertEquals(item0.getClientDownloadId(), collection.getItem(4).getClientDownloadId());

        // check database directly
        {
            List<TalkClientMediaCollectionRelation> relations = findMediaCollectionRelationsOrderedByIndex(collection.getId());
            assertNotNull(relations);
            assertEquals(5, relations.size());
            assertEquals(0, relations.get(0).getIndex());
            assertEquals(item4.getClientDownloadId(), relations.get(0).getItem().getClientDownloadId());
            assertEquals(1, relations.get(1).getIndex());
            assertEquals(item1.getClientDownloadId(), relations.get(1).getItem().getClientDownloadId());
            assertEquals(2, relations.get(2).getIndex());
            assertEquals(item2.getClientDownloadId(), relations.get(2).getItem().getClientDownloadId());
            assertEquals(3, relations.get(3).getIndex());
            assertEquals(item3.getClientDownloadId(), relations.get(3).getItem().getClientDownloadId());
            assertEquals(4, relations.get(4).getIndex());
            assertEquals(item0.getClientDownloadId(), relations.get(4).getItem().getClientDownloadId());
        }

        // first to last
        collection.reorderItemIndex(0, 4);

        assertEquals(5, collection.size());
        assertEquals(item1.getClientDownloadId(), collection.getItem(0).getClientDownloadId());
        assertEquals(item2.getClientDownloadId(), collection.getItem(1).getClientDownloadId());
        assertEquals(item3.getClientDownloadId(), collection.getItem(2).getClientDownloadId());
        assertEquals(item0.getClientDownloadId(), collection.getItem(3).getClientDownloadId());
        assertEquals(item4.getClientDownloadId(), collection.getItem(4).getClientDownloadId());

        // check database directly
        {
            List<TalkClientMediaCollectionRelation> relations = findMediaCollectionRelationsOrderedByIndex(collection.getId());
            assertNotNull(relations);
            assertEquals(5, relations.size());
            assertEquals(0, relations.get(0).getIndex());
            assertEquals(item1.getClientDownloadId(), relations.get(0).getItem().getClientDownloadId());
            assertEquals(1, relations.get(1).getIndex());
            assertEquals(item2.getClientDownloadId(), relations.get(1).getItem().getClientDownloadId());
            assertEquals(2, relations.get(2).getIndex());
            assertEquals(item3.getClientDownloadId(), relations.get(2).getItem().getClientDownloadId());
            assertEquals(3, relations.get(3).getIndex());
            assertEquals(item0.getClientDownloadId(), relations.get(3).getItem().getClientDownloadId());
            assertEquals(4, relations.get(4).getIndex());
            assertEquals(item4.getClientDownloadId(), relations.get(4).getItem().getClientDownloadId());
        }

        // move to same index
        collection.reorderItemIndex(2, 2);

        assertEquals(5, collection.size());
        assertEquals(item1.getClientDownloadId(), collection.getItem(0).getClientDownloadId());
        assertEquals(item2.getClientDownloadId(), collection.getItem(1).getClientDownloadId());
        assertEquals(item3.getClientDownloadId(), collection.getItem(2).getClientDownloadId());
        assertEquals(item0.getClientDownloadId(), collection.getItem(3).getClientDownloadId());
        assertEquals(item4.getClientDownloadId(), collection.getItem(4).getClientDownloadId());

        // check database directly
        {
            List<TalkClientMediaCollectionRelation> relations = findMediaCollectionRelationsOrderedByIndex(collection.getId());
            assertNotNull(relations);
            assertEquals(5, relations.size());
            assertEquals(0, relations.get(0).getIndex());
            assertEquals(item1.getClientDownloadId(), relations.get(0).getItem().getClientDownloadId());
            assertEquals(1, relations.get(1).getIndex());
            assertEquals(item2.getClientDownloadId(), relations.get(1).getItem().getClientDownloadId());
            assertEquals(2, relations.get(2).getIndex());
            assertEquals(item3.getClientDownloadId(), relations.get(2).getItem().getClientDownloadId());
            assertEquals(3, relations.get(3).getIndex());
            assertEquals(item0.getClientDownloadId(), relations.get(3).getItem().getClientDownloadId());
            assertEquals(4, relations.get(4).getIndex());
            assertEquals(item4.getClientDownloadId(), relations.get(4).getItem().getClientDownloadId());
        }

        // move item to invalid index and expect IndexOutOfBoundsExceptions and no changes in MediaCollection and database
        boolean exceptionThrown = false;
        try {
            collection.reorderItemIndex(1, -1);
        } catch(IndexOutOfBoundsException e) {
            exceptionThrown = true;
        }

        assertTrue(exceptionThrown);

        assertEquals(5, collection.size());
        assertEquals(item1.getClientDownloadId(), collection.getItem(0).getClientDownloadId());
        assertEquals(item2.getClientDownloadId(), collection.getItem(1).getClientDownloadId());
        assertEquals(item3.getClientDownloadId(), collection.getItem(2).getClientDownloadId());
        assertEquals(item0.getClientDownloadId(), collection.getItem(3).getClientDownloadId());
        assertEquals(item4.getClientDownloadId(), collection.getItem(4).getClientDownloadId());

        // check database directly
        {
            List<TalkClientMediaCollectionRelation> relations = findMediaCollectionRelationsOrderedByIndex(collection.getId());
            assertNotNull(relations);
            assertEquals(5, relations.size());
            assertEquals(0, relations.get(0).getIndex());
            assertEquals(item1.getClientDownloadId(), relations.get(0).getItem().getClientDownloadId());
            assertEquals(1, relations.get(1).getIndex());
            assertEquals(item2.getClientDownloadId(), relations.get(1).getItem().getClientDownloadId());
            assertEquals(2, relations.get(2).getIndex());
            assertEquals(item3.getClientDownloadId(), relations.get(2).getItem().getClientDownloadId());
            assertEquals(3, relations.get(3).getIndex());
            assertEquals(item0.getClientDownloadId(), relations.get(3).getItem().getClientDownloadId());
            assertEquals(4, relations.get(4).getIndex());
            assertEquals(item4.getClientDownloadId(), relations.get(4).getItem().getClientDownloadId());
        }

        // move item to invalid index and expect IndexOutOfBoundsExceptions and no changes in MediaCollection and database
        exceptionThrown = false;
        try {
            collection.reorderItemIndex(3, 6);
        } catch(IndexOutOfBoundsException e) {
            exceptionThrown = true;
        }

        assertTrue(exceptionThrown);

        assertEquals(5, collection.size());
        assertEquals(item1.getClientDownloadId(), collection.getItem(0).getClientDownloadId());
        assertEquals(item2.getClientDownloadId(), collection.getItem(1).getClientDownloadId());
        assertEquals(item3.getClientDownloadId(), collection.getItem(2).getClientDownloadId());
        assertEquals(item0.getClientDownloadId(), collection.getItem(3).getClientDownloadId());
        assertEquals(item4.getClientDownloadId(), collection.getItem(4).getClientDownloadId());

        // check database directly
        {
            List<TalkClientMediaCollectionRelation> relations = findMediaCollectionRelationsOrderedByIndex(collection.getId());
            assertNotNull(relations);
            assertEquals(5, relations.size());
            assertEquals(0, relations.get(0).getIndex());
            assertEquals(item1.getClientDownloadId(), relations.get(0).getItem().getClientDownloadId());
            assertEquals(1, relations.get(1).getIndex());
            assertEquals(item2.getClientDownloadId(), relations.get(1).getItem().getClientDownloadId());
            assertEquals(2, relations.get(2).getIndex());
            assertEquals(item3.getClientDownloadId(), relations.get(2).getItem().getClientDownloadId());
            assertEquals(3, relations.get(3).getIndex());
            assertEquals(item0.getClientDownloadId(), relations.get(3).getItem().getClientDownloadId());
            assertEquals(4, relations.get(4).getIndex());
            assertEquals(item4.getClientDownloadId(), relations.get(4).getItem().getClientDownloadId());
        }
    }

    @Test
    public void testListener() {
        LOG.info("testListener");

        final String collectionName = "testListener_collection";
        TalkClientMediaCollection collection = null;
        TalkClientDownload item0 = new TalkClientDownload();
        TalkClientDownload item1 = new TalkClientDownload();
        TalkClientDownload item2 = new TalkClientDownload();
        TalkClientDownload item3 = new TalkClientDownload();
        TalkClientDownload item4 = new TalkClientDownload();
        TalkClientDownload item5 = new TalkClientDownload();
        try {
            collection = mDatabase.createMediaCollection(collectionName);

            mDatabase.saveClientDownload(item0);
            mDatabase.saveClientDownload(item1);
            mDatabase.saveClientDownload(item2);
            mDatabase.saveClientDownload(item3);
            mDatabase.saveClientDownload(item4);
            mDatabase.saveClientDownload(item5);

            collection.addItem(item0);
            collection.addItem(item1);
            collection.addItem(item2);
            collection.addItem(item3);
            collection.addItem(item4);
        } catch (SQLException e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
            fail();
        }

        final String expectedCollectionName = "newCollectionName";

        final TalkClientDownload expectedItemAdded = item5;
        final int expectedItemAddedIndex = 3;
        final TalkClientDownload expectedItemRemoved = item5;
        final int expectedItemRemovedIndex = 3;
        final int expectedItemOrderFrom = 2;
        final int expectedItemOrderTo = 4;

        final ValueContainer<Boolean> onNameChangedCalled = new ValueContainer<Boolean>(false);
        final ValueContainer<Boolean> onItemOrderChangedCalled = new ValueContainer<Boolean>(false);
        final ValueContainer<Boolean> onItemRemovedCalled = new ValueContainer<Boolean>(false);
        final ValueContainer<Boolean> onItemAddedCalled = new ValueContainer<Boolean>(false);

        TalkClientMediaCollection.Listener listener = new TalkClientMediaCollection.Listener() {
            public void onCollectionNameChanged(TalkClientMediaCollection collection) {
                assertEquals(expectedCollectionName, collection.getName());
                onNameChangedCalled.value = true;
            }
            public void onItemOrderChanged(TalkClientMediaCollection collection, int fromIndex, int toIndex) {
                assertEquals(expectedItemOrderFrom, fromIndex);
                assertEquals(expectedItemOrderTo, toIndex);
                onItemOrderChangedCalled.value = true;
            }
            public void onItemRemoved(TalkClientMediaCollection collection, int indexRemoved, TalkClientDownload itemRemoved) {
                assertEquals(expectedItemRemovedIndex, indexRemoved);
                assertEquals(expectedItemRemoved, itemRemoved);
                onItemRemovedCalled.value = true;
            }
            public void onItemAdded(TalkClientMediaCollection collection, int indexAdded, TalkClientDownload itemAdded) {
                assertEquals(expectedItemAddedIndex, indexAdded);
                assertEquals(expectedItemAdded, itemAdded);
                onItemAddedCalled.value = true;
            }
        };
        collection.registerListener(listener);

        collection.setName(expectedCollectionName);
        collection.addItem(3, expectedItemAdded);
        collection.removeItem(expectedItemRemoved);
        collection.reorderItemIndex(2, 4);

        assertTrue(onNameChangedCalled.value);
        assertTrue(onItemOrderChangedCalled.value);
        assertTrue(onItemRemovedCalled.value);
        assertTrue(onItemAddedCalled.value);

        // test unregister listener
        collection.unregisterListener(listener);

        // reset states
        onNameChangedCalled.value = false;
        onItemOrderChangedCalled.value = false;
        onItemRemovedCalled.value = false;
        onItemAddedCalled.value = false;

        collection.setName(expectedCollectionName);
        collection.addItem(3, expectedItemAdded);
        collection.removeItem(expectedItemRemoved);
        collection.reorderItemIndex(2, 4);

        assertFalse(onNameChangedCalled.value);
        assertFalse(onItemOrderChangedCalled.value);
        assertFalse(onItemRemovedCalled.value);
        assertFalse(onItemAddedCalled.value);
    }

    @Test
    public void testIterator() {
        LOG.info("testIterator");

        final String collectionName = "testIterator_collection";

        TalkClientMediaCollection collection = null;

        int itemCount = 10;
        ArrayList<TalkClientDownload> expectedItemList = new ArrayList<TalkClientDownload>();
        for(int i = 0; i < itemCount; i++) {
            expectedItemList.add(new TalkClientDownload());
        }

        try {
            collection = mDatabase.createMediaCollection(collectionName);

            // create some items and addItem to collection
            for(int i = 0; i < itemCount; i++) {
                mDatabase.saveClientDownload(expectedItemList.get(i));
                collection.addItem(expectedItemList.get(i));
            }
        } catch (SQLException e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
            fail();
        }

        int index = 0;
        for(TalkClientDownload item : collection) {
            assertEquals(expectedItemList.get(index++), item);
        }
    }

    private class ValueContainer<T> {
        public T value;
        public ValueContainer(T initValue) {
            value = initValue;
        }
    }
    
    private List<TalkClientMediaCollectionRelation> findMediaCollectionRelationsOrderedByIndex(int collectionId) {

        List<TalkClientMediaCollectionRelation> relations = null;
        try {
             relations = mDatabase.getMediaCollectionRelationDao().queryBuilder()
                    .orderBy("index", true)
                    .where()
                    .eq("collection_id", collectionId)
                    .query();
        } catch(SQLException e) {
            e.printStackTrace();
        }

        return relations;
    }
}
