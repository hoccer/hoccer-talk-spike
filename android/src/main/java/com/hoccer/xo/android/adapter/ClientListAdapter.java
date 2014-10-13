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
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.model.TalkRelationship;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.view.AvatarView;
import com.hoccer.xo.release.R;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ClientListAdapter extends ContactListAdapter {

    public ClientListAdapter(Activity activity) {
        super(activity);
    }

    @Override
    protected List<TalkClientContact> getAllContacts() {
        List<TalkClientContact> all = new ArrayList<TalkClientContact>();

        List<TalkClientContact> invitedMe = null;
        List<TalkClientContact> invited = null;
        List<TalkClientContact> friends = null;

        try {
            invitedMe = XoApplication.getXoClient().getDatabase().findClientContactsByState(TalkRelationship.STATE_INVITED_ME);
            invited = XoApplication.getXoClient().getDatabase().findClientContactsByState(TalkRelationship.STATE_INVITED);
            friends = XoApplication.getXoClient().getDatabase().findClientContactsByState(TalkRelationship.STATE_FRIEND);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        Comparator comparator = new Comparator<TalkClientContact>() {
            @Override
            public int compare(TalkClientContact contact1, TalkClientContact contact2) {
                return contact1.getNickname().compareTo(contact2.getNickname());
            }
        };

        Collections.sort(invited, comparator);
        Collections.sort(friends, comparator);

        all.addAll(invitedMe);
        all.addAll(invited);
        all.addAll(friends);

        return all;
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
            } else if (relationship.isFriend()) {
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
            }
        }

        return convertView;
    }
}