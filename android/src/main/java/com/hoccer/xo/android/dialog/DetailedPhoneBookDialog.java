package com.hoccer.xo.android.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.*;
import com.hoccer.xo.android.adapter.AddressBookDialogAdapter.ContactsQuery;
import com.hoccer.xo.release.R;

import java.util.HashSet;
import java.util.Set;

public class DetailedPhoneBookDialog extends Dialog {

    private DetailPhonebookAdapter mAdapter;
    private Button mInviteButton;
    private Button mCancelButton;
    private ListView mListView;
    private Context mContext;
    private Cursor mCursor;
    private Bitmap mThumbNailBeatleMap;
    private Set<String> mSelectedRecipientsSet = new HashSet<String>();
    private Set<String> mDeselectedRecipientsSet = new HashSet<String>();
    private Set<String> mAlreadyInvited;//We don't need new object - will use link to set from AddressBookDialogAdapter

    public DetailedPhoneBookDialog(Context context, String selection, String[] id, Bitmap thumbNail, Set<String> alreadyInvited) {
//        super(context, R.style.ButtonAppBaseTheme);
        super(context);
        mContext = context;
        mCursor = context.getContentResolver().query(ContactsQuery.CONTENT_URI, ContactsQuery.PROJECTION, selection,
                id, null);
        mThumbNailBeatleMap = thumbNail;
        mAlreadyInvited = alreadyInvited;
        setCancelable(false);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_phonebook_detail);
        mAdapter = new DetailPhonebookAdapter(mContext, mThumbNailBeatleMap);
        mAdapter.changeCursor(mCursor);
        mListView = (ListView) findViewById(R.id.lv_address_book_dialog);
        mListView.setAdapter(mAdapter);
        mCancelButton = (Button) findViewById(R.id.bt_cancel);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSelectedRecipientsSet.clear();
                mDeselectedRecipientsSet.clear();
                dismiss();
            }
        });
        mInviteButton = (Button) findViewById(R.id.bt_invite);
        mInviteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
    }

    public void closeCursor() {
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
        }
    }

    public Set<String> getSelectedNumbers() {
        return mSelectedRecipientsSet;
    }

    public Set<String> getDeselectedNumbers() {
        return mDeselectedRecipientsSet;
    }

    private class DetailPhonebookAdapter extends CursorAdapter {
        private LayoutInflater inflater;
        private Bitmap thumbnailBitmap = null;


        public DetailPhonebookAdapter(Context context, Bitmap bitmap) {
            super(context, null, 0);
            thumbnailBitmap = bitmap;
            inflater = LayoutInflater.from(context);
        }

        private class ViewHolder {
            TextView displayName;
            TextView detailedInfo;
            QuickContactBadge quickContact;
            CheckBox checkBox;
            RelativeLayout clicker;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            final View itemView = inflater.inflate(R.layout.item_dialog_multi_invitation, viewGroup, false);
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
            final String displayName = cursor.getString(ContactsQuery.DISPLAY_NAME);
            final String detailedInfo = cursor.getString(ContactsQuery.MOBILE_PHONE_NUMBER).equals("") ?
                    cursor.getString(ContactsQuery.EMAIL_ADDRESS) :
                    cursor.getString(ContactsQuery.MOBILE_PHONE_NUMBER);


            holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                    if (checked) {
                        mSelectedRecipientsSet.add(detailedInfo);
                        mDeselectedRecipientsSet.remove(detailedInfo);
                    } else {
                        if (mAlreadyInvited.contains(detailedInfo) || mSelectedRecipientsSet.contains(detailedInfo)) {
                            mDeselectedRecipientsSet.add(detailedInfo);
                            mSelectedRecipientsSet.remove(detailedInfo);
                        }
                    }
                }
            });

            if (mAlreadyInvited.contains(detailedInfo)) {
                holder.checkBox.setChecked(true);
            } else {
                holder.checkBox.setChecked(false);
            }
            holder.displayName.setText(displayName);
            holder.detailedInfo.setText(detailedInfo);
            final Uri contactUri = ContactsContract.Contacts.getLookupUri(
                    cursor.getLong(ContactsQuery.ID),
                    cursor.getString(ContactsQuery.LOOKUP_KEY));
            holder.quickContact.assignContactUri(contactUri);
            if (thumbnailBitmap != null) {
                holder.quickContact.setImageBitmap(thumbnailBitmap);
            } else {
                holder.quickContact.setImageToDefault();
            }
            holder.clicker.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    holder.checkBox.setChecked(!holder.checkBox.isChecked());
                }
            });
        }
    }
}
