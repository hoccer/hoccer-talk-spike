package com.hoccer.xo.android.content;

import android.content.SharedPreferences;
import android.os.Parcel;
import android.preference.PreferenceManager;
import android.test.InstrumentationTestCase;
import com.hoccer.talk.content.ContentMediaType;
import org.apache.log4j.Logger;
import org.apache.tika.mime.MimeTypes;

public class ClipboardContentUnitTest extends InstrumentationTestCase {

    private static final Logger LOG = Logger.getLogger(ClipboardContentUnitTest.class);

    private static SharedPreferences sPreferences;
    private SelectedContent mTestContent;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        sPreferences = PreferenceManager.getDefaultSharedPreferences(this.getInstrumentation().getTargetContext());
        mTestContent = createSelectedContentWithData();
    }

    public void testParcelableClipboardContent() {
        LOG.info("Test parcelable ClipboardContent");
        ClipboardContent content = ClipboardContent.fromContentObject(mTestContent);
        Parcel parcel = createParcelFor(content);
        ClipboardContent contentFromParcel = ClipboardContent.CREATOR.createFromParcel(parcel);

        assertEquals(content, contentFromParcel);
    }

    public void testClipboardContentSavedInPreferences() {
        LOG.info("Test ClipboardContent saved in preferences");
        ClipboardContent content = ClipboardContent.fromContentObject(mTestContent);
        content.saveToPreferences(sPreferences.edit());
        ClipboardContent contentFromPreferences = ClipboardContent.fromPreferences(sPreferences);

        assertEquals(content, contentFromPreferences);
    }

    private SelectedContent createSelectedContentWithData() {
        LOG.info("Create test content obj");
        SelectedContent sc = new SelectedContent(new byte[1]);
        sc.setContentMediaType(ContentMediaType.DATA);
        sc.setFileName("random_content.txt");
        sc.setContentType(MimeTypes.PLAIN_TEXT);
        return sc;
    }

    private Parcel createParcelFor(ClipboardContent content) {
        LOG.info("Create Parcel");
        Parcel parcel = Parcel.obtain();
        LOG.info("Write ClipboardContent obj into Parcel");
        content.writeToParcel(parcel, 0);
        LOG.info("Reset Parcel parcel for reading");
        parcel.setDataPosition(0);
        LOG.info("Create ClipboardContent form Parcel");
        return parcel;
    }
}
