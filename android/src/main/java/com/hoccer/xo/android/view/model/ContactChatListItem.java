package com.hoccer.xo.android.view.model;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.client.predicates.TalkClientContactPredicates;
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.talk.model.TalkGroupMembership;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.adapter.SearchAdapter;
import com.hoccer.xo.android.profile.client.ClientProfileActivity;
import com.hoccer.xo.android.profile.group.GroupProfileActivity;
import com.hoccer.xo.android.view.avatar.AvatarView;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ContactChatListItem extends ChatListItem implements SearchAdapter.Searchable {

    private static final Logger LOG = Logger.getLogger(ContactChatListItem.class);

    protected TalkClientContact mContact;

    @Nullable
    private Date mLastMessageTimeStamp;
    private String mLastMessageText;
    private Date mContactCreationTimeStamp;

    public ContactChatListItem(TalkClientContact contact, Context context) {
        super(context);
        mContact = contact;
        update();
    }

    private void setLastMessageTime(TextView lastMessageTime) {
        if (mLastMessageTimeStamp != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE HH:mm");
            lastMessageTime.setText(sdf.format(mLastMessageTimeStamp));
        }
    }

    public void update() {
        try {
            XoClientDatabase database = XoApplication.get().getClient().getDatabase();
            mUnseenMessageCount = database.findUnseenMessageCountByContactId(mContact.getClientContactId());
            TalkClientMessage message = database.findLatestMessageByContactId(mContact.getClientContactId());
            if (message != null) {
                mLastMessageTimeStamp = message.getTimestamp();
                updateLastMessageText(message);
            }
            TalkClientContact contact = database.findContactById(mContact.getClientContactId());
            if (contact != null) {
                mContact = contact;
                mContactCreationTimeStamp = contact.getCreatedTimeStamp();
            }
        } catch (SQLException e) {
            LOG.error("sql error", e);
            // TODO: add proper exception handling
        }
    }

    @Override
    protected int getAvatarLayout() {
        return AvatarView.getLayoutResource(mContact);
    }

    @Override
    protected View updateView(View convertView) {
        TextView nameView = (TextView) convertView.findViewById(R.id.contact_name);
        TextView lastMessageTextView = (TextView) convertView.findViewById(R.id.contact_last_message);
        TextView lastMessageTimeView = (TextView) convertView.findViewById(R.id.contact_time);
        TextView unseenView = (TextView) convertView.findViewById(R.id.contact_unseen_messages);

        if (mContact.isWorldwideGroup()) {
            List<TalkClientContact> contacts = new ArrayList<TalkClientContact>();
            try {
                contacts = XoApplication.get().getClient().getDatabase().findContactsInGroupByState(mContact.getGroupId(), TalkGroupMembership.STATE_JOINED);
                CollectionUtils.filterInverse(contacts, TalkClientContactPredicates.IS_SELF_PREDICATE);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            nameView.setText(mContext.getResources().getString(R.string.all_worldwide) + " (" + contacts.size() + ")");
        } else if (mContact.isNearbyGroup()) {
            nameView.setText(R.string.all_nearby);
        } else {
            nameView.setText(mContact.getNickname());
        }
        setLastMessageTime(lastMessageTimeView);
        lastMessageTextView.setText(mLastMessageText);
        setUnseenMessages(unseenView);

        mAvatarView.setContact(mContact);
        mAvatarView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent;
                if (mContact.isGroup()) {
                    intent = new Intent(mContext, GroupProfileActivity.class)
                            .setAction(GroupProfileActivity.ACTION_SHOW)
                            .putExtra(GroupProfileActivity.EXTRA_CLIENT_CONTACT_ID, mContact.getClientContactId());
                } else {
                    intent = new Intent(mContext, ClientProfileActivity.class)
                            .setAction(ClientProfileActivity.ACTION_SHOW)
                            .putExtra(ClientProfileActivity.EXTRA_CLIENT_CONTACT_ID, mContact.getClientContactId());
                }
                mContext.startActivity(intent);
            }
        });

        return convertView;
    }

    private void updateLastMessageText(TalkClientMessage message) {
        String mediaType;
        String text;
        if (message.getDelivery().hasAttachment()) {
            if (message.isIncoming()) {
                text = mContext.getResources().getString(R.string.contact_item_received_attachment);
                mediaType = mContext.getResources().getString(getMediaTypeStringId(message.getAttachmentDownload().getMediaType()));
            } else {
                text = mContext.getResources().getString(R.string.contact_item_sent_attachment);
                mediaType = mContext.getResources().getString(getMediaTypeStringId(message.getAttachmentUpload().getMediaType()));
            }
            mLastMessageText = String.format(text, mediaType);
        } else {
            mLastMessageText = message.getText();
        }
    }

    private int getMediaTypeStringId(String mediaType) {
        int resId;
        if (ContentMediaType.IMAGE.equals(mediaType)) {
            resId = R.string.content_image;
        } else if (ContentMediaType.VIDEO.equals(mediaType)) {
            resId = R.string.content_video;
        } else if (ContentMediaType.AUDIO.equals(mediaType)) {
            resId = R.string.content_audio;
        } else if (ContentMediaType.LOCATION.equals(mediaType)) {
            resId = R.string.content_location;
        } else if (ContentMediaType.VCARD.equals(mediaType)) {
            resId = R.string.content_contact;
        } else {
            resId = R.string.content_file;
        }
        return resId;
     }

    public TalkClientContact getContact() {
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
