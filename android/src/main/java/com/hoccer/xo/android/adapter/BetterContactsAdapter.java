package com.hoccer.xo.android.adapter;

import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.IXoMessageListener;
import com.hoccer.talk.client.IXoTokenListener;
import com.hoccer.talk.client.IXoTransferListenerOld;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.client.model.TalkClientSmsToken;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.base.XoAdapter;
import com.hoccer.xo.android.view.AvatarView;
import com.hoccer.xo.release.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * TODO:
 * - add SMS Token Support
 * - add filtering
 * - add sort by unseen messages
 */
public class BetterContactsAdapter extends XoAdapter implements IXoContactListener, IXoMessageListener, IXoTokenListener, IXoTransferListenerOld {

    private List<ContactItem> mContactItems;


    public BetterContactsAdapter(XoActivity activity) {
        super(activity);
        initialize();
    }

    private void initialize() {
        mContactItems = new ArrayList<ContactItem>();
    }

    @Override
    public int getCount() {
        return mContactItems.size();
    }

    @Override
    public Object getItem(int i) {
        return mContactItems.get(i).getTalkClientContact();
    }

    @Override
    public long getItemId(int i) {
        return mContactItems.get(i).getTalkClientContact().;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        View view = mContactItems.get(i).getView(convertView);
        return view;
    }


    @Override
    public void onContactAdded(TalkClientContact contact) {

        updateAll();
    }

    @Override
    public void onContactRemoved(TalkClientContact contact) {
        updateAll();
    }

    @Override
    public void onClientPresenceChanged(TalkClientContact contact) {
        ContactItem contactItem = findContactItemForContact(contact);
        if(contactItem == null) {
            return;
        }

        contactItem.update();
    }

    // TODO:
    private void updateAll() {
        for (int i = 0; i < mContactItems.size(); i++) {
            ContactItem contactItem = mContactItems.get(i);
            contactItem.update();
        }

    }

    private ContactItem findContactItemForContact(TalkClientContact contact) {
        for (int i = 0; i < mContactItems.size(); i++) {
            ContactItem contactItem = mContactItems.get(i);
            if(contact.equals(contactItem.getTalkClientContact())) {
                return contactItem;
            }
        }
        return null;
    }

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {
        updateAll();
    }

    @Override
    public void onGroupPresenceChanged(TalkClientContact contact) {
        updateAll();
    }

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) {
        updateAll();
    }

    @Override
    public void onMessageAdded(TalkClientMessage message) {
        updateAll();
    }

    @Override
    public void onMessageRemoved(TalkClientMessage message) {
        updateAll();
    }

    @Override
    public void onMessageStateChanged(TalkClientMessage message) {
        updateAll();
    }

    @Override
    public void onTokensChanged(List<TalkClientSmsToken> tokens, boolean newTokens) {
        updateAll();
    }

    @Override
    public void onDownloadRegistered(TalkClientDownload download) {
        updateAll();
    }

    @Override
    public void onDownloadStarted(TalkClientDownload download) {
        updateAll();
    }

    @Override
    public void onDownloadProgress(TalkClientDownload download) {
    }

    @Override
    public void onDownloadFinished(TalkClientDownload download) {
        updateAll();
    }

    @Override
    public void onDownloadFailed(TalkClientDownload download) {
        updateAll();
    }

    @Override
    public void onDownloadStateChanged(TalkClientDownload download) {
        if(download.isAvatar() && download.getState() == TalkClientDownload.State.COMPLETE) {
            updateAll();
        }
    }

    @Override
    public void onUploadStarted(TalkClientUpload upload) {
        updateAll();
    }

    @Override
    public void onUploadProgress(TalkClientUpload upload) {
    }

    @Override
    public void onUploadFinished(TalkClientUpload upload) {
        updateAll();
    }

    @Override
    public void onUploadFailed(TalkClientUpload upload) {
        updateAll();
    }

    @Override
    public void onUploadStateChanged(TalkClientUpload upload) {
        updateAll();
    }


    private class ContactItem {

        private TalkClientContact mContact;

        private String mLastMessageText;
        private Date mLastMessageTimeStamp;
        private long mUnseenMessageCount;

        public ContactItem(TalkClientContact contact) {
            mContact = contact;
        }

        public TalkClientContact getTalkClientContact() {
            return mContact;
        }

        public View getView(View view) {
            if(view == null) {
                LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.item_contact_client, null);
            }

            return configureView(view);
        }

        private View configureView(View view) {
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
                mUnseenMessageCount = mDatabase.findUnseenMessageCountByContactId(mContact.getClientContactId());
                TalkClientMessage message = mDatabase.findLatestMessageByContactId(mContact.getClientContactId());
                mLastMessageText = message.getText();
                mLastMessageTimeStamp = message.getTimestamp();
            } catch (SQLException e) {
                LOG.error("sql error", e);
            }
        }
    }
}
