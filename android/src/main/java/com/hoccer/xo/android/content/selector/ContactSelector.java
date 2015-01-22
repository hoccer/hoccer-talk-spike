package com.hoccer.xo.android.content.selector;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.ContactsContract;
import com.artcom.hoccer.R;
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.content.SelectedContent;
import com.hoccer.xo.android.util.ColorSchemeManager;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class ContactSelector implements IContentSelector {

    private static final Logger LOG = Logger.getLogger(ContactSelector.class);

    private final String mName;
    private final Drawable mIcon;

    public ContactSelector(Context context) {
        mName = context.getResources().getString(R.string.content_contact);
        mIcon = ColorSchemeManager.getRepaintedDrawable(context.getResources(), R.drawable.ic_attachment_select_contact, true);
    }

    @Override
    public String getName() {
        return mName;
    }

    @Override
    public Drawable getContentIcon() {
        return mIcon;
    }

    @Override
    public Intent createSelectionIntent(Context context) {
        return new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
    }

    @Override
    public boolean isValidIntent(Context context, Intent intent) {
        return true;
    }

    @Override
    public SelectedContent createObjectFromSelectionResult(Context context, Intent intent) {
        boolean isValidIntent = isValidIntent(context, intent);
        if (!isValidIntent) {
            return null;
        }

        String lookupUri = intent.getDataString();
        String vcardUri = lookupUri.replace(ContactsContract.Contacts.CONTENT_LOOKUP_URI.toString(), ContactsContract.Contacts.CONTENT_VCARD_URI.toString());
        vcardUri = vcardUri.substring(0, vcardUri.lastIndexOf(File.separator));

        InputStream is = null;
        byte[] contactData;
        try {
            is = XoApplication.getXoClient().getHost().openInputStreamForUrl(vcardUri);
            contactData = IOUtils.toByteArray(is);
        } catch (IOException e) {
            LOG.error("Could not read contact details for selected contact: " + intent.getData());
            return null;
        } finally {
            IOUtils.closeQuietly(is);
        }

        SelectedContent contentObject = new SelectedContent(contactData);
        contentObject.setFileName("Contact");
        contentObject.setContentType(ContactsContract.Contacts.CONTENT_VCARD_TYPE);
        contentObject.setContentMediaType(ContentMediaType.VCARD);

        return contentObject;
    }
}
