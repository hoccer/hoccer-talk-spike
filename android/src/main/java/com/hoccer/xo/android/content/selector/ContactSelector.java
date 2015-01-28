package com.hoccer.xo.android.content.selector;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.provider.ContactsContract;
import com.artcom.hoccer.R;
import com.hoccer.talk.content.SelectedAttachment;
import com.hoccer.xo.android.content.SelectedContact;
import com.hoccer.xo.android.util.ColorSchemeManager;
import org.apache.log4j.Logger;

import java.io.File;

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
    public SelectedAttachment createObjectFromSelectionResult(Context context, Intent intent) {
        boolean isValidIntent = isValidIntent(context, intent);
        if (!isValidIntent) {
            return null;
        }

        String lookupUri = intent.getDataString();
        String vcardUri = lookupUri.replace(ContactsContract.Contacts.CONTENT_LOOKUP_URI.toString(), ContactsContract.Contacts.CONTENT_VCARD_URI.toString());
        vcardUri = vcardUri.substring(0, vcardUri.lastIndexOf(File.separator));
        return new SelectedContact(vcardUri);
    }
}
