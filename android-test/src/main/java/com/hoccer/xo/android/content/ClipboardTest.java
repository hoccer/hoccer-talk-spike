package com.hoccer.xo.android.content;

import android.os.Parcel;
import android.test.InstrumentationTestCase;
import com.hoccer.talk.content.ContentMediaType;
import org.apache.tika.mime.MimeTypes;

public class ClipboardTest extends InstrumentationTestCase {

    private SelectedContent mTestContent;
    private Clipboard mClipboard;

    @Override
    protected void setUp() throws Exception {
        mClipboard = Clipboard.getInstance();
        mTestContent = createSelectedContentWithData();
    }

    public void testClipboardInitialization() {
        assertFalse(mClipboard.hasContent());
        assertNull(mClipboard.getContent());
    }

    public void testClipboardSetContent() {
        mClipboard.setContent(mTestContent);

        assertTrue(mClipboard.hasContent());
        assertEquals(mTestContent, mClipboard.getContent());
    }

    public void testClipboardClearContent() {
        mClipboard.setContent(mTestContent);
        mClipboard.clearContent();

        assertFalse(mClipboard.hasContent());
        assertNull(mClipboard.getContent());
    }

    public void testParcelableClipboardContent() {
        ClipboardContent content = ClipboardContent.fromContentObject(mTestContent);
        Parcel parcel = createParcelFor(content);
        ClipboardContent contentFromParcel = ClipboardContent.CREATOR.createFromParcel(parcel);

        assertEquals(content, contentFromParcel);
    }

    private SelectedContent createSelectedContentWithData() {
        SelectedContent sc = new SelectedContent("hello".getBytes());
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
