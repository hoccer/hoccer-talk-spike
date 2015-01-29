package com.hoccer.xo.android.content;

import com.hoccer.talk.client.XoTransfer;
import com.hoccer.talk.client.model.TalkClientDownload;

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
    public boolean hasItem(XoTransfer item) {
        return false;
    }

    @Override
    public int indexOf(XoTransfer item) {
        return -1;
    }

    @Override
    public Iterator<XoTransfer> iterator() {
        return new Iterator<XoTransfer>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public XoTransfer next() {
                throw new NoSuchElementException("There is no next item in playlist.");
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
