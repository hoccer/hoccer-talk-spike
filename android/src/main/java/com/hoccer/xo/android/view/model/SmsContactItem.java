package com.hoccer.xo.android.view.model;

import com.hoccer.talk.client.model.TalkClientSmsToken;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.view.AvatarView;
import com.hoccer.xo.release.R;

import org.apache.log4j.Logger;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.TextView;

public class SmsContactItem extends BaseContactItem {

    private static final Logger LOG = Logger.getLogger(SmsContactItem.class);

    private TalkClientSmsToken mSmsToken;

    private String mName;

    private String mPhoto;

    public SmsContactItem(XoActivity activity, TalkClientSmsToken smsToken) {
        super(activity);
        mSmsToken = smsToken;
    }

    @Override
    public void update() {
        ContentResolver resolver = mXoActivity.getContentResolver();
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(mSmsToken.getSender()));

        mName = mSmsToken.getSender();
        mPhoto = "drawable://" + R.drawable.avatar_default_contact;

        Cursor cursor = resolver.query(uri,
                new String[]{
                        ContactsContract.PhoneLookup.DISPLAY_NAME,
                        ContactsContract.PhoneLookup.PHOTO_URI,
                },
                null, null, null);

        if (cursor != null && cursor.getCount() > 0) {
            int nameIndex = cursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME);
            int photoIndex = cursor.getColumnIndex(ContactsContract.Data.PHOTO_URI);
            cursor.moveToFirst();
            mName = cursor.getString(nameIndex);
            String contactPhoto = cursor.getString(photoIndex);
            if (contactPhoto != null) {
                mPhoto = contactPhoto;
            }
        }
    }

    @Override
    protected View configure(View view) {
        LOG.debug("updateToken(" + mSmsToken.getSmsTokenId() + ")");
        AvatarView avatarView = (AvatarView) view.findViewById(R.id.contact_icon);
        TextView nameView = (TextView) view.findViewById(R.id.contact_name);
        TextView typeView = (TextView) view.findViewById(R.id.contact_type);
        TextView lastMessageTextView = (TextView) view.findViewById(R.id.contact_last_message);
        TextView lastMessageTimeView = (TextView) view.findViewById(R.id.contact_time);
        TextView unseenView = (TextView) view.findViewById(R.id.contact_unseen_messages);

        avatarView.setAvatarImage(mPhoto);
        nameView.setText(mName);

        typeView.setText("");
        lastMessageTextView.setText("");
        lastMessageTimeView.setText("");
        unseenView.setVisibility(View.GONE);
        return view;
    }

    @Override
    protected TalkClientSmsToken getContent() {
        return mSmsToken;
    }


}
