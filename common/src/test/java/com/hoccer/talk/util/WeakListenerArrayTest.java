package com.hoccer.talk.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.*;

public class WeakListenerArrayTest {
    private interface Listener {
        void onCall();
    }

    private WeakListenerArray<Listener> mListenerArray;

    @Before
    public void testSetup() {
        mListenerArray = new WeakListenerArray<Listener>();
    }

    @After
    public void testCleanup() {
        mListenerArray = null;
    }

    @Test
    public void testRegisterListener() {
        final ValueContainer<Boolean> onListenerCalled = new ValueContainer<Boolean>(false);

        Listener myListener = new Listener() {
            public void onCall() {
                onListenerCalled.value = true;
            }
        };

        mListenerArray.registerListener(myListener);

        assertEquals(1, mListenerArray.size());

        for (Listener listener : mListenerArray) {
            listener.onCall();
        }

        assertTrue(onListenerCalled.value);
    }

    @Test
    public void testUnregisterListener() {
        final ValueContainer<Boolean> onListenerCalled = new ValueContainer<Boolean>(false);

        Listener myListener = new Listener() {
            public void onCall() {
                onListenerCalled.value = true;
            }
        };

        mListenerArray.registerListener(myListener);
        mListenerArray.unregisterListener(myListener);

        for (Listener listener : mListenerArray) {
            listener.onCall();
        }

        assertFalse(onListenerCalled.value);
    }

    @Test
    public void testMultipleListener() {
        final ValueContainer<Integer> onListenerCalledCount = new ValueContainer<Integer>(0);

        Integer expectedListenerCount = 10;
        for (int i = 0; i < expectedListenerCount; i++) {
            mListenerArray.registerListener(new Listener() {
                public void onCall() {
                    onListenerCalledCount.value++;
                }
            });
        }

        for (Listener listener : mListenerArray) {
            listener.onCall();
        }

        assertEquals(expectedListenerCount, onListenerCalledCount.value);
    }

    @Test
    public void testUnregisterAllListener() {
        Integer listenerCount = 10;
        for (int i = 0; i < listenerCount; i++) {
            mListenerArray.registerListener(new Listener() {
                public void onCall() {
                }
            });
        }
        mListenerArray.unregisterAll();

        assertEquals(0, mListenerArray.size());
    }

    private class ValueContainer<T> {
        public T value;

        public ValueContainer(T initValue) {
            value = initValue;
        }
    }
}
