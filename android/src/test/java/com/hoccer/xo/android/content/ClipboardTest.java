package com.hoccer.xo.android.content;

import com.hoccer.talk.content.SelectedContent;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.*;

public class ClipboardTest {

    private SelectedContent mTestContent;
    private Clipboard mClipboard;

    @Before
    public void setUp() throws Exception {
        mClipboard = Clipboard.getInstance();
        mTestContent = new SelectedLocation("hello".getBytes());
    }

    @Test
    public void testClipboardInitialization() {
        assertFalse(mClipboard.hasContent());
        assertNull(mClipboard.getContent());
    }

    @Test
    public void testClipboardSetContent() {
        mClipboard.setContent(mTestContent);

        assertTrue(mClipboard.hasContent());
        assertEquals(mTestContent, mClipboard.getContent());
    }

    @Test
    public void testClipboardClearContent() {
        mClipboard.setContent(mTestContent);
        mClipboard.clearContent();

        assertFalse(mClipboard.hasContent());
        assertNull(mClipboard.getContent());
    }
}
