package com.hoccer.talk.client.model;

import com.hoccer.talk.client.IXoClientDatabaseBackend;
import com.hoccer.talk.client.IXoMediaCollectionListener;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.XoTransfer;
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

import static junit.framework.TestCase.*;

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
    public void testCreateCollection() {
        LOG.info("testCreateCollection");

        // register MediaCollection listener
        final ValueContainer<Boolean> onMediaCollectionCreatedCalled = new ValueContainer<Boolean>(false);
        final ValueContainer<Boolean> onMediaCollectionDeletedCalled = new ValueContainer<Boolean>(false);
        mDatabase.registerMediaCollectionListener(new IXoMediaCollectionListener() {
            @Override
            public void onMediaCollectionCreated(TalkClientMediaCollection collectionCreated) {
                onMediaCollectionCreatedCalled.value = true;
            }

            @Override
            public void onMediaCollectionDeleted(TalkClientMediaCollection collectionDeleted) {
                onMediaCollectionDeletedCalled.value = true;
            }
        });

        String collectionName = "testCreateCollection_collection";
        TalkClientMediaCollection collection = null;
        try {
            collection = mDatabase.createMediaCollection(collectionName);
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            fail();
        }

        assertNotNull(collection);
        assertEquals(collectionName, collection.getName());
        assertEquals(0, collection.size());

        // listener call check
        assertTrue(onMediaCollectionCreatedCalled.value);
        assertFalse(onMediaCollectionDeletedCalled.value);

        TalkClientMediaCollection collectionCopy = null;
        try {
            collectionCopy = mDatabase.findMediaCollectionById(collection.getId());
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
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
            LOG.error(e.getMessage(), e);
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
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            fail();
        }

        // register MediaCollection listener
        final ValueContainer<Boolean> onMediaCollectionCreatedCalled = new ValueContainer<Boolean>(false);
        final ValueContainer<Boolean> onMediaCollectionDeletedCalled = new ValueContainer<Boolean>(false);
        mDatabase.registerMediaCollectionListener(new IXoMediaCollectionListener() {
            @Override
            public void onMediaCollectionCreated(TalkClientMediaCollection collectionCreated) {
                onMediaCollectionCreatedCalled.value = true;
            }

            @Override
            public void onMediaCollectionDeleted(TalkClientMediaCollection collectionDeleted) {
                onMediaCollectionDeletedCalled.value = true;
            }
        });

        try {
            mDatabase.deleteMediaCollection(collection);
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            fail();
        }

        // listener call check
        assertFalse(onMediaCollectionCreatedCalled.value);
        assertTrue(onMediaCollectionDeletedCalled.value);

        // check database directly
        {
            List<TalkClientMediaCollection> collections = null;
            try {
                collections = mDatabase.findAllMediaCollections();
            } catch (SQLException e) {
                LOG.error(e.getMessage(), e);
                fail();
            }

            assertNotNull(collections);
            assertEquals(0, collections.size());

            List<TalkClientMediaCollectionRelation> relations = findMediaCollectionRelationsOrderedByIndex(mDatabase, collection.getId());
            assertEquals(0, relations.size());
        }
    }

    @Test
    public void testDeleteCollectionById() {
        LOG.info("testDeleteCollectionById");

        String collectionName = "testDeleteCollectionById_collection";
        TalkClientMediaCollection collection = null;
        try {
            collection = mDatabase.createMediaCollection(collectionName);
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            fail();
        }

        // register MediaCollection listener
        final ValueContainer<Boolean> onMediaCollectionCreatedCalled = new ValueContainer<Boolean>(false);
        final ValueContainer<Boolean> onMediaCollectionDeletedCalled = new ValueContainer<Boolean>(false);
        mDatabase.registerMediaCollectionListener(new IXoMediaCollectionListener() {
            @Override
            public void onMediaCollectionCreated(TalkClientMediaCollection collectionCreated) {
                onMediaCollectionCreatedCalled.value = true;
            }

            @Override
            public void onMediaCollectionDeleted(TalkClientMediaCollection collectionDeleted) {
                onMediaCollectionDeletedCalled.value = true;
            }
        });

        try {
            mDatabase.deleteMediaCollectionById(collection.getId());
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            fail();
        }

        // listener call check
        assertFalse(onMediaCollectionCreatedCalled.value);
        assertTrue(onMediaCollectionDeletedCalled.value);

        // check database directly
        {
            List<TalkClientMediaCollection> collections = null;
            try {
                collections = mDatabase.findAllMediaCollections();
            } catch (SQLException e) {
                LOG.error(e.getMessage(), e);
                fail();
            }

            assertNotNull(collections);
            assertEquals(0, collections.size());

            List<TalkClientMediaCollectionRelation> relations = findMediaCollectionRelationsOrderedByIndex(mDatabase, collection.getId());
            assertEquals(0, relations.size());
        }
    }

    @Test
    public void testCreateCollectionFromDatabase() {
        LOG.info("testCreateCollectionFromDatabase");

        String collectionName = "testCreateCollectionFromDatabase";
        List<XoTransfer> items = createDownloadItems(mDatabase, 5);

        TalkClientMediaCollection collection = null;
        try {
            collection = mDatabase.createMediaCollection(collectionName);
            addItemsToCollection(items, collection);
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            fail();
        }


        TalkClientMediaCollection collectionCopy = null;
        try {
            List<TalkClientMediaCollection> collections = mDatabase.findMediaCollectionsByName(collectionName);

            assertEquals(1, collections.size());
            collectionCopy = collections.get(0);
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            fail();
        }

        validateItemsInCollection(items, collectionCopy);
    }

    @Test
    public void testAddItems() {
        LOG.info("testAddItems");

        String collectionName = "testAddItems_collection";
        int itemCount = 5;
        List<XoTransfer> items = createDownloadItems(mDatabase, itemCount);

        TalkClientMediaCollection collection = null;
        try {
            collection = mDatabase.createMediaCollection(collectionName);
            addItemsToCollection(items, collection);
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            fail();
        }

        validateItemsInCollection(items, collection);

        // check database directly
        List<TalkClientMediaCollectionRelation> relations = findMediaCollectionRelationsOrderedByIndex(mDatabase, collection.getId());
        assertNotNull(relations);
        assertEquals(itemCount, relations.size());
        for (int i = 0; i < itemCount; i++) {
            assertEquals(i, relations.get(i).getIndex());
            assertEquals(items.get(i).getTransferId(), relations.get(i).getTransferItem().getTransferId());
        }
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
            collection.addItem(0, item0); // order: 0
            collection.addItem(1, item1); // order: 0 1
            collection.addItem(0, item2); // order: 2 0 1
            collection.addItem(1, item3); // order: 2 3 0 1
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            fail();
        }

        assertEquals(4, collection.size());
        assertEquals(item2.getTransferId(), collection.getItem(0).getTransferId());
        assertEquals(item3.getTransferId(), collection.getItem(1).getTransferId());
        assertEquals(item0.getTransferId(), collection.getItem(2).getTransferId());
        assertEquals(item1.getTransferId(), collection.getItem(3).getTransferId());

        // check database directly
        List<TalkClientMediaCollectionRelation> relations = findMediaCollectionRelationsOrderedByIndex(mDatabase, collection.getId());
        assertNotNull(relations);
        assertEquals(4, relations.size());
        assertEquals(0, relations.get(0).getIndex());
        assertEquals(item2.getTransferId(), relations.get(0).getTransferItem().getTransferId());
        assertEquals(1, relations.get(1).getIndex());
        assertEquals(item3.getTransferId(), relations.get(1).getTransferItem().getTransferId());
        assertEquals(2, relations.get(2).getIndex());
        assertEquals(item0.getTransferId(), relations.get(2).getTransferItem().getTransferId());
        assertEquals(3, relations.get(3).getIndex());
        assertEquals(item1.getTransferId(), relations.get(3).getTransferItem().getTransferId());
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
            LOG.error(e.getMessage(), e);
            fail();
        }

        assertEquals(4, collection.size());
        assertEquals(item0.getTransferId(), collection.getItem(0).getTransferId());
        assertEquals(item1.getTransferId(), collection.getItem(1).getTransferId());
        assertEquals(item2.getTransferId(), collection.getItem(2).getTransferId());
        assertEquals(item3.getTransferId(), collection.getItem(3).getTransferId());

        // check database directly
        {
            List<TalkClientMediaCollectionRelation> relations = findMediaCollectionRelationsOrderedByIndex(mDatabase, collection.getId());
            assertNotNull(relations);
            assertEquals(4, relations.size());
            assertEquals(0, relations.get(0).getIndex());
            assertEquals(item0.getTransferId(), relations.get(0).getTransferItem().getTransferId());
            assertEquals(1, relations.get(1).getIndex());
            assertEquals(item1.getTransferId(), relations.get(1).getTransferItem().getTransferId());
            assertEquals(2, relations.get(2).getIndex());
            assertEquals(item2.getTransferId(), relations.get(2).getTransferItem().getTransferId());
            assertEquals(3, relations.get(3).getIndex());
            assertEquals(item3.getTransferId(), relations.get(3).getTransferItem().getTransferId());
        }

        collection.removeItem(1);

        assertEquals(3, collection.size());
        assertEquals(item0.getTransferId(), collection.getItem(0).getTransferId());
        assertEquals(item2.getTransferId(), collection.getItem(1).getTransferId());
        assertEquals(item3.getTransferId(), collection.getItem(2).getTransferId());

        // check database directly
        {
            List<TalkClientMediaCollectionRelation> relations = findMediaCollectionRelationsOrderedByIndex(mDatabase, collection.getId());
            assertNotNull(relations);
            assertEquals(3, relations.size());
            assertEquals(0, relations.get(0).getIndex());
            assertEquals(item0.getTransferId(), relations.get(0).getTransferItem().getTransferId());
            assertEquals(1, relations.get(1).getIndex());
            assertEquals(item2.getTransferId(), relations.get(1).getTransferItem().getTransferId());
            assertEquals(2, relations.get(2).getIndex());
            assertEquals(item3.getTransferId(), relations.get(2).getTransferItem().getTransferId());
        }

        collection.removeItem(item3);

        assertEquals(2, collection.size());
        assertEquals(item0.getTransferId(), collection.getItem(0).getTransferId());
        assertEquals(item2.getTransferId(), collection.getItem(1).getTransferId());

        // check database directly
        {
            List<TalkClientMediaCollectionRelation> relations = findMediaCollectionRelationsOrderedByIndex(mDatabase, collection.getId());
            assertNotNull(relations);
            assertEquals(2, relations.size());
            assertEquals(0, relations.get(0).getIndex());
            assertEquals(item0.getTransferId(), relations.get(0).getTransferItem().getTransferId());
            assertEquals(1, relations.get(1).getIndex());
            assertEquals(item2.getTransferId(), relations.get(1).getTransferItem().getTransferId());
        }

        // removeItem it again, nothing should change
        collection.removeItem(item3);

        assertEquals(2, collection.size());
        assertEquals(item0.getTransferId(), collection.getItem(0).getTransferId());
        assertEquals(item2.getTransferId(), collection.getItem(1).getTransferId());

        // check database directly
        {
            List<TalkClientMediaCollectionRelation> relations = findMediaCollectionRelationsOrderedByIndex(mDatabase, collection.getId());
            assertNotNull(relations);
            assertEquals(2, relations.size());
            assertEquals(0, relations.get(0).getIndex());
            assertEquals(item0.getTransferId(), relations.get(0).getTransferItem().getTransferId());
            assertEquals(1, relations.get(1).getIndex());
            assertEquals(item2.getTransferId(), relations.get(1).getTransferItem().getTransferId());
        }
    }

    @Test
    public void testClearCollection() {
        LOG.info("testClearCollection");

        String collectionName = "testClearCollection_collection";

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
        collection.clear();

        assertEquals(0, collection.size());
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
            LOG.error(e.getMessage(), e);
            fail();
        }

        assertEquals(5, collection.size());
        assertEquals(item0.getTransferId(), collection.getItem(0).getTransferId());
        assertEquals(item1.getTransferId(), collection.getItem(1).getTransferId());
        assertEquals(item2.getTransferId(), collection.getItem(2).getTransferId());
        assertEquals(item3.getTransferId(), collection.getItem(3).getTransferId());
        assertEquals(item4.getTransferId(), collection.getItem(4).getTransferId());

        // check database directly
        {
            List<TalkClientMediaCollectionRelation> relations = findMediaCollectionRelationsOrderedByIndex(mDatabase, collection.getId());
            assertNotNull(relations);
            assertEquals(5, relations.size());
            assertEquals(0, relations.get(0).getIndex());
            assertEquals(item0.getTransferId(), relations.get(0).getTransferItem().getTransferId());
            assertEquals(1, relations.get(1).getIndex());
            assertEquals(item1.getTransferId(), relations.get(1).getTransferItem().getTransferId());
            assertEquals(2, relations.get(2).getIndex());
            assertEquals(item2.getTransferId(), relations.get(2).getTransferItem().getTransferId());
            assertEquals(3, relations.get(3).getIndex());
            assertEquals(item3.getTransferId(), relations.get(3).getTransferItem().getTransferId());
            assertEquals(4, relations.get(4).getIndex());
            assertEquals(item4.getTransferId(), relations.get(4).getTransferItem().getTransferId());
        }

        // last to middle
        collection.reorderItemIndex(4, 3);

        assertEquals(5, collection.size());
        assertEquals(item0.getTransferId(), collection.getItem(0).getTransferId());
        assertEquals(item1.getTransferId(), collection.getItem(1).getTransferId());
        assertEquals(item2.getTransferId(), collection.getItem(2).getTransferId());
        assertEquals(item4.getTransferId(), collection.getItem(3).getTransferId());
        assertEquals(item3.getTransferId(), collection.getItem(4).getTransferId());

        // check database directly
        {
            List<TalkClientMediaCollectionRelation> relations = findMediaCollectionRelationsOrderedByIndex(mDatabase, collection.getId());
            assertNotNull(relations);
            assertEquals(5, relations.size());
            assertEquals(0, relations.get(0).getIndex());
            assertEquals(item0.getTransferId(), relations.get(0).getTransferItem().getTransferId());
            assertEquals(1, relations.get(1).getIndex());
            assertEquals(item1.getTransferId(), relations.get(1).getTransferItem().getTransferId());
            assertEquals(2, relations.get(2).getIndex());
            assertEquals(item2.getTransferId(), relations.get(2).getTransferItem().getTransferId());
            assertEquals(3, relations.get(3).getIndex());
            assertEquals(item4.getTransferId(), relations.get(3).getTransferItem().getTransferId());
            assertEquals(4, relations.get(4).getIndex());
            assertEquals(item3.getTransferId(), relations.get(4).getTransferItem().getTransferId());
        }

        // first to middle
        collection.reorderItemIndex(0, 1);

        assertEquals(5, collection.size());
        assertEquals(item1.getTransferId(), collection.getItem(0).getTransferId());
        assertEquals(item0.getTransferId(), collection.getItem(1).getTransferId());
        assertEquals(item2.getTransferId(), collection.getItem(2).getTransferId());
        assertEquals(item4.getTransferId(), collection.getItem(3).getTransferId());
        assertEquals(item3.getTransferId(), collection.getItem(4).getTransferId());

        // check database directly
        {
            List<TalkClientMediaCollectionRelation> relations = findMediaCollectionRelationsOrderedByIndex(mDatabase, collection.getId());
            assertNotNull(relations);
            assertEquals(5, relations.size());
            assertEquals(0, relations.get(0).getIndex());
            assertEquals(item1.getTransferId(), relations.get(0).getTransferItem().getTransferId());
            assertEquals(1, relations.get(1).getIndex());
            assertEquals(item0.getTransferId(), relations.get(1).getTransferItem().getTransferId());
            assertEquals(2, relations.get(2).getIndex());
            assertEquals(item2.getTransferId(), relations.get(2).getTransferItem().getTransferId());
            assertEquals(3, relations.get(3).getIndex());
            assertEquals(item4.getTransferId(), relations.get(3).getTransferItem().getTransferId());
            assertEquals(4, relations.get(4).getIndex());
            assertEquals(item3.getTransferId(), relations.get(4).getTransferItem().getTransferId());
        }

        // middle to last
        collection.reorderItemIndex(1, 4);

        assertEquals(5, collection.size());
        assertEquals(item1.getTransferId(), collection.getItem(0).getTransferId());
        assertEquals(item2.getTransferId(), collection.getItem(1).getTransferId());
        assertEquals(item4.getTransferId(), collection.getItem(2).getTransferId());
        assertEquals(item3.getTransferId(), collection.getItem(3).getTransferId());
        assertEquals(item0.getTransferId(), collection.getItem(4).getTransferId());

        // check database directly
        {
            List<TalkClientMediaCollectionRelation> relations = findMediaCollectionRelationsOrderedByIndex(mDatabase, collection.getId());
            assertNotNull(relations);
            assertEquals(5, relations.size());
            assertEquals(0, relations.get(0).getIndex());
            assertEquals(item1.getTransferId(), relations.get(0).getTransferItem().getTransferId());
            assertEquals(1, relations.get(1).getIndex());
            assertEquals(item2.getTransferId(), relations.get(1).getTransferItem().getTransferId());
            assertEquals(2, relations.get(2).getIndex());
            assertEquals(item4.getTransferId(), relations.get(2).getTransferItem().getTransferId());
            assertEquals(3, relations.get(3).getIndex());
            assertEquals(item3.getTransferId(), relations.get(3).getTransferItem().getTransferId());
            assertEquals(4, relations.get(4).getIndex());
            assertEquals(item0.getTransferId(), relations.get(4).getTransferItem().getTransferId());
        }

        // middle to first
        collection.reorderItemIndex(2, 0);

        assertEquals(5, collection.size());
        assertEquals(item4.getTransferId(), collection.getItem(0).getTransferId());
        assertEquals(item1.getTransferId(), collection.getItem(1).getTransferId());
        assertEquals(item2.getTransferId(), collection.getItem(2).getTransferId());
        assertEquals(item3.getTransferId(), collection.getItem(3).getTransferId());
        assertEquals(item0.getTransferId(), collection.getItem(4).getTransferId());

        // check database directly
        {
            List<TalkClientMediaCollectionRelation> relations = findMediaCollectionRelationsOrderedByIndex(mDatabase, collection.getId());
            assertNotNull(relations);
            assertEquals(5, relations.size());
            assertEquals(0, relations.get(0).getIndex());
            assertEquals(item4.getTransferId(), relations.get(0).getTransferItem().getTransferId());
            assertEquals(1, relations.get(1).getIndex());
            assertEquals(item1.getTransferId(), relations.get(1).getTransferItem().getTransferId());
            assertEquals(2, relations.get(2).getIndex());
            assertEquals(item2.getTransferId(), relations.get(2).getTransferItem().getTransferId());
            assertEquals(3, relations.get(3).getIndex());
            assertEquals(item3.getTransferId(), relations.get(3).getTransferItem().getTransferId());
            assertEquals(4, relations.get(4).getIndex());
            assertEquals(item0.getTransferId(), relations.get(4).getTransferItem().getTransferId());
        }

        // first to last
        collection.reorderItemIndex(0, 4);

        assertEquals(5, collection.size());
        assertEquals(item1.getTransferId(), collection.getItem(0).getTransferId());
        assertEquals(item2.getTransferId(), collection.getItem(1).getTransferId());
        assertEquals(item3.getTransferId(), collection.getItem(2).getTransferId());
        assertEquals(item0.getTransferId(), collection.getItem(3).getTransferId());
        assertEquals(item4.getTransferId(), collection.getItem(4).getTransferId());

        // check database directly
        {
            List<TalkClientMediaCollectionRelation> relations = findMediaCollectionRelationsOrderedByIndex(mDatabase, collection.getId());
            assertNotNull(relations);
            assertEquals(5, relations.size());
            assertEquals(0, relations.get(0).getIndex());
            assertEquals(item1.getTransferId(), relations.get(0).getTransferItem().getTransferId());
            assertEquals(1, relations.get(1).getIndex());
            assertEquals(item2.getTransferId(), relations.get(1).getTransferItem().getTransferId());
            assertEquals(2, relations.get(2).getIndex());
            assertEquals(item3.getTransferId(), relations.get(2).getTransferItem().getTransferId());
            assertEquals(3, relations.get(3).getIndex());
            assertEquals(item0.getTransferId(), relations.get(3).getTransferItem().getTransferId());
            assertEquals(4, relations.get(4).getIndex());
            assertEquals(item4.getTransferId(), relations.get(4).getTransferItem().getTransferId());
        }

        // move to same index
        collection.reorderItemIndex(2, 2);

        assertEquals(5, collection.size());
        assertEquals(item1.getTransferId(), collection.getItem(0).getTransferId());
        assertEquals(item2.getTransferId(), collection.getItem(1).getTransferId());
        assertEquals(item3.getTransferId(), collection.getItem(2).getTransferId());
        assertEquals(item0.getTransferId(), collection.getItem(3).getTransferId());
        assertEquals(item4.getTransferId(), collection.getItem(4).getTransferId());

        // check database directly
        {
            List<TalkClientMediaCollectionRelation> relations = findMediaCollectionRelationsOrderedByIndex(mDatabase, collection.getId());
            assertNotNull(relations);
            assertEquals(5, relations.size());
            assertEquals(0, relations.get(0).getIndex());
            assertEquals(item1.getTransferId(), relations.get(0).getTransferItem().getTransferId());
            assertEquals(1, relations.get(1).getIndex());
            assertEquals(item2.getTransferId(), relations.get(1).getTransferItem().getTransferId());
            assertEquals(2, relations.get(2).getIndex());
            assertEquals(item3.getTransferId(), relations.get(2).getTransferItem().getTransferId());
            assertEquals(3, relations.get(3).getIndex());
            assertEquals(item0.getTransferId(), relations.get(3).getTransferItem().getTransferId());
            assertEquals(4, relations.get(4).getIndex());
            assertEquals(item4.getTransferId(), relations.get(4).getTransferItem().getTransferId());
        }

        // move item to invalid index and expect IndexOutOfBoundsExceptions and no changes in MediaCollection and database
        boolean exceptionThrown = false;
        try {
            collection.reorderItemIndex(1, -1);
        } catch (IndexOutOfBoundsException e) {
            exceptionThrown = true;
        }

        assertTrue(exceptionThrown);

        assertEquals(5, collection.size());
        assertEquals(item1.getTransferId(), collection.getItem(0).getTransferId());
        assertEquals(item2.getTransferId(), collection.getItem(1).getTransferId());
        assertEquals(item3.getTransferId(), collection.getItem(2).getTransferId());
        assertEquals(item0.getTransferId(), collection.getItem(3).getTransferId());
        assertEquals(item4.getTransferId(), collection.getItem(4).getTransferId());

        // check database directly
        {
            List<TalkClientMediaCollectionRelation> relations = findMediaCollectionRelationsOrderedByIndex(mDatabase, collection.getId());
            assertNotNull(relations);
            assertEquals(5, relations.size());
            assertEquals(0, relations.get(0).getIndex());
            assertEquals(item1.getTransferId(), relations.get(0).getTransferItem().getTransferId());
            assertEquals(1, relations.get(1).getIndex());
            assertEquals(item2.getTransferId(), relations.get(1).getTransferItem().getTransferId());
            assertEquals(2, relations.get(2).getIndex());
            assertEquals(item3.getTransferId(), relations.get(2).getTransferItem().getTransferId());
            assertEquals(3, relations.get(3).getIndex());
            assertEquals(item0.getTransferId(), relations.get(3).getTransferItem().getTransferId());
            assertEquals(4, relations.get(4).getIndex());
            assertEquals(item4.getTransferId(), relations.get(4).getTransferItem().getTransferId());
        }

        // move item to invalid index and expect IndexOutOfBoundsExceptions and no changes in MediaCollection and database
        exceptionThrown = false;
        try {
            collection.reorderItemIndex(3, 6);
        } catch (IndexOutOfBoundsException e) {
            exceptionThrown = true;
        }

        assertTrue(exceptionThrown);

        assertEquals(5, collection.size());
        assertEquals(item1.getTransferId(), collection.getItem(0).getTransferId());
        assertEquals(item2.getTransferId(), collection.getItem(1).getTransferId());
        assertEquals(item3.getTransferId(), collection.getItem(2).getTransferId());
        assertEquals(item0.getTransferId(), collection.getItem(3).getTransferId());
        assertEquals(item4.getTransferId(), collection.getItem(4).getTransferId());

        // check database directly
        {
            List<TalkClientMediaCollectionRelation> relations = findMediaCollectionRelationsOrderedByIndex(mDatabase, collection.getId());
            assertNotNull(relations);
            assertEquals(5, relations.size());
            assertEquals(0, relations.get(0).getIndex());
            assertEquals(item1.getTransferId(), relations.get(0).getTransferItem().getTransferId());
            assertEquals(1, relations.get(1).getIndex());
            assertEquals(item2.getTransferId(), relations.get(1).getTransferItem().getTransferId());
            assertEquals(2, relations.get(2).getIndex());
            assertEquals(item3.getTransferId(), relations.get(2).getTransferItem().getTransferId());
            assertEquals(3, relations.get(3).getIndex());
            assertEquals(item0.getTransferId(), relations.get(3).getTransferItem().getTransferId());
            assertEquals(4, relations.get(4).getIndex());
            assertEquals(item4.getTransferId(), relations.get(4).getTransferItem().getTransferId());
        }
    }

    @Test
    public void testRegisterListener() {
        LOG.info("testRegisterListener");

        final String collectionName = "testRegisterListener_collection";
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
            LOG.error(e.getMessage(), e);
            fail();
        }

        final String expectedCollectionName = "newCollectionName";

        final TalkClientDownload expectedItemAdded = item5;
        final int expectedItemAddedIndex = 3;
        final TalkClientDownload expectedItemRemoved = item5;
        final int expectedItemRemovedIndex = 3;
        final int expectedItemOrderFrom = 2;
        final int expectedItemOrderTo = 4;

        ListenerTester listenerTester = new ListenerTester(collection);

        collection.setName(expectedCollectionName);
        collection.addItem(3, expectedItemAdded);
        collection.removeItem(expectedItemRemoved);
        collection.reorderItemIndex(2, 4);
        collection.clear();

        assertEquals(1, listenerTester.onNameChangedCalled.size());
        assertEquals(collection, listenerTester.onNameChangedCalled.get(0).args[0]);
        assertEquals(expectedCollectionName, ((TalkClientMediaCollection) listenerTester.onNameChangedCalled.get(0).args[0]).getName());

        assertEquals(1, listenerTester.onItemOrderChangedCalled.size());
        assertEquals(collection, listenerTester.onItemOrderChangedCalled.get(0).args[0]);
        assertEquals(expectedItemOrderFrom, listenerTester.onItemOrderChangedCalled.get(0).args[1]);
        assertEquals(expectedItemOrderTo, listenerTester.onItemOrderChangedCalled.get(0).args[2]);

        assertEquals(1, listenerTester.onItemRemovedCalled.size()); // callback is only invoked if MediaCollection instances are cached
        assertEquals(collection, listenerTester.onItemRemovedCalled.get(0).args[0]);
        assertEquals(expectedItemRemovedIndex, listenerTester.onItemRemovedCalled.get(0).args[1]);
        assertEquals(expectedItemRemoved, listenerTester.onItemRemovedCalled.get(0).args[2]);

        assertEquals(1, listenerTester.onItemAddedCalled.size());
        assertEquals(collection, listenerTester.onItemAddedCalled.get(0).args[0]);
        assertEquals(expectedItemAddedIndex, listenerTester.onItemAddedCalled.get(0).args[1]);
        assertEquals(expectedItemAdded, listenerTester.onItemAddedCalled.get(0).args[2]);

        assertEquals(1, listenerTester.onCollectionClearedCalled.size());
        assertEquals(collection, listenerTester.onItemAddedCalled.get(0).args[0]);
    }

    @Test
    public void testUnregisterListener() {
        LOG.info("testUnregisterListener");

        final String collectionName = "testUnregisterListener_collection";
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
            LOG.error(e.getMessage(), e);
            fail();
        }

        ListenerTester listenerTester = new ListenerTester(collection);
        listenerTester.unregister();

        collection.setName("newName");
        collection.addItem(3, item5);
        collection.removeItem(item5);
        collection.reorderItemIndex(2, 4);
        collection.clear();

        assertEquals(0, listenerTester.onNameChangedCalled.size());
        assertEquals(0, listenerTester.onItemOrderChangedCalled.size());
        assertEquals(0, listenerTester.onItemRemovedCalled.size()); // callback is only invoked if MediaCollection instances are cached
        assertEquals(0, listenerTester.onItemAddedCalled.size());
        assertEquals(0, listenerTester.onCollectionClearedCalled.size());
    }

    @Test
    public void testIterator() {
        LOG.info("testIterator");

        final String collectionName = "testIterator_collection";

        TalkClientMediaCollection collection = null;

        int itemCount = 10;
        ArrayList<TalkClientDownload> expectedItemList = new ArrayList<TalkClientDownload>();
        for (int i = 0; i < itemCount; i++) {
            expectedItemList.add(new TalkClientDownload());
        }

        try {
            collection = mDatabase.createMediaCollection(collectionName);

            // create some items and addItem to collection
            for (int i = 0; i < itemCount; i++) {
                mDatabase.saveClientDownload(expectedItemList.get(i));
                collection.addItem(expectedItemList.get(i));
            }
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            fail();
        }

        int index = 0;
        for (XoTransfer item : collection) {
            assertEquals(expectedItemList.get(index++), item);
        }

        // test remove element via iterator
        index = 0;
        int removeIndex = 2;
        for (Iterator<XoTransfer> iterator = collection.iterator();
             iterator.hasNext(); ) {
            if (index == removeIndex) {
                iterator.remove();
                break;
            }
            iterator.next();
            index++;
        }

        // expect item has been removed
        expectedItemList.remove(removeIndex);

        index = 0;
        for (XoTransfer item : collection) {
            assertEquals(expectedItemList.get(index++), item);
        }
    }

    @Test
    public void testDeleteDownload() {
        LOG.info("testDeleteDownload");

        final String collectionName = "testDeleteDownload_collection";
        TalkClientMediaCollection collection = null;
        TalkClientDownload item0 = new TalkClientDownload();
        TalkClientDownload item1 = new TalkClientDownload();
        TalkClientDownload item2 = new TalkClientDownload();
        try {
            collection = mDatabase.createMediaCollection(collectionName);

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

        ListenerTester listenerTester = new ListenerTester(collection);

        final TalkClientDownload expectedItemRemoved = item1;
        final int expectedItemRemovedIndex = 1;

        try {
            mDatabase.deleteClientDownload(expectedItemRemoved);
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            fail();
        }

        assertEquals(0, listenerTester.onNameChangedCalled.size());
        assertEquals(0, listenerTester.onItemOrderChangedCalled.size());
        assertEquals(1, listenerTester.onItemRemovedCalled.size()); // callback is only invoked if MediaCollection instances are cached
        assertEquals(collection, listenerTester.onItemRemovedCalled.get(0).args[0]);
        assertEquals(expectedItemRemovedIndex, listenerTester.onItemRemovedCalled.get(0).args[1]);
        assertEquals(expectedItemRemoved, listenerTester.onItemRemovedCalled.get(0).args[2]);
        assertEquals(0, listenerTester.onItemAddedCalled.size());
        assertEquals(0, listenerTester.onCollectionClearedCalled.size());
    }

    @Test
    public void testGetItems() {
        LOG.info("testGetItems");

        final String collectionName = "testGetItems_collection";
        TalkClientMediaCollection collection = null;

        int itemCount = 10;
        ArrayList<TalkClientDownload> expectedItemList = new ArrayList<TalkClientDownload>();
        for (int i = 0; i < itemCount; i++) {
            expectedItemList.add(new TalkClientDownload());
        }
        try {
            collection = mDatabase.createMediaCollection(collectionName);

            for (TalkClientDownload item : expectedItemList) {
                mDatabase.saveClientDownload(item);
                collection.addItem(item);
            }
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            fail();
        }

        List<XoTransfer> itemList = collection.getItems();

        assertEquals(expectedItemList.size(), itemList.size());

        int index = 0;
        for (TalkClientDownload item : expectedItemList) {
            assertEquals(item, itemList.get(index++));
        }
    }

    ////////////////////////////////
    //////// Helper methods ////////
    ////////////////////////////////

    private static List<XoTransfer> createDownloadItems(XoClientDatabase database, int count) {
        ArrayList<XoTransfer> list = new ArrayList<XoTransfer>();

        try {
            for (int i = 0; i < count; i++) {
                TalkClientDownload item = new TalkClientDownload();

                // set unique value on data field to check serialization
                item.setContentHmac(String.valueOf(i));
                database.saveClientDownload(item);
                list.add(item);
            }
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            fail();
        }

        return list;
    }

    private static void addItemsToCollection(List<XoTransfer> items, TalkClientMediaCollection collection) {
        for (int i = 0; i < items.size(); i++) {
            collection.addItem(items.get(i));
        }
    }

    private static void validateItemsInCollection(List<XoTransfer> expectedItems, TalkClientMediaCollection collection) {
        assertEquals(expectedItems.size(), collection.size());

        for (int i = 0; i < expectedItems.size(); i++) {
            XoTransfer expectedItem = expectedItems.get(i);
            XoTransfer actualItem = collection.getItem(i);

            assertEquals(expectedItem.getTransferId(), actualItem.getTransferId());

            // check data field to see if it was correctly serialized
            assertEquals(expectedItem.getContentHmac(), actualItem.getContentHmac());
        }
    }

    private class ValueContainer<T> {
        public T value;

        public ValueContainer(T initValue) {
            value = initValue;
        }
    }

    private static List<TalkClientMediaCollectionRelation> findMediaCollectionRelationsOrderedByIndex(XoClientDatabase database, int collectionId) {

        List<TalkClientMediaCollectionRelation> relations = null;
        try {
            relations = database.getMediaCollectionRelationDao().queryBuilder()
                    .orderBy("index", true)
                    .where()
                    .eq("collection_id", collectionId)
                    .query();
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            fail();
        }

        return relations;
    }

    public class Call {
        public Object[] args;

        public Call(Object... args) {
            this.args = args;
        }
    }

    // logs listener calls
    private class ListenerTester {
        private TalkClientMediaCollection mCollection;
        private TalkClientMediaCollection.Listener mListener;

        public ArrayList<Call> onNameChangedCalled = new ArrayList<Call>();
        public ArrayList<Call> onItemOrderChangedCalled = new ArrayList<Call>();
        public ArrayList<Call> onItemRemovedCalled = new ArrayList<Call>();
        public ArrayList<Call> onItemAddedCalled = new ArrayList<Call>();
        public ArrayList<Call> onCollectionClearedCalled = new ArrayList<Call>();

        public ListenerTester(TalkClientMediaCollection collection) {
            mCollection = collection;
            mListener = new TalkClientMediaCollection.Listener() {
                @Override
                public void onCollectionNameChanged(TalkClientMediaCollection collection) {
                    onNameChangedCalled.add(new Call(collection));
                }

                @Override
                public void onItemOrderChanged(TalkClientMediaCollection collection, int fromIndex, int toIndex) {
                    onItemOrderChangedCalled.add(new Call(collection, fromIndex, toIndex));
                }

                @Override
                public void onItemRemoved(TalkClientMediaCollection collection, int indexRemoved, XoTransfer itemRemoved) {
                    onItemRemovedCalled.add(new Call(collection, indexRemoved, itemRemoved));
                }

                @Override
                public void onItemAdded(TalkClientMediaCollection collection, int indexAdded, XoTransfer itemAdded) {
                    onItemAddedCalled.add(new Call(collection, indexAdded, itemAdded));
                }

                @Override
                public void onCollectionCleared(TalkClientMediaCollection collection) {
                    onCollectionClearedCalled.add(new Call(collection));
                }
            };
            collection.registerListener(mListener);
        }

        public void unregister() {
            mCollection.unregisterListener(mListener);
        }
    }
}
