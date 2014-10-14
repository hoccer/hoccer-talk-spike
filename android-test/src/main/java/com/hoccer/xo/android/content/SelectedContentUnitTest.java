package com.hoccer.xo.android.content;

import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import com.hoccer.talk.content.ContentMediaType;
import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.apache.tika.mime.MimeTypes;

public class SelectedContentUnitTest extends TestCase {

    private static final Logger LOG = Logger.getLogger(SelectedContentUnitTest.class);

    public void testParcelableContentWithData() {
        LOG.info("Test parcelable Content");
        SelectedContent content = createSelectedContentWithData();
        assertEquals(content, getSelectedContentFromParcel(content));
    }

    public void testParcelableContentWithContentUri() {
        LOG.info("Test parcelable Content");
        SelectedContent content = createSelectedContentWithContentUri();
        assertEquals(content, getSelectedContentFromParcel(content));
    }

    public void testParcelableContentWithIntent() {
        LOG.info("Test parcelable Content");
        SelectedContent content = createSelectedContentWithContentIntent();
        assertEquals(content, getSelectedContentFromParcel(content));
    }

    private SelectedContent createSelectedContentWithData() {
        LOG.info("Create SelectedContent obj");
        SelectedContent sc = new SelectedContent(new byte[1]);
        sc.setContentMediaType(ContentMediaType.DATA);
        sc.setFileName("random_content.txt");
        sc.setContentType(MimeTypes.PLAIN_TEXT);
        return sc;
    }

    private SelectedContent createSelectedContentWithContentUri() {
        LOG.info("Create SelectedContent obj with content-URI");
        SelectedContent sc = new SelectedContent("content://test", "file://test");
        sc.setContentMediaType(ContentMediaType.DATA);
        sc.setFileName("random_content.txt");
        sc.setContentType(MimeTypes.PLAIN_TEXT);
        return sc;
    }

    private SelectedContent createSelectedContentWithContentIntent() {
        LOG.info("Create SelectedContent obj with Intent");
        Intent i = new Intent();
        i.setData(new Uri.Builder().path("content://test").build());
        i.setType(MimeTypes.PLAIN_TEXT);
        SelectedContent sc = new SelectedContent(i, "file://test/random_content.txt");
        sc.setContentMediaType(ContentMediaType.DATA);
        return sc;
    }

    private SelectedContent getSelectedContentFromParcel(SelectedContent content) {
        return SelectedContent.CREATOR.createFromParcel(createParcelFor(content));
    }

    private Parcel createParcelFor(SelectedContent content) {
        LOG.info("Create Parcel");
        Parcel parcel = Parcel.obtain();
        LOG.info("Write SelectedContent obj into Parcel");
        content.writeToParcel(parcel, 0);
        LOG.info("Reset Parcel parcel for reading");
        parcel.setDataPosition(0);
        LOG.info("Create SelectedContent form Parcel");
        return parcel;
    }
}
