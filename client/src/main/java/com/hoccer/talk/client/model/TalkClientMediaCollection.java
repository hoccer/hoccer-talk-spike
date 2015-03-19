package com.hoccer.talk.client.model;

import com.hoccer.talk.client.IXoMediaCollectionDatabase;
import com.hoccer.talk.client.XoTransfer;
import com.hoccer.talk.util.WeakListenerArray;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.table.DatabaseTable;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Encapsulates a collection of media items with a specific order. The data is kept in sync with the database.
 */
@DatabaseTable(tableName = "mediaCollection")
public class TalkClientMediaCollection implements Iterable<XoTransfer> {

    // collection update/change listener
    public interface Listener {
        void onCollectionNameChanged(TalkClientMediaCollection collection);

        void onItemOrderChanged(TalkClientMediaCollection collection, int fromIndex, int toIndex);

        void onItemRemoved(TalkClientMediaCollection collection, int indexRemoved, XoTransfer itemRemoved);

        void onItemAdded(TalkClientMediaCollection collection, int indexAdded, XoTransfer itemAdded);

        void onCollectionCleared(TalkClientMediaCollection collection);
    }

    @DatabaseField(generatedId = true, columnName = "collectionId")
    private int mCollectionId;

    @DatabaseField(columnName = "name")
    private String mName;

    private IXoMediaCollectionDatabase mDatabase;

    private List<XoTransfer> mItemList = new ArrayList<XoTransfer>();

    WeakListenerArray<Listener> mListenerArray = new WeakListenerArray<Listener>();

    // do not call constructor directly but create instances via IXoMediaCollectionDatabase.createMediaCollection()
    public TalkClientMediaCollection() {
    }

    // do not call constructor directly but create instances via IXoMediaCollectionDatabase.createMediaCollection()
    public TalkClientMediaCollection(String collectionName) {
        mName = collectionName;
    }

    public Integer getId() {
        return mCollectionId;
    }

    // this method should only by called by the database on instantiation
    public void setDatabase(IXoMediaCollectionDatabase db) {
        if(mDatabase == null) {
            mDatabase = db;
            mItemList = findMediaCollectionItemsOrderedByIndex();
        }
    }

    public void setName(String name) {
        mName = name;
        updateMediaCollection();
        for (Listener listener : mListenerArray) {
            listener.onCollectionNameChanged(this);
        }
    }

    public String getName() {
        return mName;
    }

    // Appends the given item to the collection
    public void addItem(XoTransfer item) {
        addItem(mItemList.size(), item);
    }

    // Inserts the given item into the collection
    public void addItem(int index, XoTransfer item) {
        if(hasItem(item)) {
            return;
        }

        if (createRelation(item, index)) {
            mItemList.add(index, item);
            for (Listener listener : mListenerArray) {
                listener.onItemAdded(this, index, item);
            }
        }
    }

    // Returns if a given item is contained in this collection or not
    public boolean hasItem(XoTransfer item) {
        return mItemList.contains(item);
    }

    // Returns the index of the item if it is contained in this collection or -1
    public int indexOf(XoTransfer item) {
        return mItemList.indexOf(item);
    }

    // Removes the given item from the collection
    public void removeItem(XoTransfer item) {
        int index = mItemList.indexOf(item);
        if (index >= 0) {
            removeItem(index);
        }
    }

    // Removes the item at the given index from the collection
    public void removeItem(int index) {
        if (removeRelation(index)) {
            XoTransfer item = mItemList.get(index);
            mItemList.remove(index);
            for (Listener listener : mListenerArray) {
                listener.onItemRemoved(this, index, item);
            }
        }
    }

    // Moves the item at index 'from' to index 'to'.
    // Throws an IndexOutOfBoundsException if 'from' or 'to' is out of bounds.
    public void reorderItemIndex(int from, int to) {
        if (from < 0 || from >= mItemList.size()) {
            throw new IndexOutOfBoundsException("'from' parameter is out of bounds [0," + mItemList.size() + "] with value: " + from);
        }

        if (to < 0 || to >= mItemList.size()) {
            throw new IndexOutOfBoundsException("'to' parameter is out of bounds [0," + mItemList.size() + "] with value: " + to);
        }

        if (from == to) {
            return;
        }

        if (reorderRelation(from, to)) {
            XoTransfer item = mItemList.get(from);
            mItemList.remove(from);
            mItemList.add(to, item);
            for (Listener listener : mListenerArray) {
                listener.onItemOrderChanged(this, from, to);
            }
        }
    }

    // Returns the size of the collection array
    public int size() {
        return mItemList.size();
    }

    // Returns the item at the given index
    public XoTransfer getItem(int index) {
        return mItemList.get(index);
    }

    // Returns the item with the given id or null if not found
    public XoTransfer getItemFromId(int itemId) {
        XoTransfer result = null;
        for(XoTransfer item : mItemList) {
            if(item.getTransferId() == itemId) {
                result = item;
                break;
            }
        }
        return result;
    }

