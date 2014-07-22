package com.hoccer.xo.android.content;

import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientMediaCollection;

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
            invokeItemOrderChanged(fromIndex, toIndex);
        }

        @Override
        public void onItemRemoved(TalkClientMediaCollection collection, int indexRemoved, TalkClientDownload itemRemoved) {
            invokeItemRemoved(indexRemoved, itemRemoved);
        }

        @Override
        public void onItemAdded(TalkClientMediaCollection collection, int indexAdded, TalkClientDownload itemAdded) {
            invokeItemAdded(indexAdded, itemAdded);
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
    public TalkClientDownload getItem(int index) {
        return mCollection.getItem(index);
    }

    @Override
    public int size() {
        return mCollection.size();
    }

    @Override
    public boolean hasItem(TalkClientDownload item) {
        return mCollection.hasItem(item);
    }

    @Override
    public int indexOf(TalkClientDownload item) {
        return mCollection.indexOf(item);
    }

    @Override
    public Iterator<TalkClientDownload> iterator() {
        return new Iterator<TalkClientDownload>() {
            private Iterator<TalkClientDownload> mIterator = mCollection.iterator();

            @Override
            public boolean hasNext() {
                return mIterator.hasNext();
            }

            @Override
            public TalkClientDownload next() {
                return mIterator.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
