package com.hoccer.xo.android.content;

import com.hoccer.talk.content.SelectedContent;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;

import static junit.framework.TestCase.*;

public class ClipboardTest {

    private SelectedContent mTestContent;
    private Clipboard mClipboard;

    @Before
    public void setUp() throws Exception {
        resetStaticClipboardInstance();
        mClipboard = Clipboard.get();
        mTestContent = new SelectedLocation("hello".getBytes());
    }

    private static void resetStaticClipboardInstance() {
        try {
            Field clipboardInstance = Clipboard.class.getDeclaredField("sInstance");
            clipboardInstance.setAccessible(true);
            clipboardInstance.set(null, null);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void clipboardInitialization() {
        assertFalse(mClipboard.hasContent());
        assertNull(mClipboard.getContent());
    }

    @Test
    public void clipboardSetContent() {
        mClipboard.setContent(mTestContent);

        assertTrue(mClipboard.hasContent());
        assertEquals(mTestContent, mClipboard.getContent());
    }

    @Test
    public void clipboardClearContent() {
        mClipboard.setContent(mTestContent);
        mClipboard.clearContent();

        assertFalse(mClipboard.hasContent());
        assertNull(mClipboard.getContent());
    }
}
