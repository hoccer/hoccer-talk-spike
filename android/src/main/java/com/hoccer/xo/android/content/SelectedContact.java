package com.hoccer.xo.android.content;

import android.provider.ContactsContract;
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.talk.content.SelectedContent;
import com.hoccer.xo.android.XoApplication;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;


public class SelectedContact extends SelectedContent {

    private static final Logger LOG = Logger.getLogger(SelectedContact.class);

    private final String mVcardContentUri;

    public SelectedContact(String vcardContentUri) {
        mVcardContentUri = vcardContentUri;
    }

    @Override
    protected String writeToFile() {
        InputStream is = null;
        File file = new File(XoApplication.getAttachmentDirectory(), UUID.randomUUID().toString());
        try {
            is = XoApplication.getXoClient().getHost().openInputStreamForUrl(mVcardContentUri);
            FileUtils.copyInputStreamToFile(is, file);
        } catch (IOException e) {
            LOG.error("Could not save contact vcard to file", e);
        } finally {
            IOUtils.closeQuietly(is);
        }
        return file.getPath();
    }

    @Override
    public String getContentMediaType() {
        return ContentMediaType.VCARD;
    }

    @Override
    public String getContentType() {
        return ContactsContract.Contacts.CONTENT_VCARD_TYPE;
    }

    @Override
    public double getAspectRatio() {
        return 0;
    }
}
