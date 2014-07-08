package com.hoccer.talk.client.model;

import com.hoccer.talk.client.IXoMediaCollectionDatabase;
import com.hoccer.talk.util.WeakListenerArray;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.table.DatabaseTable;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.*;

/**
 * Encapsulates a collection of media items with a specific order. The data is kept in sync with the database.
 */
@DatabaseTable(tableName = "mediaCollection")
public class TalkClientMediaCollection implements Iterable<TalkClientDownload> {

    // collection update/change listener
    public interface Listener {
        void onCollectionNameChanged(TalkClientMediaCollection collection);
        void onItemOrderChanged(TalkClientMediaCollection collection, int fromIndex, int toIndex);
        void onItemRemoved(TalkClientMediaCollection collection, int indexRemoved, TalkClientDownload itemRemoved);
        void onItemAdded(TalkClientMediaCollection collection, int indexAdded, TalkClientDownload itemAdded);
        void onCollectionCleared(TalkClientMediaCollection collection);
    }

    private static final Logger LOG = Logger.getLogger(TalkClientMediaCollection.class);

    @DatabaseField(generatedId = true, columnName = "collectionId")
    private int mCollectionId;

    @DatabaseField(columnName = "name")
    private String mName;

    private IXoMediaCollectionDatabase mDatabase;

    private List<TalkClientDownload> mItemList = new ArrayList<TalkClientDownload>();

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
        mDatabase = db;
        mItemList = findMediaCollectionItemsOrderedByIndex();
    }

    public void setName(String name) {
        mName = name;
        updateMediaCollection();
        for(Listener listener : mListenerArray) {
            listener.onCollectionNameChanged(this);
        }
    }

    public String getName() {
        return mName;
    }

    // Appends the given item to the collection
    public void addItem(TalkClientDownload item) {
        if(createRelation(item, mItemList.size())) {
            mItemList.add(item);
            for(Listener listener : mListenerArray) {
                listener.onItemAdded(this, mItemList.size() - 1, item);
            }
        }
    }

    // Inserts the given item into the collection
    public void addItem(int index, TalkClientDownload item) {
        if(index >= mItemList.size()) {
            addItem(item); // simply append
        } else {
            if(createRelation(item, index)) {
                mItemList.add(index, item);
                for(Listener listener : mListenerArray) {
                    listener.onItemAdded(this, index, item);
                }
            }
        }
    }

    // Removes the given item from the collection
    public void removeItem(TalkClientDownload item) {
        int index = mItemList.indexOf(item);
        if(index >= 0) {
            removeItem(index);
        }
    }

    // Removes the item at the given index from the collection
    public void removeItem(int index) {
        if(removeRelation(index)) {
            TalkClientDownload item = mItemList.get(index);
            mItemList.remove(index);
            for(Listener listener : mListenerArray) {
                listener.onItemRemoved(this, index, item);
            }
        }
    }

    // Moves the item at index 'from' to index 'to'.
    // Throws an IndexOutOfBoundsException if 'from' or 'to' is out of bounds.
    public void reorderItemIndex(int from, int to) {
        if(from < 0 || from >= mItemList.size()) {
            throw new IndexOutOfBoundsException("'from' parameter is out of bounds [0," + mItemList.size() + "] with value: " + from);
        }

        if(to < 0 || to >= mItemList.size()) {
            throw new IndexOutOfBoundsException("'to' parameter is out of bounds [0," + mItemList.size() + "] with value: " + to);
        }

        if(from == to) {
            return;
        }

        if(reorderRelation(from, to)) {
            TalkClientDownload item = mItemList.get(from);
            mItemList.remove(from);
            mItemList.add(to, item);
            for(Listener listener : mListenerArray) {
                listener.onItemOrderChanged(this, from, to);
            }
        }
    }

    // Returns the size of the collection array
    public int size() {
        return mItemList.size();
    }

    public TalkClientDownload getItem(int index) {
        return mItemList.get(index);
    }

    // Remove all items from collection
    public void clear() {
        if(mItemList.size() == 0) {
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
        for(Listener listener : mListenerArray) {
            listener.onCollectionCleared(this);
        }
    }

    // Returns a copy of the internal TalkClientDownload array
    public TalkClientDownload[] toArray() {
        return mItemList.toArray(new TalkClientDownload[mItemList.size()]);
    }

    public void registerListener(Listener listener) {
        mListenerArray.registerListener(listener);
    }

    public void unregisterListener(Listener listener) {
        mListenerArray.unregisterListener(listener);
    }

    @Override
    public Iterator<TalkClientDownload> iterator() {
        return new Iterator<TalkClientDownload>() {
            int mCurrentIndex = 0;

            @Override
            public boolean hasNext() {
                return mCurrentIndex < mItemList.size();
            }

            @Override
            public TalkClientDownload next() {
                if(mCurrentIndex < mItemList.size()) {
                    return mItemList.get(mCurrentIndex++);
                } else {
                    throw new NoSuchElementException("There is no next item in media collection.");
                }
            }

            @Override
            public void remove() {
                if(mCurrentIndex < mItemList.size()) {
                    removeItem(mCurrentIndex);
                } else {
                    throw new NoSuchElementException("There is no next item in media collection.");
                }
            }
        };
    }

    private List<TalkClientDownload> findMediaCollectionItemsOrderedByIndex() {
        List<TalkClientDownload> items = new ArrayList<TalkClientDownload>();

        try {
            List<TalkClientMediaCollectionRelation> relations = mDatabase.getMediaCollectionRelationDao().queryBuilder()
                    .orderBy("index", true)
                    .where()
                    .eq("collection_id", mCollectionId)
                    .query();

            for(TalkClientMediaCollectionRelation relation : relations) {
                items.add(relation.getItem());
            }
        } catch(SQLException e) {
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
            for(int i = 0; i < relations.size(); i++) {
                TalkClientMediaCollectionRelation relation = relations.get(i);
                if(relation.getIndex() == from) {
                    relation.setIndex(to);
                } else {
                    relation.setIndex(relation.getIndex() + step);
                }
                relationDao.update(relation);
            }
        } catch(SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean createRelation(TalkClientDownload item, int index) {
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

            for(int i = 0; i < relations.size(); i++) {
                TalkClientMediaCollectionRelation relation = relations.get(i);
                relation.setIndex(relation.getIndex() + 1);
                relationDao.update(relation);
            }

            TalkClientMediaCollectionRelation newRelation = new TalkClientMediaCollectionRelation(mCollectionId, item, index);
            relationDao.create(newRelation);
        } catch(SQLException e) {
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

            if(relationToDelete != null) {
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
        } catch(SQLException e) {
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

    public boolean hasItem(TalkClientDownload item) {
        if (mItemList.contains(item)) {
            return true;
        }
        return false;
    }
}
