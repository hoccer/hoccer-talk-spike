package com.hoccer.xo.android.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.model.TalkRelationship;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.release.R;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ClientContactsAdapter extends BaseAdapter implements IXoContactListener {

    private List<TalkClientContact> mClientContactList = new ArrayList<TalkClientContact>();

    public ClientContactsAdapter() {
        try {
            mClientContactList = XoApplication.getXoClient().getDatabase().findAllClientContacts();
            sortTalkClientContacts();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getCount() {
        return mClientContactList.size();
    }

    @Override
    public Object getItem(int position) {
        return mClientContactList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mClientContactList.get(position).getClientContactId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.item_contact_client, null);
        }

        TextView contactNameTextView = (TextView) convertView.findViewById(R.id.contact_name);
        LinearLayout invitedMeLayout = (LinearLayout) convertView.findViewById(R.id.ll_invited_me);
        Button acceptButton = (Button) convertView.findViewById(R.id.btn_accept);
        Button declineButton = (Button) convertView.findViewById(R.id.btn_decline);
        TextView isInvitedTextView = (TextView) convertView.findViewById(R.id.tv_is_invited);
        TextView isFriendTextView = (TextView) convertView.findViewById(R.id.tv_is_friend);

        final TalkClientContact contact = (TalkClientContact) getItem(position);

        contactNameTextView.setText(contact.getName());

        acceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                XoApplication.getXoClient().acceptFriend(contact);
            }
        });

        declineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                XoApplication.getXoClient().declineFriend(contact);
            }
        });

        if (contact.getClientRelationship().invitedMe()) {
            invitedMeLayout.setVisibility(View.VISIBLE);
            isInvitedTextView.setVisibility(View.GONE);
            isFriendTextView.setVisibility(View.GONE);
        } else if (contact.getClientRelationship().isInvited()) {
            invitedMeLayout.setVisibility(View.GONE);
            isInvitedTextView.setVisibility(View.VISIBLE);
            isFriendTextView.setVisibility(View.GONE);
        } else if (contact.getClientRelationship().isFriend()) {
            invitedMeLayout.setVisibility(View.GONE);
            isInvitedTextView.setVisibility(View.GONE);

            long messageCount = 0;
            long attachmentCount = 0;
            try {
                messageCount = XoApplication.getXoClient().getDatabase().getMessageCountByContactId(contact.getClientContactId());
//                TODO: attachmentCount = XoApplication.getXoClient().getDatabase().getAttachmentCountByContactId(contact.getClientContactId());
            } catch (SQLException e) {
                e.printStackTrace();
            }
            isInvitedTextView.setText(convertView.getResources().getString(R.string.message_and_attachment_count_info, messageCount, attachmentCount));
            isFriendTextView.setVisibility(View.VISIBLE);
        }

        return convertView;
    }

    @Override
    public void onContactAdded(TalkClientContact contact) {
        // add contact to list

        // sort list
        sortTalkClientContacts();

        // update
    }

    @Override
    public void onContactRemoved(TalkClientContact contact) {
        // remove contact from list
        mClientContactList.remove(contact);
        // update

        notifyDataSetChanged();
    }

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {
        mClientContactList.clear();
        try {
            mClientContactList = XoApplication.getXoClient().getDatabase().findAllClientContacts();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        sortTalkClientContacts();
        notifyDataSetChanged();
    }

    @Override
    public void onClientPresenceChanged(TalkClientContact contact) {
        // show online status ?
    }

    @Override
    public void onGroupPresenceChanged(TalkClientContact contact) {
    }

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) {
    }

    private void sortTalkClientContacts() {
        List<TalkClientContact> invitedMeContacts = new ArrayList<TalkClientContact>();
        List<TalkClientContact> invitedContacts = new ArrayList<TalkClientContact>();
        List<TalkClientContact> friendContacts = new ArrayList<TalkClientContact>();

        for (TalkClientContact contact : mClientContactList) {
            TalkRelationship relationship = contact.getClientRelationship();
            if (relationship != null) {
                if (relationship.invitedMe()) {
                    invitedMeContacts.add(contact);
                } else if (relationship.isInvited()) {
                    invitedContacts.add(contact);
                } else if (relationship.isFriend()) {
                    friendContacts.add(contact);
                }
            }
        }

        invitedContacts.addAll(friendContacts);
        invitedMeContacts.addAll(invitedContacts);

        mClientContactList = invitedMeContacts;
    }
}
