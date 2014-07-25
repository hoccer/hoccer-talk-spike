package com.hoccer.xo.android.content.audio;

import com.hoccer.talk.content.IContentObject;
import com.hoccer.xo.android.content.EmptyPlaylist;
import com.hoccer.xo.android.content.MediaPlaylist;
import org.apache.log4j.Logger;

import java.util.*;

public class MediaPlaylistController implements MediaPlaylist.Listener {

    public static enum RepeatMode {
        REPEAT_ITEM, REPEAT_ALL, NO_REPEAT;
    }

    private static final Logger LOG = Logger.getLogger(MediaPlaylistController.class);

    private MediaPlaylist mPlaylist = new EmptyPlaylist();
    private List<Integer> mPlaylistOrder;


    private int mCurrentIndex;
    private IContentObject mCurrentItem;

    private boolean mShuffleActive = false;

    private RepeatMode mRepeatMode = RepeatMode.NO_REPEAT;

    public MediaPlaylistController() {
        reset();
    }

    public void setCurrentIndex(int index) {
        if(isIndexInBounds(index)) {
            mCurrentIndex = mPlaylistOrder.indexOf(index);
            mCurrentItem = mPlaylist.getItem(index);
            createPlaylistIndexes();
        } else {
            throw new IndexOutOfBoundsException("Index out of bounds");
        }
    }

    public int getCurrentIndex() {
        if(isIndexInBounds(mCurrentIndex)) {
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
                if(mPlaylist.size() > 0) {
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
                if(mPlaylist.size() > 0) {
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

    public IContentObject backward() {
        if(!canBackward()) {
            return null;
        }

        switch (mRepeatMode) {
            case NO_REPEAT:
                --mCurrentIndex;
                mCurrentItem = mPlaylist.getItem(mPlaylistOrder.get(mCurrentIndex));
                break;
            case REPEAT_ALL:
                mCurrentIndex--;
                if (mCurrentIndex < 0) {
                    mCurrentIndex = mPlaylist.size() - 1;
                }
                mCurrentItem = mPlaylist.getItem(mPlaylistOrder.get(mCurrentIndex));
                break;
            case REPEAT_ITEM:
                break; // do nothing
            default:
                throw new IllegalArgumentException("Invalid repeat mode encountered");
        }
        return mCurrentItem;
    }

    public IContentObject forward() {
        if(!canForward()) {
            return null;
        }

        switch (mRepeatMode) {
            case NO_REPEAT:
                ++mCurrentIndex;
                mCurrentItem = mPlaylist.getItem(mPlaylistOrder.get(mCurrentIndex));
                break;
            case REPEAT_ALL:
                ++mCurrentIndex;
                if (mCurrentIndex >= mPlaylist.size()) {
                    mCurrentIndex = 0;
                }
                mCurrentItem = mPlaylist.getItem(mPlaylistOrder.get(mCurrentIndex));
                break;
            case REPEAT_ITEM:
                break; // do nothing
            default:
                throw new IllegalArgumentException("Invalid repeat mode encountered");
        }
        return mCurrentItem;
    }

    public IContentObject getCurrentItem() {
        return mCurrentItem;
    }

    public void setPlaylist(MediaPlaylist playlist) {
        mPlaylist.unregisterListener(this);
        mPlaylist = playlist;
        mPlaylist.registerListener(this);

        // set initial item
        if(playlist.size() > 0) {
            if (mShuffleActive) {
                Random rnd = new Random(System.nanoTime());
                mCurrentIndex = rnd.nextInt(playlist.size());
            } else {
                mCurrentIndex = 0;
            }
            mCurrentItem = mPlaylist.getItem(mCurrentIndex);
        } else {
            mCurrentIndex = 0;
            mCurrentItem = null;
        }

        createPlaylistIndexes();
    }

    public void reset() {
        setPlaylist(new EmptyPlaylist());
        mCurrentIndex = 0;
        mCurrentItem = null;
        createPlaylistIndexes();
    }

    public RepeatMode getRepeatMode() {
        return mRepeatMode;
    }

    public void setRepeatMode(RepeatMode repeatMode) {
        this.mRepeatMode = repeatMode;
    }

    public boolean getShuffleActive() {
        return mShuffleActive;
    }

    public void setShuffleActive(boolean mShuffleActive) {
        this.mShuffleActive = mShuffleActive;
        createPlaylistIndexes();
    }

    private void createPlaylistIndexes() {
        mPlaylistOrder = new ArrayList<Integer>();
        for (int i = 0; i < mPlaylist.size(); i++) {
            mPlaylistOrder.add(i);
        }

        if(mShuffleActive) {
            Random rnd = new Random(System.nanoTime());
            Collections.shuffle(mPlaylistOrder, rnd);

            // move current item index to first shuffle position
            if(mCurrentItem != null) {
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

    @Override
    public void onItemOrderChanged(MediaPlaylist playlist) {
        createPlaylistIndexes();
    }

    @Override
    public void onItemRemoved(MediaPlaylist playlist, IContentObject itemRemoved) {
        if(mPlaylist.size() > 0 && mCurrentItem != null) {
            // if the current title was removed
            if(itemRemoved == mCurrentItem) {
                switch (mRepeatMode) {
                    case NO_REPEAT:
                        if(mCurrentIndex < mPlaylist.size()) {
                            mCurrentItem = mPlaylist.getItem(mPlaylistOrder.get(mCurrentIndex));
                        } else {
                            mCurrentIndex = -1;
                            mCurrentItem = null;
                        }
                        break;
                    case REPEAT_ALL:
                        if (mCurrentIndex < mPlaylist.size()) {
                            mCurrentItem = mPlaylist.getItem(mPlaylistOrder.get(mCurrentIndex));
                        } else {
                            mCurrentIndex = 0;
                            mCurrentItem = mPlaylist.getItem(mPlaylistOrder.get(mCurrentIndex));
                        }
                        break;
                    case REPEAT_ITEM:
                        mCurrentIndex = -1;
                        mCurrentItem = null;
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid repeat mode encountered");
                }
            }
        } else {
            mCurrentIndex = -1;
            mCurrentItem = null;
        }
        createPlaylistIndexes();
    }

    @Override
    public void onItemAdded(MediaPlaylist playlist, IContentObject itemAdded) {
        createPlaylistIndexes();
    }

    @Override
    public void onPlaylistCleared(MediaPlaylist playlist) {
        mCurrentIndex = -1;
        mCurrentItem = null;
        createPlaylistIndexes();
    }
}
