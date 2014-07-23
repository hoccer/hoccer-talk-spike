package com.hoccer.xo.android.view.model;

import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.xo.android.adapter.BetterContactsAdapter;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.view.AvatarView;
import com.hoccer.xo.release.R;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import android.view.View;
import android.widget.TextView;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TalkClientContactItem extends BaseContactItem {

    private static final Logger LOG = Logger.getLogger(TalkClientContactItem.class);

    private TalkClientContact mContact;

    private String mLastMessageText = "";
    @Nullable
    private Date mLastMessageTimeStamp = null;
    private long mUnseenMessageCount = 0;

    public TalkClientContactItem(XoActivity activity, TalkClientContact contact) {
        super(activity);
        mContact = contact;
        update();
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
        String text = "";
        if (mLastMessageTimeStamp != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE HH:mm");
            lastMessageTime.setText(sdf.format(mLastMessageTimeStamp));
        }
    }

    public void update() {
        try { // TODO: add proper exception handling
            mUnseenMessageCount = mXoActivity.getXoDatabase().findUnseenMessageCountByContactId(mContact.getClientContactId());
            TalkClientMessage message = mXoActivity.getXoDatabase().findLatestMessageByContactId(mContact.getClientContactId());
            if(message != null) {
                mLastMessageText = message.getText();
                mLastMessageTimeStamp = message.getTimestamp();
            }
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

        nameView.setText(mContact.getNickname());
        setClientType(typeView);
        setLastMessageTime(lastMessageTimeView);
        lastMessageTextView.setText(mLastMessageText);
        setUnseenMessages(unseenView);

        avatarView.setContact(mContact);
        avatarView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mXoActivity.showContactProfile(mContact);
            }
        });

        return view;
    }

    private void setUnseenMessages(TextView unseenView) {
        if(mUnseenMessageCount <= 0) {
            unseenView.setVisibility(View.GONE);
        } else {
            unseenView.setText(Long.toString(mUnseenMessageCount));
            unseenView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public TalkClientContact getContent() {
        return mContact;
    }

    @Override
    public long getTimeStamp() {
        if(mLastMessageTimeStamp == null) {
            return 0;
        }
        return mLastMessageTimeStamp.getTime();
    }

}
