package com.hoccer.xo.android.content;

import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientDownload;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Playlist instance wrapping a single media items.
 */
public class SingleItemPlaylist extends MediaPlaylist {

    private TalkClientDownload mItem;

    /*
     * Constructs a playlist containing only the given item.
     */
    public SingleItemPlaylist(TalkClientDownload item) {
        mItem = item;
    }

    @Override
    public TalkClientDownload getItem(int index) {
        if(index != 0 || mItem == null) {
            throw new IndexOutOfBoundsException();
        }
        return mItem;
    }

    @Override
    public int size() {
        return mItem == null ? 0 : 1;
    }

    @Override
    public boolean hasItem(TalkClientDownload item) {
        return item.equals(mItem);
    }

    @Override
    public int indexOf(TalkClientDownload item) {
        return item.equals(mItem) ? 0 : -1;
    }

    @Override
    public Iterator<TalkClientDownload> iterator() {
        return new Iterator<TalkClientDownload>() {
            private int mCurrentIndex = 0;

            @Override
            public boolean hasNext() {
                return mCurrentIndex == 0 && mItem != null;
            }

            @Override
            public TalkClientDownload next() {
                if (hasNext()) {
                    mCurrentIndex++;
                    return mItem;
                } else {
                    throw new NoSuchElementException("There is no next item in playlist.");
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
