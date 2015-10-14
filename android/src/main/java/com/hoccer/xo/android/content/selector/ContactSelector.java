package com.hoccer.xo.android.content.selector;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.provider.ContactsContract;
import com.artcom.hoccer.R;
import com.hoccer.talk.content.SelectedContent;
import com.hoccer.xo.android.content.SelectedContact;
import com.hoccer.xo.android.util.UriUtils;
import com.hoccer.xo.android.util.colorscheme.ColoredDrawable;

import java.io.File;

public class ContactSelector implements IContentSelector {

    private final String mName;
    private final Drawable mIcon;

    public ContactSelector(Context context) {
        mName = context.getResources().getString(R.string.content_contact);
        mIcon = ColoredDrawable.getFromCache(R.drawable.ic_attachment_select_contact, R.color.primary);
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
    public SelectedContent createObjectFromSelectionResult(Context context, Intent intent) {
        String dataUri = intent.getDataString();
        if (UriUtils.isLookUpUri(dataUri)) {
            dataUri = replaceLookUpUriWithVCardUri(dataUri);
        }

        return new SelectedContact(dataUri);
    }

    private String replaceLookUpUriWithVCardUri(String lookUpUri) {
        String vCardUri = lookUpUri.replace(ContactsContract.Contacts.CONTENT_LOOKUP_URI.toString(), ContactsContract.Contacts.CONTENT_VCARD_URI.toString());
        return vCardUri.substring(0, vCardUri.lastIndexOf(File.separator));
    }
}
