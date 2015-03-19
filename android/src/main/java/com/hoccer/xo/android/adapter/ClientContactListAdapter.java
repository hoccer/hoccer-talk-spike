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
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.model.TalkRelationship;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.view.AvatarView;
import com.artcom.hoccer.R;
import org.apache.commons.collections4.ListUtils;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class ClientContactListAdapter extends ContactListAdapter {

    private static final Logger LOG = Logger.getLogger(ClientContactListAdapter.class);

    public ClientContactListAdapter(Activity activity) {
        super(activity);
    }

    @Override
    protected List<TalkClientContact> getAllContacts() {
        List<TalkClientContact> invitedMe;
        List<TalkClientContact> invited;
        List<TalkClientContact> friends;
        List<TalkClientContact> blocked;

        try {
            XoClientDatabase database = XoApplication.get().getXoClient().getDatabase();
            invitedMe = database.findClientContactsByState(TalkRelationship.STATE_INVITED_ME);
            invited = database.findClientContactsByState(TalkRelationship.STATE_INVITED);
            friends = database.findClientContactsByState(TalkRelationship.STATE_FRIEND);
            blocked = database.findClientContactsByState(TalkRelationship.STATE_BLOCKED);
        } catch (SQLException e) {
            LOG.error("Could not fetch client contacts", e);
            return Collections.emptyList();
        }

        Collections.sort(invited, CLIENT_CONTACT_COMPARATOR);
        Collections.sort(friends, CLIENT_CONTACT_COMPARATOR);
        Collections.sort(blocked, CLIENT_CONTACT_COMPARATOR);

        return ListUtils.union(invitedMe, ListUtils.union(invited, ListUtils.union(friends, blocked)));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder;

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.item_contact_client, null);
            viewHolder = createAndInitViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final TalkClientContact contact = (TalkClientContact) getItem(position);

        updateView(viewHolder, contact);

        return convertView;
    }

    private void updateView(ViewHolder viewHolder, final TalkClientContact contact) {
        viewHolder.avatarView.setContact(contact);
        viewHolder.contactNameTextView.setText(contact.getNickname());

        viewHolder.acceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                XoApplication.get().getXoClient().acceptFriend(contact);
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
            XoClientDatabase database = XoApplication.get().getXoClient().getDatabase();
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
                        XoApplication.get().getXoClient().declineFriend(contact);
                    }
                });
    }

    private ViewHolder createAndInitViewHolder(View convertView) {
        ViewHolder viewHolder;
        viewHolder = new ViewHolder();
        viewHolder.avatarView = (AvatarView) convertView.findViewById(R.id.contact_icon);
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
    }
}
