package com.hoccer.xo.android.fragment;

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
import com.hoccer.talk.model.TalkGroupMember;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.release.R;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class GroupsAdapter extends BaseAdapter implements IXoContactListener {

    private List<TalkClientContact> mGroups = new ArrayList<TalkClientContact>();

    private Activity mActivity;

    public GroupsAdapter(Activity activity) {
        try {
            mActivity = activity;
            mGroups = XoApplication.getXoClient().getDatabase().findAllGroupContacts();
            sortGroupContacts();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getCount() {
        return mGroups.size();
    }

    @Override
    public Object getItem(int position) {
        return mGroups.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mGroups.get(position).getClientContactId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.item_contact_group, null);
        }

        TextView contactNameTextView = (TextView) convertView.findViewById(R.id.contact_name);
        LinearLayout invitedMeLayout = (LinearLayout) convertView.findViewById(R.id.ll_invited_me);
        Button acceptButton = (Button) convertView.findViewById(R.id.btn_accept);
        Button declineButton = (Button) convertView.findViewById(R.id.btn_decline);
        TextView isJoinedTextView = (TextView) convertView.findViewById(R.id.is_joined);

        final TalkClientContact group = (TalkClientContact) getItem(position);

        contactNameTextView.setText(group.getNickname());

        acceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                XoApplication.getXoClient().joinGroup(group.getGroupId());
            }
        });

        declineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                XoDialogs.showYesNoDialog("ConfirmDeclineGroupInvitationDialog",
                        mActivity.getString(R.string.group_request_decline_invitation_title),
                        mActivity.getString(R.string.group_request_decline_invitation_message, group.getNickname()),
                        mActivity,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                XoApplication.getXoClient().leaveGroup(group.getGroupId());
                            }
                        });
            }
        });

        if (group.isGroup() && group.getGroupMember() != null) {
            TalkGroupMember member = group.getGroupMember();
            if (member.isInvited()) {
                invitedMeLayout.setVisibility(View.VISIBLE);
                isJoinedTextView.setVisibility(View.GONE);
            } else if (member.isJoined()) {
                invitedMeLayout.setVisibility(View.GONE);
                isJoinedTextView.setVisibility(View.VISIBLE);
            }
        }

        return convertView;
    }

    private void sortGroupContacts() {
        List<TalkClientContact> invitedMeGroups = new ArrayList<TalkClientContact>();
        List<TalkClientContact> joinedGroups = new ArrayList<TalkClientContact>();

        for (TalkClientContact group : mGroups) {
            TalkGroupMember member = group.getGroupMember();
            if (member != null) {
                if (member.isInvited()){
                    invitedMeGroups.add(group);
                } else if (member.isJoined()) {
                    joinedGroups.add(group);
                }
            }
        }

        invitedMeGroups.addAll(joinedGroups);

        mGroups = invitedMeGroups;
    }

    @Override
    public void onContactAdded(TalkClientContact contact) {

    }

    @Override
    public void onContactRemoved(TalkClientContact contact) {

    }

    @Override
    public void onClientPresenceChanged(TalkClientContact contact) {

    }

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {

    }

    @Override
    public void onGroupPresenceChanged(TalkClientContact contact) {

    }

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) {
        try {
            mGroups = XoApplication.getXoClient().getDatabase().findAllGroupContacts();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        sortGroupContacts();
        refreshView();
    }

    private void refreshView() {
        Handler guiHandler = new Handler(Looper.getMainLooper());
        guiHandler.post(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }
}
