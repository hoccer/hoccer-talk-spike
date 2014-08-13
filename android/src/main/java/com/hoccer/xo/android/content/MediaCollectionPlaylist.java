package com.hoccer.xo.android.content;

import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientMediaCollection;
import com.hoccer.talk.content.IContentObject;

import java.util.Iterator;

/**
 * Playlist instance wrapping a media collection.
 */
public class MediaCollectionPlaylist extends MediaPlaylist {

    private TalkClientMediaCollection mCollection;

    private TalkClientMediaCollection.Listener mListener = new TalkClientMediaCollection.Listener() {
        @Override
        public void onCollectionNameChanged(TalkClientMediaCollection collection) {
            // do nothing
        }

        @Override
        public void onItemOrderChanged(TalkClientMediaCollection collection, int fromIndex, int toIndex) {
            invokeItemOrderChanged();
        }

        @Override
        public void onItemRemoved(TalkClientMediaCollection collection, int indexRemoved, TalkClientDownload itemRemoved) {
            invokeItemRemoved(itemRemoved);
        }

        @Override
        public void onItemAdded(TalkClientMediaCollection collection, int indexAdded, TalkClientDownload itemAdded) {
            invokeItemAdded(itemAdded);
        }

        @Override
        public void onCollectionCleared(TalkClientMediaCollection collection) {
            invokePlaylistCleared();
        }
    };

    public MediaCollectionPlaylist(TalkClientMediaCollection collection) {
        mCollection = collection;
        collection.registerListener(mListener);
    }

    public TalkClientMediaCollection getMediaCollection() {
        return mCollection;
    }

    @Override
    public IContentObject getItem(int index) {
        return mCollection.getItem(index);
    }

    @Override
    public int size() {
        return mCollection.size();
    }

    @Override
    public boolean hasItem(IContentObject item) {
        TalkClientDownload download = (TalkClientDownload)item;
        if(download != null) {
            return mCollection.hasItem(download);
        } else {
            return false;
        }
    }

    @Override
    public int indexOf(IContentObject item) {
        TalkClientDownload download = (TalkClientDownload)item;
        if(download != null) {
            return mCollection.indexOf(download);
        } else {
            return -1;
        }
    }

    @Override
    public Iterator<IContentObject> iterator() {
        return new Iterator<IContentObject>() {
            private Iterator<TalkClientDownload> mIterator = mCollection.iterator();

            @Override
            public boolean hasNext() {
                return mIterator.hasNext();
            }

            @Override
            public IContentObject next() {
                return mIterator.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}