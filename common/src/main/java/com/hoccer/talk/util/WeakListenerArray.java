package com.hoccer.talk.util;

import java.lang.ref.WeakReference;
import java.util.*;


/**
 * Encapsulates a list of weak referenced listeners.
 */
public class WeakListenerArray<Listener> implements Iterable<Listener>{

    private ArrayList<WeakReference<Listener>> mItems = new ArrayList<WeakReference<Listener>>();

    // Adds the given listener to array
    public void addListener(Listener listener) {
        if(!mItems.contains(listener)) {
            mItems.add(new WeakReference<Listener>(listener));
        }
    }

    // Removes the given listener instance from array
    public void removeListener(Listener listener) {
        for (Iterator<WeakReference<Listener>> iterator = mItems.iterator();
             iterator.hasNext(); ) {
            WeakReference<Listener> weakListener = iterator.next();
            if (weakListener.get() == listener) {
                iterator.remove();
            }
        }
    }

    @Override
    public Iterator<Listener> iterator() {
        return new Iterator<Listener>() {
            int mCurrentIndex = 0;

            @Override
            public boolean hasNext() {
                seekNextValid();
                return mCurrentIndex < mItems.size();
            }

            @Override
            public Listener next() {
                seekNextValid();
                if(mCurrentIndex < mItems.size()) {
                    return mItems.get(mCurrentIndex++).get();
                } else {
                    throw new NoSuchElementException("There is no next listener in listener array.");
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Listener array must not be modified through iterator.");
            }

            private void seekNextValid() {
                while(mCurrentIndex < mItems.size() && mItems.get(mCurrentIndex).get() == null) {
                    mItems.remove(mCurrentIndex);
                }
            }
        };
    }
}
