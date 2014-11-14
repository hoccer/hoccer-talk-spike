package com.hoccer.xo.android.view.model;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.TextView;
import com.hoccer.talk.client.model.TalkClientSmsToken;
import com.hoccer.xo.android.adapter.SearchAdapter;
import com.hoccer.xo.android.view.AvatarView;
import com.artcom.hoccer.R;
import org.apache.log4j.Logger;

public class SmsChatItem extends BaseChatItem implements SearchAdapter.Searchable{

    private static final Logger LOG = Logger.getLogger(SmsChatItem.class);

    private Context mContext;
    private TalkClientSmsToken mSmsToken;
    private String mName;
    private String mPhoto;

    public SmsChatItem(TalkClientSmsToken smsToken, Context context) {
        mSmsToken = smsToken;
        mContext = context;
        update();
    }

    @Override
    public void update() {
        ContentResolver resolver = mContext.getContentResolver();
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(mSmsToken.getSender()));

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
    protected View configure(Context context, View view) {
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
    public TalkClientSmsToken getContent() {
        return mSmsToken;
    }

    @Override
    public long getMessageTimeStamp() {
        return 0;
    }

    @Override
    public long getContactCreationTimeStamp() {
        return 0;
    }

    @Override
    public boolean matches(String query) {
        return mName.contains(query);
    }
}
