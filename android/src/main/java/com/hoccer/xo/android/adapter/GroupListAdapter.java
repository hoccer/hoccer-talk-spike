package com.hoccer.xo.android.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.model.TalkGroupMember;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.view.AvatarView;
import com.hoccer.xo.release.R;

import java.sql.SQLException;
import java.util.*;

public class GroupListAdapter extends ContactListAdapter {

    private static final int DISPLAY_NAMES_MAX_LENGTH = 30;

    private Activity mActivity;
    private List<TalkClientContact> mGroups = new ArrayList<TalkClientContact>();
    private String mQuery;

    public GroupListAdapter(Activity activity) {
        super(activity);
    }

    @Override
    protected List<TalkClientContact> getAllContacts() {
        List<TalkClientContact> invitedMe = null;
        List<TalkClientContact> joined = null;
        try {
            invitedMe = XoApplication.getXoClient().getDatabase().findGroupContactsByState(TalkGroupMember.STATE_INVITED);
            joined = XoApplication.getXoClient().getDatabase().findGroupContactsByState(TalkGroupMember.STATE_JOINED);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        Collections.sort(joined, new Comparator<TalkClientContact>() {
            @Override
            public int compare(TalkClientContact o1, TalkClientContact o2) {
                return o1.getNickname().compareTo(o2.getNickname());
            }
        });

        invitedMe.addAll(joined);

        return invitedMe;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.item_contact_group, null);
        }

        AvatarView avatarView = (AvatarView) convertView.findViewById(R.id.contact_icon);
        TextView contactNameTextView = (TextView) convertView.findViewById(R.id.contact_name);
        LinearLayout invitedMeLayout = (LinearLayout) convertView.findViewById(R.id.ll_invited_me);
        Button acceptButton = (Button) convertView.findViewById(R.id.btn_accept);
        Button declineButton = (Button) convertView.findViewById(R.id.btn_decline);
        TextView groupMembersTextView = (TextView) convertView.findViewById(R.id.tv_group_members);

        final TalkClientContact group = (TalkClientContact) getItem(position);

        contactNameTextView.setText(group.getNickname());
        avatarView.setContact(group);

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
                groupMembersTextView.setVisibility(View.GONE);
            } else if (member.isJoined()) {
                invitedMeLayout.setVisibility(View.GONE);
                groupMembersTextView.setVisibility(View.VISIBLE);
                groupMembersTextView.setText(getGroupMembersString(group));
            }
        }

        return convertView;
    }

    private String getGroupMembersString(TalkClientContact group) {
        ArrayDeque<String> displayMembers = new ArrayDeque<String>();
        List<TalkClientContact> joinedContacts = group.getJoinedGroupContacts();

        for (TalkClientContact contact : joinedContacts) {
            displayMembers.addLast(contact.getNickname());

            if (TextUtils.join(", ", displayMembers).length() > DISPLAY_NAMES_MAX_LENGTH) {
                displayMembers.removeLast();
                break;
            }
        }

        String groupMembersString = TextUtils.join(", ", displayMembers);
        int moreCount = joinedContacts.size() - displayMembers.size();

        if (moreCount > 0) {
            Resources resources = mActivity.getResources();
            String moreString = resources.getQuantityString(R.plurals.groups_and_x_more, moreCount, moreCount);
            groupMembersString = groupMembersString + " " + moreString;
        }

        return groupMembersString;
    }

    @Override
    public void onGroupPresenceChanged(TalkClientContact contact) {
        refreshView();
    }

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) {
        refreshView();
    }
}
