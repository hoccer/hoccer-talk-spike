package com.hoccer.xo.android.content;

import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientMediaCollection;
import com.hoccer.talk.util.WeakListenerArray;

/**
 * Base class for Playlist implementations.
 */
public abstract class MediaPlaylist implements Iterable<TalkClientDownload> {

    private WeakListenerArray<Listener> mListenerArray = new WeakListenerArray<Listener>();

    // playlist update/change listener
    public interface Listener {
        void onItemOrderChanged(MediaPlaylist playlist, int fromIndex, int toIndex);

        void onItemRemoved(MediaPlaylist playlist, int indexRemoved, TalkClientDownload itemRemoved);

        void onItemAdded(MediaPlaylist playlist, int indexAdded, TalkClientDownload itemAdded);

        void onPlaylistCleared(MediaPlaylist playlist);
    }

    public abstract TalkClientDownload getItem(int index);

    public abstract int size();

    public abstract boolean hasItem(TalkClientDownload item);

    public abstract int indexOf(TalkClientDownload item);

    public void registerListener(Listener listener) {
        mListenerArray.registerListener(listener);
    }

    public void unregisterListener(Listener listener) {
        mListenerArray.unregisterListener(listener);
    }

    protected void invokeItemOrderChanged(int fromIndex, int toIndex) {
        for(Listener listener : mListenerArray) {
            listener.onItemOrderChanged(this, fromIndex, toIndex);
        }
    }

    protected void invokeItemRemoved(int indexRemoved, TalkClientDownload itemRemoved) {
        for(Listener listener : mListenerArray) {
            listener.onItemRemoved(this, indexRemoved, itemRemoved);
        }
    }

    protected void invokeItemAdded(int indexAdded, TalkClientDownload itemAdded) {
        for(Listener listener : mListenerArray) {
            listener.onItemAdded(this, indexAdded, itemAdded);
        }
    }

    protected void invokePlaylistCleared() {
        for(Listener listener : mListenerArray) {
            listener.onPlaylistCleared(this);
        }
    }
}
