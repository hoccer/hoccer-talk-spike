package com.hoccer.xo.android.view.model;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.activity.GroupProfileActivity;
import com.hoccer.xo.android.activity.SingleProfileActivity;
import com.hoccer.xo.android.adapter.SearchAdapter;
import com.hoccer.xo.android.view.AvatarView;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TalkClientChatItem extends BaseChatItem implements SearchAdapter.Searchable{

    private static final Logger LOG = Logger.getLogger(TalkClientChatItem.class);

    private Context mContext;
    private TalkClientContact mContact;

    @Nullable
    private Date mLastMessageTimeStamp = null;
    private String mLastMessageText;
    private long mUnseenMessageCount = 0;
    private Date mContactCreationTimeStamp = null;

    public TalkClientChatItem(TalkClientContact contact, Context context) {
        mContact = contact;
        mContext = context;
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
        if (mLastMessageTimeStamp != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE HH:mm");
            lastMessageTime.setText(sdf.format(mLastMessageTimeStamp));
        }
    }

    public void update() {
        try {
            mUnseenMessageCount = XoApplication.getXoClient().getDatabase().findUnseenMessageCountByContactId(mContact.getClientContactId());
            TalkClientMessage message = XoApplication.getXoClient().getDatabase().findLatestMessageByContactId(mContact.getClientContactId());
            if (message != null) {
                mLastMessageTimeStamp = message.getTimestamp();
                updateLastMessageText(message);
            }
            TalkClientContact contact = XoApplication.getXoClient().getDatabase().findContactById(mContact.getClientContactId());
            if (contact != null) {
                mContact = contact;
                mContactCreationTimeStamp = contact.getCreatedTimeStamp();
            }
        } catch (SQLException e) {
            LOG.error("sql error", e);
            // TODO: add proper exception handling
        }
    }

    private void updateLastMessageText(TalkClientMessage message) {
        if (message != null) {
            String mediaType = null;
            String text = null;
            TalkClientUpload upload = message.getAttachmentUpload();
            if (upload != null) {
                mediaType = upload.getMediaType();
                text = mContext.getResources().getString(R.string.contact_item_sent_attachment);
            } else {
                TalkClientDownload download = message.getAttachmentDownload();
                if (download != null) {
                    mediaType = download.getMediaType();
                    text = mContext.getResources().getString(R.string.contact_item_received_attachment);
                }
            }
            if (text != null) {
                mLastMessageText = String.format(text, mediaType);
            } else {
                mLastMessageText = message.getText();
            }
        } else {
            mLastMessageText = "";
        }
    }

    @Override
    protected View configure(final Context context, View view) {
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
                Intent intent;
                if (mContact.isGroup()) {
                    intent = new Intent(context, GroupProfileActivity.class);
                    intent.putExtra(GroupProfileActivity.EXTRA_CLIENT_CONTACT_ID,
                            mContact.getClientContactId());
                } else {
                    intent = new Intent(context, SingleProfileActivity.class);
                    intent.putExtra(SingleProfileActivity.EXTRA_CLIENT_CONTACT_ID,
                            mContact.getClientContactId());
                }
                context.startActivity(intent);
            }
        });

        return view;
    }

    private void setUnseenMessages(TextView unseenView) {
        if (mUnseenMessageCount <= 0) {
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
    public long getMessageTimeStamp() {
        if (mLastMessageTimeStamp == null) {
            return 0;
        }
        return mLastMessageTimeStamp.getTime();
    }

    @Override
    public long getContactCreationTimeStamp() {
        if (mContactCreationTimeStamp == null) {
            return 0;
        }
        return mContactCreationTimeStamp.getTime();
    }

    @Override
    public boolean matches(String query) {
        return mContact.getName().toLowerCase().contains(query.toLowerCase()) || mContact.getNickname().toLowerCase()
                .contains(query.toLowerCase());
    }
}
