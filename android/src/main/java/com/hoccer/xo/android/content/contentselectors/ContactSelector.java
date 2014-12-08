package com.hoccer.xo.android.content.contentselectors;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.ContactsContract;
import com.artcom.hoccer.R;
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.xo.android.content.SelectedContent;
import com.hoccer.xo.android.util.ColorSchemeManager;
import org.apache.log4j.Logger;

import java.io.FileNotFoundException;

public class ContactSelector implements IContentSelector {

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

        Uri selectedContent = intent.getData();
        String[] columns = {
                ContactsContract.Contacts.LOOKUP_KEY
        };

        Cursor cursor = context.getContentResolver().query(selectedContent, columns, null, null, null);
        cursor.moveToFirst();
        int lookupKeyIndex = cursor.getColumnIndex(columns[0]);
        String lookupKey = cursor.getString(lookupKeyIndex);
        cursor.close();

        Uri contentUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey);
        String contentUriPath = contentUri.toString();

        if (contentUriPath.startsWith("content:/com.android.contacts")) {
            contentUriPath = contentUriPath.replace("content:/com.android.contacts", "content://com.android.contacts");
        }

        SelectedContent contentObject = new SelectedContent(intent, contentUriPath);
        contentObject.setFileName("Contact");
        contentObject.setContentType(ContactsContract.Contacts.CONTENT_VCARD_TYPE);
        contentObject.setContentMediaType(ContentMediaType.VCARD);

        AssetFileDescriptor fileDescriptor;
        long fileSize = 0;
        try {
            fileDescriptor = context.getContentResolver().openAssetFileDescriptor(contentUri, "r");
            fileSize = fileDescriptor.getLength();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        contentObject.setContentLength((int) fileSize);

        return contentObject;
    }
}
