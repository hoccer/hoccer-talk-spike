package com.hoccer.xo.android.content;

import com.hoccer.talk.client.IXoDownloadListener;
import com.hoccer.talk.client.model.TalkClientDownload;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Playlist instance acts as dummy without items.
 */
public class EmptyPlaylist extends MediaPlaylist implements IXoDownloadListener {

    @Override
    public TalkClientDownload getItem(int index) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean hasItem(TalkClientDownload item) {
        return false;
    }

    @Override
    public int indexOf(TalkClientDownload item) {
        return -1;
    }

    @Override
    public Iterator<TalkClientDownload> iterator() {
        return new Iterator<TalkClientDownload>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public TalkClientDownload next() {
                throw new NoSuchElementException("There is no next item in playlist.");
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public void onDownloadSaved(TalkClientDownload download, boolean isCreated) {
    }

    @Override
    public void onDownloadRemoved(TalkClientDownload download) {
    }
}
