package com.hoccer.talk.util;

import java.lang.ref.WeakReference;
import java.util.*;


/**
 * Encapsulates a list of weak referenced listeners.
 */
public class WeakListenerArray<Listener> implements Iterable<Listener>{

    private ArrayList<WeakReference<Listener>> mItemList = new ArrayList<WeakReference<Listener>>();

    // Adds the given listener to array
    public void registerListener(Listener listener) {
        if(!mItemList.contains(listener)) {
            mItemList.add(new WeakReference<Listener>(listener));
        }
    }

    // Removes the given listener instance from array
    public void unregisterListener(Listener listener) {
        for (Iterator<WeakReference<Listener>> iterator = mItemList.iterator();
             iterator.hasNext(); ) {
            WeakReference<Listener> weakListener = iterator.next();
            if (weakListener.get() == listener) {
                iterator.remove();
                return;
            }
        }
    }

    public void unregisterAll() {
        mItemList.clear();
    }

    public int size() {
        return mItemList.size();
    }

    @Override
    public Iterator<Listener> iterator() {
        return new Iterator<Listener>() {
            int mCurrentIndex = 0;

            @Override
            public boolean hasNext() {
                seekNextValid();
                return mCurrentIndex < mItemList.size();
            }

            @Override
            public Listener next() {
                seekNextValid();
                if(mCurrentIndex < mItemList.size()) {
                    return mItemList.get(mCurrentIndex++).get();
                } else {
                    throw new NoSuchElementException("There is no next listener in listener array.");
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Listener array must not be modified through iterator.");
            }

            private void seekNextValid() {
                while(mCurrentIndex < mItemList.size() && mItemList.get(mCurrentIndex).get() == null) {
                    mItemList.remove(mCurrentIndex);
                }
            }
        };
    }
}
