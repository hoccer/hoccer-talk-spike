package com.hoccer.xo.android.content;

import com.hoccer.talk.client.XoTransfer;
import com.hoccer.talk.content.SelectedAttachment;
import com.hoccer.talk.util.WeakListenerArray;

/**
 * Base class for Playlist implementations.
 */
public abstract class MediaPlaylist implements Iterable<XoTransfer> {

    private final WeakListenerArray<Listener> mListenerArray = new WeakListenerArray<Listener>();

    // playlist update/change listener
    public interface Listener {
        void onItemOrderChanged(MediaPlaylist playlist);

        void onItemRemoved(MediaPlaylist playlist, XoTransfer itemRemoved);

        void onItemAdded(MediaPlaylist playlist, XoTransfer itemAdded);

        void onPlaylistCleared(MediaPlaylist playlist);
    }

    public abstract XoTransfer getItem(int index);

    public abstract int size();

    public abstract boolean hasItem(XoTransfer item);

    public abstract int indexOf(XoTransfer item);

    public void registerListener(Listener listener) {
        mListenerArray.registerListener(listener);
    }

    public void unregisterListener(Listener listener) {
        mListenerArray.unregisterListener(listener);
    }

    protected void invokeItemOrderChanged() {
        for(Listener listener : mListenerArray) {
            listener.onItemOrderChanged(this);
        }
    }

    protected void invokeItemRemoved(XoTransfer itemRemoved) {
        for(Listener listener : mListenerArray) {
            listener.onItemRemoved(this, itemRemoved);
        }
    }

    protected void invokeItemAdded(XoTransfer itemAdded) {
        for(Listener listener : mListenerArray) {
            listener.onItemAdded(this, itemAdded);
        }
    }

    protected void invokePlaylistCleared() {
        for(Listener listener : mListenerArray) {
            listener.onPlaylistCleared(this);
        }
    }
}
