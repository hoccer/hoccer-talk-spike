package com.hoccer.xo.android.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.QuickContactBadge;
import android.widget.TextView;
import com.hoccer.xo.release.R;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class AddressBookDialogAdapter extends CursorAdapter {

    private LayoutInflater mInflater;
    private Context mContext;
    private ArrayList<String> mRecipientsList = new ArrayList<String>();

    public AddressBookDialogAdapter(Context context) {
        super(context, null, 0);
        mContext = context;
        mInflater = LayoutInflater.from(context);
    }

    private class ViewHolder {
        TextView displayName;
        TextView detailedInfo;
        QuickContactBadge quickContact;
        CheckBox checkBox;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        final View itemView = mInflater.inflate(R.layout.item_dialog_multi_invitation, viewGroup, false);
        final ViewHolder holder = new ViewHolder();
        holder.displayName = (TextView) itemView.findViewById(R.id.tv_displayname);
        holder.quickContact = (QuickContactBadge) itemView.findViewById(R.id.cb_quickcontact);
        holder.detailedInfo = (TextView) itemView.findViewById(R.id.tv_detailed_info);
        holder.checkBox = (CheckBox) itemView.findViewById(R.id.checkBox);
        itemView.setTag(holder);
        return itemView;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final ViewHolder holder = (ViewHolder) view.getTag();
        final String photoData = cursor.getString(ContactsQuery.PHOTO_THUMBNAIL_DATA);
        final String displayName = cursor.getString(ContactsQuery.DISPLAY_NAME);
        final String detailedInfo = cursor.getString(ContactsQuery.MOBILE_PHONE_NUMBER).equals("") ?
                cursor.getString(ContactsQuery.EMAIL_ADDRESS) : cursor.getString(ContactsQuery.MOBILE_PHONE_NUMBER);
        holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                if (checked) {
                    mRecipientsList.add(detailedInfo);
                } else {
                    mRecipientsList.remove(detailedInfo);
                }
            }
        });
        holder.displayName.setText(displayName);
        holder.detailedInfo.setText(detailedInfo);
        final Uri contactUri = Contacts.getLookupUri(
                cursor.getLong(ContactsQuery.ID),
                cursor.getString(ContactsQuery.LOOKUP_KEY));
        holder.quickContact.assignContactUri(contactUri);
        Bitmap thumbnailBitmap =  loadContactPhotoThumbnail(photoData);
        if (thumbnailBitmap != null) {
            holder.quickContact.setImageBitmap(thumbnailBitmap);
        } else {
            holder.quickContact.setImageToDefault();
        }
    }

    private Bitmap loadContactPhotoThumbnail(String photoData) {
        if (photoData == null) {
            return null;
        }
        AssetFileDescriptor afd = null;
        try {
            Uri thumbUri;
            if (hasHoneycomb()) {
                thumbUri = Uri.parse(photoData);
            } else {
                final Uri contactUri = Uri.withAppendedPath(Contacts.CONTENT_URI, photoData);
                thumbUri = Uri.withAppendedPath(contactUri, Contacts.Photo.CONTENT_DIRECTORY);
            }
            afd = mContext.getContentResolver().openAssetFileDescriptor(thumbUri, "r");
            FileDescriptor fileDescriptor = afd.getFileDescriptor();
            if (fileDescriptor != null) {
                return BitmapFactory.decodeFileDescriptor(fileDescriptor);
            }
        } catch (FileNotFoundException e) {

        } finally {
            if (afd != null) {
                try {
                    afd.close();
                } catch (IOException e) {
                }
            }
        }
        return null;
    }

    public interface ContactsQuery {
        final static int QUERY_ID = 1;

        final static Uri CONTENT_URI = ContactsContract.Data.CONTENT_URI;

        @SuppressLint("InlinedApi")
        final static String SELECTION_WITH_PHONES =
                (hasHoneycomb() ? Contacts.DISPLAY_NAME_PRIMARY : Contacts.DISPLAY_NAME) +
                        "<>''" + " AND " + ContactsContract.CommonDataKinds.Phone.TYPE +
                        "=" + ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE;

        @SuppressLint("InlinedApi")
        final static String SELECTION_WITH_EMAILS =
                (hasHoneycomb() ? Contacts.DISPLAY_NAME_PRIMARY : Contacts.DISPLAY_NAME) +
                        "<>''" + " AND " + ContactsContract.Data.MIMETYPE + "=" + "='" +
                        ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE + "'";

        @SuppressLint("InlinedApi")
        final static String SORT_ORDER = hasHoneycomb() ? Contacts.SORT_KEY_PRIMARY :
                Contacts.DISPLAY_NAME;

        @SuppressLint("InlinedApi")
        final static String[] PROJECTION = {

                Contacts._ID,

                Contacts.LOOKUP_KEY,

                hasHoneycomb() ? Contacts.DISPLAY_NAME_PRIMARY :
                        Contacts.DISPLAY_NAME,

                hasHoneycomb() ? Contacts.PHOTO_THUMBNAIL_URI :
                        Contacts._ID,

                ContactsContract.CommonDataKinds.Phone.NUMBER,

                ContactsContract.CommonDataKinds.Email.ADDRESS,

                SORT_ORDER,
        };

        final static int ID = 0;
        final static int LOOKUP_KEY = 1;
        final static int DISPLAY_NAME = 2;
        final static int PHOTO_THUMBNAIL_DATA = 3;
        final static int MOBILE_PHONE_NUMBER = 4;
        final static int EMAIL_ADDRESS = 5;
    }

    public static boolean hasHoneycomb() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }


    public String getRecipients() {
        String result = "";
        for (String recipient: mRecipientsList) {
            result += recipient + ";";
        }
        return result;
    }
}