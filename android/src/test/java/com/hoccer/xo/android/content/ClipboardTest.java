package com.hoccer.xo.android.content;

import com.hoccer.talk.content.ContentMediaType;
import org.apache.tika.mime.MimeTypes;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.*;

public class ClipboardTest {

    private SelectedContent mTestContent;
    private Clipboard mClipboard;

    @Before
    public void setUp() throws Exception {
        mClipboard = Clipboard.getInstance();
        mTestContent = createSelectedContentWithData();
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

    private static SelectedContent createSelectedContentWithData() {
        SelectedContent sc = new SelectedContent("hello".getBytes());
        sc.setContentMediaType(ContentMediaType.DATA);
        sc.setFileName("random_content.txt");
        sc.setContentType(MimeTypes.PLAIN_TEXT);
        return sc;
    }
}
