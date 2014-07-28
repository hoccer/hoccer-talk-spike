package com.hoccer.xo.android.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.hoccer.xo.android.dialog.DetailedPhoneBookDialog;
import com.hoccer.xo.release.R;
import org.omg.DynamicAny._DynAnyFactoryStub;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class AddressBookDialogAdapter extends CursorAdapter {

    public static String mSearchTerm;
    private LayoutInflater mInflater;
    private Context mContext;
//    private TextAppearanceSpan highlightTextSpan;
    private Set<String> mRecipientsSet = new HashSet<String>();
    private boolean mIsSmsInvitation;
    private boolean mOnCheckedCalledFromOnChecked = false;

    public AddressBookDialogAdapter(Context context, boolean isSmsInvitation) {
        super(context, null, 0);
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mIsSmsInvitation = isSmsInvitation;
//        highlightTextSpan = new TextAppearanceSpan(mContext, R.style.searchTextHiglight);
    }

    private class ViewHolder {
        TextView displayName;
        TextView detailedInfo;
        QuickContactBadge quickContact;
        RelativeLayout clicker;
        CheckBox checkBox;
    }

    private int indexOfSearchQuery(String displayName) {
        if (!TextUtils.isEmpty(mSearchTerm)) {
            return displayName.toLowerCase(Locale.getDefault()).indexOf(
                    mSearchTerm.toLowerCase(Locale.getDefault()));
        }
        return -1;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        final View itemView = mInflater.inflate(R.layout.item_dialog_multi_invitation, viewGroup, false);
        final ViewHolder holder = new ViewHolder();
        holder.displayName = (TextView) itemView.findViewById(R.id.tv_displayname);
        holder.quickContact = (QuickContactBadge) itemView.findViewById(R.id.cb_quickcontact);
        holder.detailedInfo = (TextView) itemView.findViewById(R.id.tv_detailed_info);
        holder.checkBox = (CheckBox) itemView.findViewById(R.id.checkBox);
        holder.clicker = (RelativeLayout) itemView.findViewById(R.id.clickablelayout);
        itemView.setTag(holder);
        return itemView;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final ViewHolder holder = (ViewHolder) view.getTag();
        final String photoData = cursor.getString(ContactsQuery.PHOTO_THUMBNAIL_DATA);
        final String displayName = cursor.getString(ContactsQuery.DISPLAY_NAME);
        String info = "";
        String detailSelection = "";
        if (mIsSmsInvitation) {
            info =  cursor.getString(ContactsQuery.MOBILE_PHONE_NUMBER);
            detailSelection = Contacts.LOOKUP_KEY + "=?" + " AND " + ContactsContract.Data.MIMETYPE  + "='" +
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "'";
        } else {
            info = cursor.getString(ContactsQuery.EMAIL_ADDRESS);
            detailSelection = Contacts.LOOKUP_KEY + "=?" + " AND " + ContactsContract.Data.MIMETYPE  + "='" +
                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE + "'";
        }
        final String detailedInfo = info;
        final Uri contactUri = Contacts.getLookupUri(
                cursor.getLong(ContactsQuery.ID),
                cursor.getString(ContactsQuery.LOOKUP_KEY));
        holder.quickContact.assignContactUri(contactUri);
        final Bitmap thumbnailBitmap =  loadContactPhotoThumbnail(photoData);
        if (thumbnailBitmap != null) {
            holder.quickContact.setImageBitmap(thumbnailBitmap);
        } else {
            holder.quickContact.setImageToDefault();
        }
        final String selection = detailSelection;
        final String[] id = new String[]{cursor.getString(ContactsQuery.LOOKUP_KEY)};
        Cursor individualContactCursor = mContext.getContentResolver().query(ContactsQuery.CONTENT_URI,
                ContactsQuery.PROJECTION, selection, id, null);
        individualContactCursor.moveToFirst();
        if (individualContactCursor.getCount() > 1) {
            holder.detailedInfo.setText(R.string.invite_several_entries);
            holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                    holder.checkBox.setChecked(!checked);
                    if (holder.detailedInfo.getText().toString().
                            equals(mContext.getResources().getString(R.string.invite_several_entries))) {
                        holder.checkBox.setChecked(false);
                    } else {
                        holder.checkBox.setChecked(true);
                    }
                    if (!mOnCheckedCalledFromOnChecked) {
                        final DetailedPhoneBookDialog d = new DetailedPhoneBookDialog(mContext, selection, id, thumbnailBitmap, mRecipientsSet);
                        d.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialogInterface) {
                                if (d.getDeselectedNumbers().isEmpty() && d.getSelectedNumbers().isEmpty()) {
                                    mOnCheckedCalledFromOnChecked = false;
                                    return;
                                }
                                mOnCheckedCalledFromOnChecked = true;
                                mRecipientsSet.removeAll(d.getDeselectedNumbers());
                                mRecipientsSet.addAll(d.getSelectedNumbers());
                                String info = "";
                                for (String number: d.getSelectedNumbers()) {
                                    info += number + ", ";
                                }
                                if (info.isEmpty()) {
                                    if (!holder.checkBox.isChecked()) {
                                        mOnCheckedCalledFromOnChecked = false;
                                    }
                                    holder.detailedInfo.setText(R.string.invite_several_entries);
                                    holder.checkBox.setChecked(false);
                                } else {
                                    info = info.substring(0, info.lastIndexOf(","));
                                    holder.detailedInfo.setText(info);
                                    if (holder.checkBox.isChecked()) {
                                        mOnCheckedCalledFromOnChecked = false;
                                    }
                                    holder.checkBox.setChecked(true);
                                }
                                d.closeCursor();
                            }
                        });
                        d.show();
                    } else {
                        mOnCheckedCalledFromOnChecked = false;
                    }
                }
            });
        } else {
            holder.detailedInfo.setText(detailedInfo);
            holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                    if (checked) {
                        mRecipientsSet.add(detailedInfo);
                    } else {
                        mRecipientsSet.remove(detailedInfo);
                    }
                }
            });
        }
        individualContactCursor.close();
        if (mRecipientsSet.contains(detailedInfo)) {
            holder.checkBox.setChecked(true);
        } else {
            holder.checkBox.setChecked(false);
        }
        holder.displayName.setText(displayName);
        final int startIndex = indexOfSearchQuery(displayName);
        if (startIndex == -1) {
            holder.displayName.setText(displayName);
        } else {
//            final SpannableString highlightedName = new SpannableString(displayName);
//            highlightedName.setSpan(highlightTextSpan, startIndex,
//                    startIndex + mSearchTerm.length(), 0);
//            holder.displayName.setText(highlightedName);
        }
        holder.clicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                holder.checkBox.setChecked(!holder.checkBox.isChecked());
            }
        });
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
                        "<>''" + " AND " + ContactsContract.CommonDataKinds.Phone.IS_PRIMARY +
                        "<>'0'" + " AND " + ContactsContract.Data.MIMETYPE  + "='" +
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "'";

        @SuppressLint("InlinedApi")
        final static String SELECTION_WITH_PHONES_FILTERED =
                (hasHoneycomb() ? Contacts.DISPLAY_NAME_PRIMARY : Contacts.DISPLAY_NAME) +
                        " LIKE ? " + " AND " + ContactsContract.CommonDataKinds.Phone.IS_PRIMARY +
                        "<>'0'" + " AND " + ContactsContract.Data.MIMETYPE  + "='" +
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "'";

        @SuppressLint("InlinedApi")
        final static String SELECTION_WITH_EMAILS =
                (hasHoneycomb() ? Contacts.DISPLAY_NAME_PRIMARY : Contacts.DISPLAY_NAME) +
                        "<>''" + " AND " + ContactsContract.CommonDataKinds.Email.IS_PRIMARY +
                        "<>'0'" + " AND " + ContactsContract.Data.MIMETYPE  + "='" +
                        ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE + "'";

        @SuppressLint("InlinedApi")
        final static String SELECTION_WITH_EMAILS_FILTERED =
                (hasHoneycomb() ? Contacts.DISPLAY_NAME_PRIMARY : Contacts.DISPLAY_NAME) +
                        " LIKE ? " + " AND " + ContactsContract.CommonDataKinds.Email.IS_PRIMARY +
                        "<>'0'" + " AND " + ContactsContract.Data.MIMETYPE  + "='" +
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
        for (String recipient: mRecipientsSet) {
            result += recipient + ";";
        }
        return result;
    }
}