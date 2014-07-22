package com.hoccer.xo.android.content;

import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientDownload;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Playlist instance wrapping a user filtered list of media items.
 */
public class UserPlaylist extends MediaPlaylist {

    private List<TalkClientDownload> mList;
    private TalkClientContact mContact;

    /*
     * Constructs a playlist filtered by the given contact.
     * Note: If contact is null no filtering is performed.
     */
    public UserPlaylist(TalkClientContact contact) {
        mContact = contact;
        mList = new ArrayList<TalkClientDownload>();
    }

    public TalkClientContact getContact() {
        return mContact;
    }

    @Override
    public TalkClientDownload getItem(int index) {
        return mList.get(index);
    }

    @Override
    public int size() {
        return mList.size();
    }

    @Override
    public boolean hasItem(TalkClientDownload item) {
        return mList.contains(item);
    }

    @Override
    public int indexOf(TalkClientDownload item) {
        return mList.indexOf(item);
    }

    @Override
    public Iterator<TalkClientDownload> iterator() {
        return new Iterator<TalkClientDownload>() {
            private Iterator<TalkClientDownload> mIterator = mList.iterator();

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
