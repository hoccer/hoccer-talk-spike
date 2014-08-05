package com.hoccer.xo.android.content;

import com.hoccer.talk.client.IXoDownloadListener;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.content.IContentObject;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Playlist instance acts as dummy without items.
 */
public class EmptyPlaylist extends MediaPlaylist {

    @Override
    public TalkClientDownload getItem(int index) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean hasItem(IContentObject item) {
        return false;
    }

    @Override
    public int indexOf(IContentObject item) {
        return -1;
    }

    @Override
    public Iterator<IContentObject> iterator() {
        return new Iterator<IContentObject>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public IContentObject next() {
                throw new NoSuchElementException("There is no next item in playlist.");
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
