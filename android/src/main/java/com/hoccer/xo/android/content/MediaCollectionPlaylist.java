package com.hoccer.xo.android.content;

import com.hoccer.talk.client.XoTransfer;
import com.hoccer.talk.client.model.TalkClientMediaCollection;

import java.util.Iterator;


/**
 * Playlist instance wrapping a media collection.
 */
public class MediaCollectionPlaylist extends MediaPlaylist {

    private final TalkClientMediaCollection mCollection;

    private final TalkClientMediaCollection.Listener mListener = new TalkClientMediaCollection.Listener() {
        @Override
        public void onCollectionNameChanged(TalkClientMediaCollection collection) {
            // do nothing
        }

        @Override
        public void onItemOrderChanged(TalkClientMediaCollection collection, int fromIndex, int toIndex) {
            invokeItemOrderChanged();
        }

        @Override
        public void onItemRemoved(TalkClientMediaCollection collection, int indexRemoved, XoTransfer itemRemoved) {
            invokeItemRemoved(itemRemoved);
        }

        @Override
        public void onItemAdded(TalkClientMediaCollection collection, int indexAdded, XoTransfer itemAdded) {
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
    public XoTransfer getItem(int index) {
        return mCollection.getItem(index);
    }

    @Override
    public int size() {
        return mCollection.size();
    }

    @Override
    public boolean hasItem(XoTransfer item) {
        if (item != null) {
            return mCollection.hasItem(item);
        } else {
            return false;
        }
    }

    @Override
    public int indexOf(XoTransfer item) {
        if (item != null) {
            return mCollection.indexOf(item);
        } else {
            return -1;
        }
    }

    @Override
    public Iterator<XoTransfer> iterator() {
        return new Iterator<XoTransfer>() {
            private final Iterator<XoTransfer> mIterator = mCollection.iterator();

            @Override
            public boolean hasNext() {
                return mIterator.hasNext();
            }

            @Override
            public XoTransfer next() {
                return mIterator.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
