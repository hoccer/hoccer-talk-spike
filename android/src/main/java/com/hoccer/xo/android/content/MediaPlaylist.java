package com.hoccer.xo.android.content;

import com.hoccer.talk.content.IContentObject;
import com.hoccer.talk.util.WeakListenerArray;

/**
 * Base class for Playlist implementations.
 */
public abstract class MediaPlaylist implements Iterable<IContentObject> {

    private WeakListenerArray<Listener> mListenerArray = new WeakListenerArray<Listener>();

    // playlist update/change listener
    public interface Listener {
        void onItemOrderChanged(MediaPlaylist playlist);

        void onItemRemoved(MediaPlaylist playlist, IContentObject itemRemoved);

        void onItemAdded(MediaPlaylist playlist, IContentObject itemAdded);

        void onPlaylistCleared(MediaPlaylist playlist);
    }

    public abstract IContentObject getItem(int index);

    public abstract int size();

    public abstract boolean hasItem(IContentObject item);

    public abstract int indexOf(IContentObject item);

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

    protected void invokeItemRemoved(IContentObject itemRemoved) {
        for(Listener listener : mListenerArray) {
            listener.onItemRemoved(this, itemRemoved);
        }
    }

    protected void invokeItemAdded(IContentObject itemAdded) {
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
