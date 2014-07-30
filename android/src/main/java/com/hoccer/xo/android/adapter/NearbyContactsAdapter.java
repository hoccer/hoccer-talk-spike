package com.hoccer.xo.android.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.IXoMessageListener;
import com.hoccer.talk.client.IXoTransferListenerOld;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.view.AvatarView;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

public class NearbyContactsAdapter extends BaseAdapter implements IXoContactListener, IXoMessageListener, IXoTransferListenerOld {
    private XoClientDatabase mDatabase;
    private XoActivity mXoActivity;
    private Logger LOG = Logger.getLogger(NearbyContactsAdapter.class);

    private List<TalkClientContact> mNearbyContacts = new ArrayList<TalkClientContact>();
    private TalkClientContact mCurrentNearbyGroup;

    public NearbyContactsAdapter(XoClientDatabase db, XoActivity xoActivity) {
        super();
        mDatabase = db;
        mXoActivity = xoActivity;
    }

    @Override
    public int getCount() {
        return mNearbyContacts.size();
    }

    @Override
    public Object getItem(int position) {
        return mNearbyContacts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact_client, null);
        }
        updateContact(convertView, (TalkClientContact) getItem(position));
        return convertView;
    }

    public void registerListeners() {
        mXoActivity.getXoClient().registerContactListener(this);
        mXoActivity.getXoClient().registerTransferListener(this);
        mXoActivity.getXoClient().registerMessageListener(this);
    }

    public void unregisterListeners() {
        mXoActivity.getXoClient().unregisterContactListener(this);
        mXoActivity.getXoClient().unregisterTransferListener(this);
        mXoActivity.getXoClient().unregisterMessageListener(this);
    }

    public void retrieveDataFromDb(final TalkClientContact group) {
        if (group == null || mDatabase == null) {
            return;
        }
//        if (mDatabase == null) {
//            Timer timer = new Timer();
//            TimerTask task = new TimerTask() {
//                @Override
//                public void run() {
//                    updateAdapter(group);
//                }
//            };
//            timer.schedule(task, 1000);
//            return;
//        }
        try {
            mNearbyContacts.clear();
            List<TalkClientContact> allClientContacts = mDatabase.findAllNearbyContactsInGroup(group);
            for (TalkClientContact contact : allClientContacts) {

                if (contact.isGroup()) {
                    if (contact.getGroupPresence().isTypeNearby()) {
                        contact.setNickname(mXoActivity.getResources().getString(R.string.nearby_text));
                        mNearbyContacts.add(contact);
                    }
                } else {
                    if (contact.isClientGroupJoined(group) && !contact.isSelf()) {
                        mNearbyContacts.add(contact);
                    }
                    TalkClientDownload avatarDownload = contact.getAvatarDownload();
                    if (avatarDownload != null) {
                        mDatabase.refreshClientDownload(avatarDownload);
                    }
                }
            }
        } catch (SQLException e) {
            LOG.error("SQL Error while retrieving nearby group contacts.");
        }
    }


    private void updateContact(final View view, final TalkClientContact contact) {
        TextView nameView = ViewHolderForAdapters.get(view, R.id.contact_name);
        AvatarView avatarView = ViewHolderForAdapters.get(view, R.id.contact_icon);
        TextView typeView = ViewHolderForAdapters.get(view, R.id.contact_type);
        TextView lastMessageTimeView = (TextView) view.findViewById(R.id.contact_time);
        TextView lastMessageText = (TextView) view.findViewById(R.id.contact_last_message);
        TextView unseenView = (TextView) view.findViewById(R.id.contact_unseen_messages);

        if (contact.isGroup()) {
            nameView.setText(contact.getNickname() + " (" + (mNearbyContacts.size() - 1) + ")");
        } else {
            nameView.setText(contact.getNickname());
        }
        avatarView.setContact(contact);


        typeView.setText("");
        lastMessageText.setText("");
        lastMessageTimeView.setText("");
        unseenView.setText("");

        if (contact.isGroup()) {
            if (contact.isGroupInvited()) {
                typeView.setText(R.string.common_group_invite);
            } else {
                typeView.setText(R.string.common_group);
            }
        }
        TalkClientMessage message = null;
        long unseenMessages = 0;
        try {
            message = mDatabase.findLatestMessageByContactId(contact.getClientContactId());
            unseenMessages = mDatabase.findUnseenMessageCountByContactId(contact.getClientContactId());
        } catch (SQLException e) {
            LOG.error("NearbyContactsAdapter: SQL error", e);
        }
        if (message != null) {
            Date messageTime = message.getTimestamp();
            SimpleDateFormat sdf = new SimpleDateFormat("EEE HH:mm");
            String lastMessageTime = sdf.format(messageTime);

            lastMessageTimeView.setText(lastMessageTime);
            if (message.getAttachmentDownload() != null) {
                TalkClientDownload attachment = message.getAttachmentDownload();
                lastMessageText.setText(chooseAttachmentType(view.getContext(),
                        attachment.getMediaType()));
            } else {
                lastMessageText.setText(message.getText());
            }
        }
        if (unseenMessages > 0) {
            unseenView.setText(Long.toString(unseenMessages));
            unseenView.setVisibility(View.VISIBLE);
        } else {
            unseenView.setVisibility(View.GONE);
        }
        avatarView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mXoActivity.showContactProfile(contact);
            }
        });
    }

    private String chooseAttachmentType(Context context, String attachmentType) {
        String text = context.getResources().getString(R.string.contact_item_received_attachment);
        return String.format(text, attachmentType);
    }

    private void updateAdapter(final TalkClientContact group) {
        mXoActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    retrieveDataFromDb(group);
                }
                notifyDataSetChanged();
            }
        });
    }

    private void refreshList() {
        mXoActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    public interface Filter {
        public boolean shouldShow(TalkClientContact contact);
    }

    private List<TalkClientContact> filter(List<TalkClientContact> in, Filter filter) {
        ArrayList<TalkClientContact> res = new ArrayList<TalkClientContact>();
        for (TalkClientContact contact : in) {
            if (filter.shouldShow(contact)) {
                res.add(contact);
            }
        }
        return res;
    }

    private
    @Nullable
    TalkClientContact getActiveNearbyGroup() {
        if (mXoActivity != null) {
            return mXoActivity.getXoClient().getCurrentNearbyGroup();
        }
        return null;
    }

    @Override
    public void onContactAdded(TalkClientContact contact) {

    }

    @Override
    public void onContactRemoved(TalkClientContact contact) {
    }

    @Override
    public void onClientPresenceChanged(TalkClientContact contact) {
        refreshList();
    }

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {
    }

    @Override
    public void onGroupPresenceChanged(TalkClientContact contact) {
        if (mCurrentNearbyGroup != null && contact.getGroupId().equals(mCurrentNearbyGroup.getGroupId())) {
            updateAdapter(contact);
        }
    }

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) {
        TalkClientContact currentNearbyGroup = getActiveNearbyGroup();
        if (currentNearbyGroup != null) {
            mCurrentNearbyGroup = currentNearbyGroup;
        }
        if (mCurrentNearbyGroup != null && contact.getGroupId().equals(mCurrentNearbyGroup.getGroupId())) {
            updateAdapter(contact);
        }
    }

    @Override
    public void onMessageAdded(TalkClientMessage message) {
        refreshList();
    }

    @Override
    public void onMessageRemoved(TalkClientMessage message) {
        refreshList();
    }

    @Override
    public void onMessageStateChanged(TalkClientMessage message) {
        refreshList();
    }

    @Override
    public void onDownloadRegistered(TalkClientDownload download) {

    }

    @Override
    public void onDownloadStarted(TalkClientDownload download) {

    }

    @Override
    public void onDownloadProgress(TalkClientDownload download) {

    }

    @Override
    public void onDownloadFinished(TalkClientDownload download) {
        if (download.isAvatar()) {
            refreshList();
        }
    }

    @Override
    public void onDownloadFailed(TalkClientDownload download) {

    }

    @Override
    public void onDownloadStateChanged(TalkClientDownload download) {

    }

    @Override
    public void onUploadStarted(TalkClientUpload upload) {
        if (upload.isAvatar()) {
            refreshList();
        }
    }

    @Override
    public void onUploadProgress(TalkClientUpload upload) {

    }

    @Override
    public void onUploadFinished(TalkClientUpload upload) {

    }

    @Override
    public void onUploadFailed(TalkClientUpload upload) {

    }

    @Override
    public void onUploadStateChanged(TalkClientUpload upload) {

    }
}
