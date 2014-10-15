package com.hoccer.xo.android.content;

import android.os.Parcel;
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

    @Test
    public void testParcelableClipboardContent() {
        ClipboardContent content = ClipboardContent.fromContentObject(mTestContent);
        Parcel parcel = createParcelFor(content);
        ClipboardContent contentFromParcel = ClipboardContent.CREATOR.createFromParcel(parcel);

        assertEquals(content, contentFromParcel);
    }

    private SelectedContent createSelectedContentWithData() {
        SelectedContent sc = new SelectedContent(new byte[1]);
        sc.setContentMediaType(ContentMediaType.DATA);
        sc.setFileName("random_content.txt");
        sc.setContentType(MimeTypes.PLAIN_TEXT);
        return sc;
    }

    private Parcel createParcelFor(ClipboardContent content) {
        Parcel parcel = Parcel.obtain();
        content.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        return parcel;
    }
}
