package com.hoccer.xo.android.content.audio;

import com.hoccer.talk.client.XoTransfer;
import com.hoccer.talk.util.WeakListenerArray;
import com.hoccer.xo.android.content.EmptyPlaylist;
import com.hoccer.xo.android.content.MediaPlaylist;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MediaPlaylistController implements MediaPlaylist.Listener {

    public interface Listener {
        void onCurrentItemChanged(XoTransfer newItem);

        void onPlaylistChanged(MediaPlaylist newPlaylist);

        void onRepeatModeChanged(RepeatMode newMode);

        void onShuffleChanged(boolean isShuffled);
    }

    public static enum RepeatMode {
        REPEAT_ITEM, REPEAT_ALL, NO_REPEAT
    }

    private static final Logger LOG = Logger.getLogger(MediaPlaylistController.class);

    private MediaPlaylist mPlaylist = new EmptyPlaylist();
    private List<Integer> mPlaylistOrder;


    private int mCurrentIndex;
    private XoTransfer mCurrentItem;

    private boolean mShuffleActive;

    private RepeatMode mRepeatMode = RepeatMode.NO_REPEAT;

    WeakListenerArray<Listener> mListener = new WeakListenerArray<Listener>();

    public MediaPlaylistController() {
        reset();
    }

    public void setCurrentIndex(int index) {
        if (isIndexInBounds(index)) {
            mCurrentIndex = mPlaylistOrder.indexOf(index);
            setNewCurrentItem(mPlaylist.getItem(index));
            createPlaylistIndexes();
        } else {
            throw new IndexOutOfBoundsException("Index out of bounds");
        }
    }

    public int getCurrentIndex() {
        if (isIndexInBounds(mCurrentIndex)) {
            return mPlaylistOrder.get(mCurrentIndex);
        } else {
            return -1;
        }
    }

    public int size() {
        return mPlaylist.size();
    }

    public boolean canBackward() {
        switch (mRepeatMode) {
            case NO_REPEAT:
                if (isIndexInBounds(mCurrentIndex - 1)) {
                    return true;
                } else {
                    return false;
                }
            case REPEAT_ALL:
                if (mPlaylist.size() > 0) {
                    return true;
                } else {
                    return false;
                }
            case REPEAT_ITEM:
                return mCurrentItem != null;
            default:
                throw new IllegalArgumentException("Invalid repeat mode encountered");
        }
    }

    public boolean canForward() {
        switch (mRepeatMode) {
            case NO_REPEAT:
                if (isIndexInBounds(mCurrentIndex + 1)) {
                    return true;
                } else {
                    return false;
                }
            case REPEAT_ALL:
                if (mPlaylist.size() > 0) {
                    return true;
                } else {
                    return false;
                }
            case REPEAT_ITEM:
                return mCurrentItem != null;
            default:
                throw new IllegalArgumentException("Invalid repeat mode encountered");
        }
    }

    public XoTransfer backward() {
        if (!canBackward()) {
            mCurrentIndex = -1;
            setNewCurrentItem(null);
        } else {
            switch (mRepeatMode) {
                case NO_REPEAT:
                    --mCurrentIndex;
                    setNewCurrentItem(mPlaylist.getItem(mPlaylistOrder.get(mCurrentIndex)));
                    break;
                case REPEAT_ALL:
                    if (mCurrentIndex - 1 < 0) {
                        mCurrentIndex = mPlaylist.size() - 1;
                        setNewCurrentItem(mPlaylist.getItem(mPlaylistOrder.get(mCurrentIndex)));
                        if (mShuffleActive) {
                            createPlaylistIndexes();  // reshuffle playlist
                        }
                    } else {
                        mCurrentIndex = mCurrentIndex - 1;
                        setNewCurrentItem(mPlaylist.getItem(mPlaylistOrder.get(mCurrentIndex)));
                    }
                    break;
                case REPEAT_ITEM:
                    break; // do nothing
                default:
                    throw new IllegalArgumentException("Invalid repeat mode encountered");
            }
        }
        return mCurrentItem;
    }

    public XoTransfer forward() {
        if (!canForward()) {
            mCurrentIndex = -1;
            setNewCurrentItem(null);
        } else {
            switch (mRepeatMode) {
                case NO_REPEAT:
                    ++mCurrentIndex;
                    setNewCurrentItem(mPlaylist.getItem(mPlaylistOrder.get(mCurrentIndex)));
                    break;
                case REPEAT_ALL:
                    if (mCurrentIndex + 1 >= mPlaylist.size()) {
                        mCurrentIndex = 0;
                        setNewCurrentItem(mPlaylist.getItem(mPlaylistOrder.get(mCurrentIndex)));
                        if (mShuffleActive) {
                            createPlaylistIndexes();  // reshuffle playlist
                        }
                    } else {
                        mCurrentIndex = mCurrentIndex + 1;
                        setNewCurrentItem(mPlaylist.getItem(mPlaylistOrder.get(mCurrentIndex)));
                    }
                    break;
                case REPEAT_ITEM:
                    break; // do nothing
                default:
                    throw new IllegalArgumentException("Invalid repeat mode encountered");
            }
        }
        return mCurrentItem;
    }

    public XoTransfer getCurrentItem() {
        return mCurrentItem;
    }

    public void setPlaylist(MediaPlaylist playlist) {
        mPlaylist.unregisterListener(this);
        mPlaylist = playlist;
        mPlaylist.registerListener(this);

        // set initial item
        if (playlist.size() > 0) {
            if (mShuffleActive) {
                Random rnd = new Random(System.nanoTime());
                mCurrentIndex = rnd.nextInt(playlist.size());
            } else {
                mCurrentIndex = 0;
            }
            setNewCurrentItem(mPlaylist.getItem(mCurrentIndex));
        } else {
            mCurrentIndex = 0;
            setNewCurrentItem(null);
        }

        createPlaylistIndexes();

        for (Listener listener : mListener) {
            listener.onPlaylistChanged(mPlaylist);
        }
    }

    public void reset() {
        setPlaylist(new EmptyPlaylist());
    }

    public RepeatMode getRepeatMode() {
        return mRepeatMode;
    }

    public void setRepeatMode(RepeatMode repeatMode) {
        if (mRepeatMode == repeatMode) {
            return;
        }

        this.mRepeatMode = repeatMode;

        for (Listener listener : mListener) {
            listener.onRepeatModeChanged(mRepeatMode);
        }
    }

    public boolean getShuffleActive() {
        return mShuffleActive;
    }

    public void setShuffleActive(boolean shuffleActive) {
        if (mShuffleActive == shuffleActive) {
            return;
        }

        mShuffleActive = shuffleActive;
        createPlaylistIndexes();

        for (Listener listener : mListener) {
            listener.onShuffleChanged(mShuffleActive);
        }
    }

    private void createPlaylistIndexes() {
        mPlaylistOrder = new ArrayList<Integer>();
        for (int i = 0; i < mPlaylist.size(); i++) {
            mPlaylistOrder.add(i);
        }

        if (mShuffleActive) {
            Random rnd = new Random(System.nanoTime());
            Collections.shuffle(mPlaylistOrder, rnd);

            // move current item index to first shuffle position
            if (mCurrentItem != null) {
                int playlistIndex = mPlaylist.indexOf(mCurrentItem);
                int shuffledIndex = mPlaylistOrder.indexOf(playlistIndex);
                int firstShuffledPlaylistIndex = mPlaylistOrder.get(0);
                mPlaylistOrder.set(0, playlistIndex);
                mPlaylistOrder.set(shuffledIndex, firstShuffledPlaylistIndex);
                mCurrentIndex = 0;
            }
        } else {
            // update index of current item
            if (mCurrentItem != null) {
                int playlistIndex = mPlaylist.indexOf(mCurrentItem);
                mCurrentIndex = mPlaylistOrder.indexOf(playlistIndex);
            }
        }
    }

    private boolean isIndexInBounds(int index) {
        return (index >= 0) && (index < mPlaylist.size());
    }

    public void registerListener(Listener listener) {
        mListener.registerListener(listener);
    }

    public void unregisterListener(Listener listener) {
        mListener.unregisterListener(listener);
    }

    @Override
    public void onItemOrderChanged(MediaPlaylist playlist) {
        createPlaylistIndexes();
    }

    @Override
    public void onItemRemoved(MediaPlaylist playlist, XoTransfer itemRemoved) {
        if (mCurrentItem != null) {
            if (mPlaylist.size() > 0) {
                if (itemRemoved == mCurrentItem) {
                    // the current title was removed move on to next based on repeat mode
                    switch (mRepeatMode) {
                        case NO_REPEAT:
                            if (mCurrentIndex < mPlaylist.size()) {
                                setNewCurrentItem(mPlaylist.getItem(mPlaylistOrder.get(mCurrentIndex)));
                            } else {
                                mCurrentIndex = -1;
                                setNewCurrentItem(null);
                            }
                            break;
                        case REPEAT_ALL:
                            if (mCurrentIndex < mPlaylist.size()) {
                                setNewCurrentItem(mPlaylist.getItem(mPlaylistOrder.get(mCurrentIndex)));
                            } else {
                                mCurrentIndex = 0;
                                setNewCurrentItem(mPlaylist.getItem(mPlaylistOrder.get(mCurrentIndex)));
                            }
                            break;
                        case REPEAT_ITEM:
                            mCurrentIndex = -1;
                            setNewCurrentItem(null);
                            break;
                        default:
                            throw new IllegalArgumentException("Invalid repeat mode encountered");
                    }
                }
            } else {
                mCurrentIndex = -1;
                setNewCurrentItem(null);
            }
        }

        createPlaylistIndexes();
        for (Listener listener : mListener) {
            listener.onPlaylistChanged(mPlaylist);
        }
    }

    private void setNewCurrentItem(XoTransfer newItem) {
        // do nothing if there is no change
        if (mCurrentItem == newItem) {
            return;
        }

        mCurrentItem = newItem;

        for (Listener listener : mListener) {
            listener.onCurrentItemChanged(mCurrentItem);
        }
    }

    @Override
    public void onItemAdded(MediaPlaylist playlist, XoTransfer itemAdded) {
        createPlaylistIndexes();

        for (Listener listener : mListener) {
            listener.onPlaylistChanged(mPlaylist);
        }
    }

    @Override
    public void onPlaylistCleared(MediaPlaylist playlist) {
        mCurrentIndex = -1;
        setNewCurrentItem(null);
        createPlaylistIndexes();
    }
}
