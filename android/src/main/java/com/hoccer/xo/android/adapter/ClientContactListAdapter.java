package com.hoccer.xo.android.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.model.TalkRelationship;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.view.avatar.AvatarView;
import org.apache.commons.collections4.ListUtils;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class ClientContactListAdapter extends ContactListAdapter {

    private static final Logger LOG = Logger.getLogger(ClientContactListAdapter.class);
    private static final int TYPE_PRESENCE = 0;
    private static final int TYPE_NEARBY_HISTORY = 1;
    private static final int TYPE_WORLDWIDE_HISTORY = 2;
    private static final int TYPE_HISTORY = 3;
    private static final int TYPE_SIMPLE = 4;

    public ClientContactListAdapter(Activity activity) {
        super(activity);
    }

    @Override
    protected List<TalkClientContact> getAllContacts() {
        List<TalkClientContact> invitedMe;
        List<TalkClientContact> invited;
        List<TalkClientContact> friends;
        List<TalkClientContact> blockedFriends;

        try {
            XoClientDatabase database = XoApplication.get().getClient().getDatabase();
            invitedMe = database.findClientContactsByState(TalkRelationship.STATE_INVITED_ME);
            invited = database.findClientContactsByState(TalkRelationship.STATE_INVITED);
            friends = database.findClientContactsByState(TalkRelationship.STATE_FRIEND);
            blockedFriends = database.findClientContactsByState(TalkRelationship.STATE_BLOCKED, TalkRelationship.STATE_FRIEND);
        } catch (SQLException e) {
            LOG.error("Could not fetch client contacts", e);
            return Collections.emptyList();
        }

        Collections.sort(invited, CLIENT_CONTACT_COMPARATOR);
        Collections.sort(friends, CLIENT_CONTACT_COMPARATOR);
        Collections.sort(blockedFriends, CLIENT_CONTACT_COMPARATOR);

        return ListUtils.union(invitedMe, ListUtils.union(invited, ListUtils.union(friends, blockedFriends)));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder;

        TalkClientContact contact = (TalkClientContact) getItem(position);

        int viewType = getViewTypeForContact(contact);
        if (convertView == null || getViewType(convertView) != viewType) {
            convertView = inflate(viewType, parent);
            viewHolder = createAndInitViewHolder(convertView);
            viewHolder.type = viewType;
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        updateView(viewHolder, contact);

        return convertView;
    }

    private int getViewType(View convertView) {
        return ((ViewHolder) convertView.getTag()).type;
    }

    private int getViewTypeForContact(TalkClientContact contact) {
        int type;

        if (contact.getClientRelationship().isFriend() || contact.getClientRelationship().isBlocked() || contact.isNearby() || contact.isWorldwide()) {
            type = TYPE_PRESENCE;
        } else if (contact.isKept() && contact.isNearbyAcquaintance()) {
            type = TYPE_NEARBY_HISTORY;
        } else if (contact.isKept() && contact.isWorldwideAcquaintance()) {
            type = TYPE_WORLDWIDE_HISTORY;
        } else if (contact.isKept()) {
            type = TYPE_HISTORY;
        } else {
            type = TYPE_SIMPLE;
        }
        return type;
    }

    private View inflate(int type, ViewGroup parent) {
        View convertView;
        LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (type == TYPE_PRESENCE) {
            convertView = inflater.inflate(R.layout.item_contact_client_presence, null);
        } else if (type == TYPE_NEARBY_HISTORY) {
            convertView = inflater.inflate(R.layout.item_contact_client_history_nearby, null);
        } else if (type == TYPE_WORLDWIDE_HISTORY) {
            convertView = inflater.inflate(R.layout.item_contact_client_history_worldwide, null);
        } else if (type == TYPE_HISTORY) {
            convertView = inflater.inflate(R.layout.item_contact_client_history, null);
        } else {
            convertView = inflater.inflate(R.layout.item_contact_client_simple, null);
        }
        return convertView;
    }

    private void updateView(ViewHolder viewHolder, final TalkClientContact contact) {
        viewHolder.avatarView.setContact(contact);
        viewHolder.contactNameTextView.setText(contact.getNickname());

        viewHolder.acceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                XoApplication.get().getClient().acceptFriend(contact);
            }
        });
        viewHolder.declineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showConfirmDialog(contact);
            }
        });

        TalkRelationship relationship = contact.getClientRelationship();
        if (relationship != null) {
            if (relationship.invitedMe()) {
                updateViewForInvitedMe(viewHolder);
            } else if (relationship.isInvited()) {
                updateViewForInvited(viewHolder);
            } else if (relationship.isFriend()) {
                updateViewForFriend(viewHolder, contact);
            } else if (relationship.isBlocked()) {
                updateViewForFriend(viewHolder, contact);
                updateViewForBlocked(viewHolder);
            }
        }
    }

    private void updateViewForInvitedMe(ViewHolder viewHolder) {
        viewHolder.invitedMeLayout.setVisibility(View.VISIBLE);
        viewHolder.isInvitedTextView.setVisibility(View.GONE);
        viewHolder.isFriendTextView.setVisibility(View.GONE);
    }

    private void updateViewForInvited(ViewHolder viewHolder) {
        viewHolder.invitedMeLayout.setVisibility(View.GONE);
        viewHolder.isInvitedTextView.setVisibility(View.VISIBLE);
        viewHolder.isFriendTextView.setVisibility(View.GONE);
    }

    private void updateViewForFriend(ViewHolder viewHolder, TalkClientContact contact) {
        viewHolder.invitedMeLayout.setVisibility(View.GONE);
        viewHolder.isInvitedTextView.setVisibility(View.GONE);
        viewHolder.isFriendTextView.setVisibility(View.VISIBLE);

        String messageAndAttachmentCount = getMessageAndAttachmentCount(contact);
        viewHolder.isFriendTextView.setText(messageAndAttachmentCount);
    }

    private String getMessageAndAttachmentCount(TalkClientContact contact) {
        try {
            XoClientDatabase database = XoApplication.get().getClient().getDatabase();
            int messageCount = (int) database.getMessageCountByContactId(contact.getClientContactId());
            int attachmentCount = (int) database.getAttachmentCountByContactId(contact.getClientContactId());

            String messageCountString = mActivity.getResources().getQuantityString(R.plurals.message_count, messageCount, messageCount);
            String attachmentCountString = mActivity.getResources().getQuantityString(R.plurals.attachment_count, attachmentCount, attachmentCount);

            return messageCountString + " | " + attachmentCountString;
        } catch (SQLException e) {
            LOG.error("Error counting messages and attachments for " + contact.getClientId(), e);
        }

        return "";
    }

    private void updateViewForBlocked(ViewHolder viewHolder) {
        viewHolder.isFriendTextView.setText(mActivity.getResources().getString(R.string.blocked_contact));
    }

    private void showConfirmDialog(final TalkClientContact contact) {
        XoDialogs.showYesNoDialog("ConfirmDeclineClientInvitationDialog",
                mActivity.getString(R.string.friend_request_decline_invitation_title),
                mActivity.getString(R.string.friend_request_decline_invitation_message, contact.getNickname()),
                mActivity,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        XoApplication.get().getClient().declineFriend(contact);
                    }
                });
    }

    private ViewHolder createAndInitViewHolder(View convertView) {
        ViewHolder viewHolder;
        viewHolder = new ViewHolder();
        viewHolder.avatarView = (AvatarView) convertView.findViewById(R.id.avatar);
        viewHolder.contactNameTextView = (TextView) convertView.findViewById(R.id.contact_name);
        viewHolder.invitedMeLayout = (LinearLayout) convertView.findViewById(R.id.ll_invited_me);
        viewHolder.acceptButton = (Button) convertView.findViewById(R.id.btn_accept);
        viewHolder.declineButton = (Button) convertView.findViewById(R.id.btn_decline);
        viewHolder.isInvitedTextView = (TextView) convertView.findViewById(R.id.tv_is_invited);
        viewHolder.isFriendTextView = (TextView) convertView.findViewById(R.id.tv_is_friend);
        return viewHolder;
    }

    private class ViewHolder {
        public AvatarView avatarView;
        public TextView contactNameTextView;
        public LinearLayout invitedMeLayout;
        public Button acceptButton;
        public Button declineButton;
        public TextView isInvitedTextView;
        public TextView isFriendTextView;
        public int type;
    }
}
