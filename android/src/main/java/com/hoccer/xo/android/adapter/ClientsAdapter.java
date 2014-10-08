package com.hoccer.xo.android.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
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
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.view.AvatarView;
import com.hoccer.xo.release.R;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ClientsAdapter extends BaseAdapter implements IXoContactListener {

    private Activity mActivity;

    private List<TalkClientContact> mClients = new ArrayList<TalkClientContact>();

    public ClientsAdapter(Activity activity) {
        mActivity = activity;
        mClients = getAllClientContacts();
    }

    private List<TalkClientContact> getAllClientContacts() {

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

        invited.addAll(friends);
        invitedMe.addAll(invited);

        return invitedMe;
    }

    @Override
    public int getCount() {
        return mClients.size();
    }

    @Override
    public Object getItem(int position) {
        return mClients.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mClients.get(position).getClientContactId();
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

        return convertView;
    }

    @Override
    public boolean isEnabled(int position) {
        TalkClientContact contact = (TalkClientContact) getItem(position);
        return contact.getClientRelationship().isFriend();
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {
        refreshView();
    }

    @Override
    public void onContactRemoved(TalkClientContact contact) {
        refreshView();
    }

    @Override
    public void onContactAdded(TalkClientContact contact) {
    }

    @Override
    public void onClientPresenceChanged(TalkClientContact contact) {
        refreshView();
    }

    @Override
    public void onGroupPresenceChanged(TalkClientContact contact) {
    }

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) {
    }

    private void refreshView() {
        final List<TalkClientContact> newClients = getAllClientContacts();

        Handler guiHandler = new Handler(Looper.getMainLooper());
        guiHandler.post(new Runnable() {
            @Override
            public void run() {
                mClients = newClients;
                notifyDataSetChanged();
            }
        });
    }
}
