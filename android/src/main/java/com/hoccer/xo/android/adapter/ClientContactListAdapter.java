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
import com.hoccer.xo.release.R;
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
            XoClientDatabase database = XoApplication.getXoClient().getDatabase();
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

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.item_contact_client, null);
        }

        AvatarView avatarView = (AvatarView) convertView.findViewById(R.id.contact_icon);
        TextView contactNameTextView = (TextView) convertView.findViewById(R.id.contact_name);
        LinearLayout invitedMeLayout = (LinearLayout) convertView.findViewById(R.id.ll_invited_me);
        Button acceptButton = (Button) convertView.findViewById(R.id.btn_accept);
        Button declineButton = (Button) convertView.findViewById(R.id.btn_decline);
        TextView isInvitedTextView = (TextView) convertView.findViewById(R.id.tv_is_invited);
        TextView isFriendTextView = (TextView) convertView.findViewById(R.id.tv_is_friend);

        final TalkClientContact contact = (TalkClientContact) getItem(position);

        avatarView.setContact(contact);

        contactNameTextView.setText(contact.getNickname());

        acceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                XoApplication.getXoClient().acceptFriend(contact);
            }
        });

        declineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                XoDialogs.showYesNoDialog("ConfirmDeclineClientInvitationDialog",
                    mActivity.getString(R.string.friend_request_decline_invitation_title),
                    mActivity.getString(R.string.friend_request_decline_invitation_message, contact.getNickname()),
                    mActivity,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            XoApplication.getXoClient().declineFriend(contact);
                        }
                    });
            }
        });

        TalkRelationship relationship = contact.getClientRelationship();
        if (relationship != null) {
            if (relationship.invitedMe()) {
                invitedMeLayout.setVisibility(View.VISIBLE);
                isInvitedTextView.setVisibility(View.GONE);
                isFriendTextView.setVisibility(View.GONE);
            } else if (relationship.isInvited()) {
                invitedMeLayout.setVisibility(View.GONE);
                isInvitedTextView.setVisibility(View.VISIBLE);
                isFriendTextView.setVisibility(View.GONE);
            } else if (relationship.isFriend() || relationship.isBlocked()) {
                invitedMeLayout.setVisibility(View.GONE);
                isInvitedTextView.setVisibility(View.GONE);
                isFriendTextView.setVisibility(View.VISIBLE);

                long messageCount = 0;
                long attachmentCount = 0;
                try {
                    messageCount = XoApplication.getXoClient().getDatabase().getMessageCountByContactId(contact.getClientContactId());
                    attachmentCount = XoApplication.getXoClient().getDatabase().getAttachmentCountByContactId(contact.getClientContactId());
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                String messageAndAttachmentCountInfo = convertView.getResources().getString(R.string.message_and_attachment_count_info, messageCount, attachmentCount);
                isFriendTextView.setText(messageAndAttachmentCountInfo);

                if (relationship.isBlocked()) {
                    isFriendTextView.setText(convertView.getResources().getString(R.string.blocked_contact));
                }
            }
        }

        return convertView;
    }
}
