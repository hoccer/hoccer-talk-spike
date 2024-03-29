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

    private final String mVcardContentUrl;

    public SelectedContact(String vcardContentUrl) {
        super(null, ContactsContract.Contacts.CONTENT_VCARD_TYPE, ContentMediaType.VCARD);
        mVcardContentUrl = vcardContentUrl;
    }

    @Override
    public String writeContentToFile() {
        InputStream is = null;
        File file = new File(XoApplication.getAttachmentDirectory(), UUID.randomUUID().toString() + ".vcf");
        try {
            is = XoApplication.get().getClient().getHost().openInputStreamForUrl(mVcardContentUrl);
            FileUtils.copyInputStreamToFile(is, file);
        } catch (IOException e) {
            LOG.error("Could not save contact vcard to file", e);
        } finally {
            IOUtils.closeQuietly(is);
        }

        return file.getPath();
    }
}
