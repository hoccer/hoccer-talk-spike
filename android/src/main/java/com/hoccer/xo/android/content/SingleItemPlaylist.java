package com.hoccer.xo.android.content;

import com.hoccer.talk.client.IXoDownloadListener;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.content.IContentObject;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Playlist instance wrapping a single media items.
 */
public class SingleItemPlaylist extends MediaPlaylist implements IXoDownloadListener {

    private IContentObject mItem;

    /*
     * Constructs a playlist containing only the given item.
     */
    public SingleItemPlaylist(XoClientDatabase database, IContentObject item) {
        mItem = item;
        database.registerDownloadListener(this);

    }

    @Override
    public IContentObject getItem(int index) {
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
    public boolean hasItem(IContentObject item) {
        return item.equals(mItem);
    }

    @Override
    public int indexOf(IContentObject item) {
        return item.equals(mItem) ? 0 : -1;
    }

    @Override
    public Iterator<IContentObject> iterator() {
        return new Iterator<IContentObject>() {
            private int mCurrentIndex = 0;

            @Override
            public boolean hasNext() {
                return mCurrentIndex == 0 && mItem != null;
            }

            @Override
            public IContentObject next() {
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

    @Override
    public void onDownloadCreated(TalkClientDownload download, boolean isCreated) {
        // do nothing
    }

    @Override
    public void onDownloadUpdated(TalkClientDownload download) {
        // do nothing
    }

    @Override
    public void onDownloadDeleted(TalkClientDownload download) {
        if(mItem != null && mItem.equals(download)) {
            mItem = null;
            invokeItemRemoved(download);
        }
    }
}