    // Remove all items from collection
    public void clear() {
        if (mItemList.isEmpty()) {
            return;
        }

        // delete all relationships from  database
        try {
            DeleteBuilder<TalkClientMediaCollectionRelation, Integer> deleteBuilder = mDatabase.getMediaCollectionRelationDao().deleteBuilder();
            deleteBuilder.where()
                    .eq("collection_id", mCollectionId);
            deleteBuilder.delete();
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }
        mItemList.clear();
        for (Listener listener : mListenerArray) {
            listener.onCollectionCleared(this);
        }
    }

    // This method should only be called in environments where multiple instances of the same collection can exist
    // (no instance caching) to ensure that all changes made by another instance are refreshed in this instance.
    public void refresh() {
        try {
            mDatabase.getMediaCollectionDao().refresh(this);
            mItemList = findMediaCollectionItemsOrderedByIndex();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Returns a copy of the internal TalkClientDownload array
    public List<XoTransfer> getItems() {
        return new ArrayList<XoTransfer>(mItemList);
    }

    public void registerListener(Listener listener) {
        mListenerArray.registerListener(listener);
    }

    public void unregisterListener(Listener listener) {
        mListenerArray.unregisterListener(listener);
    }

    @Override
    public Iterator<XoTransfer> iterator() {
        return new Iterator<XoTransfer>() {
            private int mCurrentIndex;

            @Override
            public boolean hasNext() {
                return mCurrentIndex < mItemList.size();
            }

            @Override
            public XoTransfer next() {
                if (hasNext()) {
                    return mItemList.get(mCurrentIndex++);
                } else {
                    throw new NoSuchElementException("There is no next item in media collection.");
                }
            }

            @Override
            public void remove() {
                if (mCurrentIndex < mItemList.size()) {
                    removeItem(mCurrentIndex);
                } else {
                    throw new NoSuchElementException("There is no next item in media collection.");
                }
            }
        };
    }

    private List<XoTransfer> findMediaCollectionItemsOrderedByIndex() {
        List<XoTransfer> items = new ArrayList<XoTransfer>();

        try {
            List<TalkClientMediaCollectionRelation> relations = mDatabase.getMediaCollectionRelationDao().queryBuilder()
                    .orderBy("index", true)
                    .where()
                    .eq("collection_id", mCollectionId)
                    .query();

            for (TalkClientMediaCollectionRelation relation : relations) {
                items.add(relation.getTransferItem());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return items;
    }

    private boolean reorderRelation(int from, int to) {
        try {
            int low = from < to ? from : to;
            int high = from > to ? from : to;

            // find all relations of this collection within [low,high]
            Dao<TalkClientMediaCollectionRelation, Integer> relationDao = mDatabase.getMediaCollectionRelationDao();
            List<TalkClientMediaCollectionRelation> relations = relationDao.queryBuilder()
                    .where()
                    .not()
                    .lt("index", low)
                    .and()
                    .le("index", high)
                    .and()
                    .eq("collection_id", mCollectionId)
                    .query();

            // decrease/increase index of all affected items except the reordered one
            int step = from < to ? -1 : 1;
            for (int i = 0; i < relations.size(); i++) {
                TalkClientMediaCollectionRelation relation = relations.get(i);
                if (relation.getIndex() == from) {
                    relation.setIndex(to);
                } else {
                    relation.setIndex(relation.getIndex() + step);
                }
                relationDao.update(relation);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean createRelation(XoTransfer item, int index) {
        try {
            // increment index of all items with same or higher index
            Dao<TalkClientMediaCollectionRelation, Integer> relationDao = mDatabase.getMediaCollectionRelationDao();
            List<TalkClientMediaCollectionRelation> relations = relationDao.queryBuilder()
                    .where()
                    .not()
                    .lt("index", index)
                    .and()
                    .eq("collection_id", mCollectionId)
                    .query();

            for (int i = 0; i < relations.size(); i++) {
                TalkClientMediaCollectionRelation relation = relations.get(i);
                relation.setIndex(relation.getIndex() + 1);
                relationDao.update(relation);
            }

            TalkClientMediaCollectionRelation newRelation = new TalkClientMediaCollectionRelation(mCollectionId, item, index);
            relationDao.create(newRelation);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean removeRelation(int index) {
        try {
            Dao<TalkClientMediaCollectionRelation, Integer> relationDao = mDatabase.getMediaCollectionRelationDao();
            TalkClientMediaCollectionRelation relationToDelete = relationDao.queryBuilder()
                    .where()
                    .eq("collection_id", mCollectionId)
                    .and()
                    .eq("index", index)
                    .queryForFirst();

            if (relationToDelete != null) {
                relationDao.delete(relationToDelete);

                // decrement index of all items with higher index
                List<TalkClientMediaCollectionRelation> relations = relationDao.queryBuilder()
                        .where()
                        .not()
                        .le("index", index)
                        .and()
                        .eq("collection_id", mCollectionId)
                        .query();

                for (int i = 0; i < relations.size(); i++) {
                    TalkClientMediaCollectionRelation relation = relations.get(i);
                    relation.setIndex(relation.getIndex() - 1);
                    relationDao.update(relation);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean updateMediaCollection() {
        try {
            mDatabase.getMediaCollectionDao().update(this);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
