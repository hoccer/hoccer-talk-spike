package com.hoccer.xo.android.view.model;

import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.xo.android.adapter.BetterContactsAdapter;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.view.AvatarView;
import com.hoccer.xo.release.R;

import org.apache.log4j.Logger;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
* Created by jacob on 22.07.14.
*/
public class ContactItem extends BaseContactItem {

    private static final Logger LOG = Logger.getLogger(ContactItem.class);

    private BetterContactsAdapter mBetterContactsAdapter;

    private TalkClientContact mContact;

    private String mLastMessageText;
    private Date mLastMessageTimeStamp;
    private long mUnseenMessageCount;

    public ContactItem(XoActivity activity, BetterContactsAdapter betterContactsAdapter, TalkClientContact contact) {
        super(activity);
        mBetterContactsAdapter = betterContactsAdapter;
        mContact = contact;
        update();
    }

    public TalkClientContact getTalkClientContact() {
        return mContact;
    }

    private void setClientType(TextView typeView) {
        if (mContact.isGroup()) {
            if (mContact.isGroupInvited()) {
                typeView.setText(R.string.common_group_invite);
            } else {
                typeView.setText(R.string.common_group);
            }
        }
    }

    private void setLastMessageTime(TextView lastMessageTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE HH:mm");
        lastMessageTime.setText(sdf.format(mLastMessageTimeStamp));
    }

    public void update() {
        try { // TODO: add proper exception handling
            mUnseenMessageCount = mXoActivity.getXoDatabase().findUnseenMessageCountByContactId(mContact.getClientContactId());
            TalkClientMessage message = mXoActivity.getXoDatabase().findLatestMessageByContactId(mContact.getClientContactId());
            mLastMessageText = message.getText();
            mLastMessageTimeStamp = message.getTimestamp();
        } catch (SQLException e) {
            LOG.error("sql error", e);
        }
    }

    @Override
    protected View configure(View view) {
        AvatarView avatarView = (AvatarView) view.findViewById(R.id.contact_icon);
        TextView nameView = (TextView) view.findViewById(R.id.contact_name);
        TextView typeView = (TextView) view.findViewById(R.id.contact_type);
        TextView lastMessageTextView = (TextView) view.findViewById(R.id.contact_last_message);
        TextView lastMessageTimeView = (TextView) view.findViewById(R.id.contact_time);
        TextView unseenView = (TextView) view.findViewById(R.id.contact_unseen_messages);

        avatarView.setContact(mContact);
        nameView.setText(mContact.getNickname());
        setClientType(typeView);
        setLastMessageTime(lastMessageTimeView);
        lastMessageTextView.setText(mLastMessageText);
        unseenView.setText(Long.toString(mUnseenMessageCount));

        return view;
    }

}
