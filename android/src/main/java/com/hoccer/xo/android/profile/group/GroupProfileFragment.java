package com.hoccer.xo.android.profile.group;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.XoTransfer;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.content.SelectedContent;
import com.hoccer.talk.model.TalkGroupMembership;
import com.hoccer.talk.model.TalkGroupPresence;
import com.hoccer.talk.model.TalkRelationship;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.activity.ChatActivity;
import com.hoccer.xo.android.activity.GroupContactSelectionResultActivity;
import com.hoccer.xo.android.activity.MediaBrowserActivity;
import com.hoccer.xo.android.adapter.ContactsAdapter;
import com.hoccer.xo.android.adapter.GroupMemberContactsAdapter;
import com.hoccer.xo.android.base.BaseActivity;
import com.hoccer.xo.android.fragment.ContactSelectionFragment;
import com.hoccer.xo.android.profile.ProfileFragment;
import com.hoccer.xo.android.util.DisplayUtils;
import com.hoccer.xo.android.util.IntentHelper;
import com.hoccer.xo.android.util.UriUtils;
import com.squareup.picasso.Picasso;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static com.hoccer.talk.client.model.TalkClientContact.transformToContactIds;


/**
 * Fragment for display and editing of group profiles.
 */
public class GroupProfileFragment extends ProfileFragment
        implements IXoContactListener, ActionMode.Callback, AdapterView.OnItemClickListener {

    private static final Logger LOG = Logger.getLogger(GroupProfileFragment.class);

    public static final int SELECT_CONTACT_REQUEST = 1;

    private boolean mBackPressed;
    public static final String ARG_START_IN_ACTION_MODE = "ARG_START_IN_ACTION_MODE";
    private MenuItem mFriendsRequestMenuItem;

    public enum Mode {
        PROFILE,
        EDIT_GROUP
    }

    private Mode mMode;

    private EditText mNameEditText;
    private LinearLayout mGroupMembersContainer;
    private ListView mGroupMembersList;

    private RelativeLayout mChatContainer;
    private RelativeLayout mChatMessagesContainer;
    private TextView mChatMessagesText;

    @Nullable
    private ContactsAdapter mGroupMemberAdapter;

    private final ArrayList<TalkClientContact> mContactsToDisinviteAsFriend = new ArrayList<TalkClientContact>();
    private ArrayList<TalkClientContact> mContactsToInviteAsFriend = new ArrayList<TalkClientContact>();

    private final ContactsAdapter.Filter mInvitedOrJoinedClientFilter = new ContactsAdapter.Filter() {
        @Override
        public boolean shouldShow(TalkClientContact contact) {
            try {
                if (contact.isClient()) {
                    TalkGroupMembership membership = XoApplication.get().getXoClient().getDatabase().findMembershipInGroupByClientId(mContact.getGroupId(), contact.getClientId());
                    if (membership != null) {
                        return membership.isInvited() || membership.isJoined();
                    }
                }
            } catch (SQLException e) {
                LOG.error("GroupProfileFragment.Filter.shouldShow()", e);
            }

            return false;
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_group_profile, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.setFocusableInTouchMode(true);
        view.setOnKeyListener(new BackPressListener());

        mChatContainer = (RelativeLayout) view.findViewById(R.id.inc_profile_chat_stats);
        mChatMessagesContainer = (RelativeLayout) view.findViewById(R.id.rl_profile_messages);
        mChatMessagesText = (TextView) view.findViewById(R.id.tv_messages_text);
        mChatMessagesContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMessagingActivity();
            }
        });

        mGroupMembersContainer = (LinearLayout) view.findViewById(R.id.profile_group_members_container);
        mGroupMembersList = (ListView) mGroupMembersContainer.findViewById(R.id.profile_group_members_list);

        mNameEditText = (EditText) view.findViewById(R.id.profile_group_name_edit);
        mNameEditText.setFocusableInTouchMode(true);
        mNameEditText.setOnKeyListener(new BackPressListener());
    }

    private void inviteOrDisinviteMembersAsFriends() {
        if (mContactsToDisinviteAsFriend.isEmpty()) {
            inviteAllMembersAsFriends();
        } else {
            disinviteAllMembersAsFriends();
        }
    }

    private void showMessagingActivity() {
        Intent intent = new Intent(getActivity(), ChatActivity.class);
        intent.putExtra(IntentHelper.EXTRA_CONTACT_ID, mContact.getClientContactId());
        if (mContact.isKept() || mContact.isKeptGroup()) {
            intent.putExtra(ChatActivity.EXTRA_CLIENT_HISTORY, true);
        }
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();

        XoApplication.get().getXoClient().registerContactListener(this);

        mMode = Mode.PROFILE;

        if (mGroupMemberAdapter == null) {
            mGroupMemberAdapter = new GroupMemberContactsAdapter((BaseActivity) getActivity(), mContact.getGroupId());
            mGroupMemberAdapter.setFilter(mInvitedOrJoinedClientFilter);
            mGroupMemberAdapter.registerListeners();
            mGroupMemberAdapter.registerDataSetObserver(new DataSetObserver() {
                @Override
                public void onInvalidated() {
                    super.onInvalidated();
                    DisplayUtils.setListViewHeightBasedOnChildren(mGroupMembersList);
                }
            });
            mGroupMembersList.setAdapter(mGroupMemberAdapter);
        }
        mGroupMemberAdapter.loadContacts();

        if (mContact.isNearbyGroup() || mContact.isWorldwideGroup()) {
            mGroupMembersList.setOnItemClickListener(this);
        } else {
            mGroupMembersList.setOnItemClickListener(null);
        }

        updateContact();

        Bundle arguments = getArguments();
        if (arguments != null) {
            if (arguments.containsKey(ARG_START_IN_ACTION_MODE)) {
                if (arguments.getBoolean(ARG_START_IN_ACTION_MODE)) {
                    getActivity().startActionMode(this);
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        XoApplication.get().getXoClient().unregisterContactListener(this);
    }

    class BackPressListener implements View.OnKeyListener {

        @Override
        public boolean onKey(View view, int i, KeyEvent keyEvent) {
            if (i == KeyEvent.KEYCODE_BACK && mMode == Mode.EDIT_GROUP) {
                mBackPressed = true;
            }
            return false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mGroupMemberAdapter != null) {
            mGroupMemberAdapter.unRegisterListeners();
            mGroupMemberAdapter = null;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.fragment_group_profile, menu);
        configureOptionsMenuItems(menu);
    }

    private void configureOptionsMenuItems(Menu menu) {
        MenuItem editGroupItem = menu.findItem(R.id.menu_group_profile_edit);
        MenuItem leaveGroupItem = menu.findItem(R.id.menu_group_profile_leave);
        MenuItem listAttachmentsItem = menu.findItem(R.id.menu_audio_attachment_list);
        MenuItem removeGroupItem = menu.findItem(R.id.menu_group_profile_remove);

        mFriendsRequestMenuItem = menu.findItem(R.id.menu_group_profile_request_friends);

        editGroupItem.setVisible(false);
        leaveGroupItem.setVisible(false);

        if (!(mContact.isNearbyGroup() || mContact.isWorldwideGroup())) {
            if (mContact.isEditable()) {
                editGroupItem.setVisible(true);
                listAttachmentsItem.setVisible(true);
            } else {
                editGroupItem.setVisible(false);
                if (mContact.isGroupJoined()) {
                    leaveGroupItem.setVisible(true);
                    listAttachmentsItem.setVisible(true);
                } else if (mContact.isKeptGroup()) {
                    removeGroupItem.setVisible(true);
                }
            }
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        updateFriendsRequestMenuItem();
    }

    private void updateFriendsRequestMenuItem() {
        if (mGroupMemberAdapter == null) {
            return;
        }
        updateContactsToInviteAsFriend();
        String buttonText;
        if (mContactsToDisinviteAsFriend.isEmpty()) {
            int numberOfClients = mContactsToInviteAsFriend.size();
            buttonText = String.format(getString(R.string.group_invite_all), numberOfClients);
            if (numberOfClients > 0) {
                mFriendsRequestMenuItem.setVisible(true);
            } else {
                mFriendsRequestMenuItem.setVisible(false);
            }
        } else {
            mFriendsRequestMenuItem.setVisible(true);
            int numberOfInvitedClients = mContactsToDisinviteAsFriend.size();
            buttonText = String.format(getString(R.string.group_disinvite_all), numberOfInvitedClients);
        }
        mFriendsRequestMenuItem.setTitle(buttonText);
    }

    private void updateContactsToInviteAsFriend() {
        mContactsToInviteAsFriend = new ArrayList<TalkClientContact>();
        if (mGroupMemberAdapter == null) {
            return;
        }
        List<TalkClientContact> contacts = mGroupMemberAdapter.getContacts();
        for (TalkClientContact contact : contacts) {
            TalkRelationship relationship = contact.getClientRelationship();
            if (relationship == null || !relationship.isRelated()) {
                mContactsToInviteAsFriend.add(contact);
            }
        }
    }

    private void updateMemberContentVisibility() {
        if (mGroupMemberAdapter == null || mGroupMemberAdapter.getContacts().isEmpty()) {
            mGroupMembersContainer.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean isSelectionHandled;
        switch (item.getItemId()) {
            case R.id.menu_group_profile_edit:
                getActivity().startActionMode(this);
                isSelectionHandled = true;
                break;
            case R.id.menu_group_profile_leave:
                XoDialogs.showYesNoDialog("LeaveGroupDialog",
                        R.string.dialog_leave_group_title,
                        R.string.dialog_leave_group_message,
                        getActivity(),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                XoApplication.get().getXoClient().leaveGroup(mContact.getGroupId());
                                getActivity().finish();
                            }
                        });
                isSelectionHandled = true;
                break;
            case R.id.menu_audio_attachment_list:
                Intent intent = new Intent(getActivity(), MediaBrowserActivity.class);
                intent.putExtra(IntentHelper.EXTRA_CONTACT_ID, mContact.getClientContactId());
                startActivity(intent);
                isSelectionHandled = true;
                break;
            case R.id.menu_group_profile_remove:
                XoDialogs.showYesNoDialog("RemoveGroupDialog",
                        R.string.dialog_remove_group_title,
                        R.string.dialog_remove_group_message,
                        getActivity(),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                removeGroup();
                                getActivity().finish();
                            }
                        });
                isSelectionHandled = true;
                break;
            case R.id.menu_group_profile_request_friends:
                inviteOrDisinviteMembersAsFriends();
            default:
                isSelectionHandled = super.onOptionsItemSelected(item);
        }
        return isSelectionHandled;
    }

    private void removeGroup() {
        if (mContact.getGroupPresence() != null) {
            mContact.getGroupPresence().setKept(false);
            try {
                XoApplication.get().getXoClient().getDatabase().saveGroupPresence(mContact.getGroupPresence());
            } catch (SQLException e) {
                LOG.error("SQL error", e);
            }
        }
    }

    private void saveEditedGroup() {
        String newGroupName = mNameEditText.getText().toString();
        if (newGroupName.isEmpty()) {
            newGroupName = "";
        }
        mNameText.setText(newGroupName);

        XoApplication.get().getXoClient().setGroupName(mContact, newGroupName);
    }

    protected void updateViews() {
        updateAvatarView();
        updateChatContainer();
        updateGroupName();
        updateContentVisibility();
        updateMemberContentVisibility();
    }

    private void updateChatContainer() {
        mChatContainer.setVisibility(View.VISIBLE);
        updateMessageText();
    }

    protected void updateMessageText() {
        try {
            int count = (int) XoApplication.get().getXoClient().getDatabase().getMessageCountByContactId(mContact.getClientContactId());
            updateMessageText(count);
        } catch (SQLException e) {
            LOG.error("SQL Error fetching message count from database.", e);
        }
    }

    private void updateMessageText(int count) {
        if (shouldShowChatContainer(count)) {
            mChatContainer.setVisibility(View.VISIBLE);
            mChatMessagesText.setText(getResources().getQuantityString(R.plurals.message_count, count, count));
        } else {
            mChatContainer.setVisibility(View.GONE);
        }
    }

    protected boolean shouldShowChatContainer(int count) {
        if (mContact.isKeptGroup() && count > 0) {
            return true;
        } else {
            int membershipCount = 0;
            try {
                membershipCount = XoApplication.get().getXoClient().getDatabase().findMembershipsInGroup(mContact.getGroupId()).size();
            } catch (SQLException e) {
                LOG.error("SQL error", e);
            }
            return mContact.isGroupJoined() && membershipCount > 1;
        }
    }

    private void updateContentVisibility() {
        switch (mMode) {
            case PROFILE:
                mNameText.setVisibility(View.VISIBLE);
                mNameEditText.setVisibility(View.GONE);
                mGroupMembersContainer.setVisibility(View.VISIBLE);
                break;
            case EDIT_GROUP:
                mNameText.setVisibility(View.GONE);
                mNameEditText.setVisibility(View.VISIBLE);
                mGroupMembersContainer.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    private void updateGroupName() {
        String name = null;

        TalkGroupPresence groupPresence = mContact.getGroupPresence();
        if (groupPresence != null) {
            name = groupPresence.getGroupName();
        }

        if (mMode == Mode.PROFILE) {
            if (name == null) {
                name = "";
            }
        } else if (mMode == Mode.EDIT_GROUP) {
            name = mNameEditText.getText().toString();
        }

        if (mContact.isNearbyGroup()) {
            mNameText.setText(R.string.all_nearby);
        } else if (mContact.isWorldwideGroup()) {
            mNameText.setText(R.string.all_worldwide);
        } else {
            mNameText.setText(name);
        }

        mNameEditText.setText(name);
    }

    private void updateAvatarView() {
        XoTransfer avatarTransfer;
        if (mContact.isGroupAdmin()) {
            avatarTransfer = mContact.getAvatarUpload();
        } else {
            avatarTransfer = mContact.getAvatarDownload();
        }

        Uri avatarUri = null;
        if (avatarTransfer != null && avatarTransfer.isContentAvailable() && avatarTransfer.getFilePath() != null) {
            avatarUri = UriUtils.getAbsoluteFileUri(avatarTransfer.getFilePath());
        }

        int placeholderId = getGroupPlaceholder();

        Picasso.with(getActivity())
                .load(avatarUri)
                .centerCrop()
                .fit()
                .placeholder(placeholderId)
                .error(placeholderId)
                .into(mAvatarImage);
    }

    private int getGroupPlaceholder() {
        int placeholderId = R.drawable.group_large;
        if (mContact.isNearbyGroup()) {
            placeholderId = R.drawable.location_large;
        } else if (mContact.isWorldwideGroup()) {
            placeholderId = R.drawable.world_large;
        }
        return placeholderId;
    }

    private void refreshContact() {
        try {
            XoApplication.get().getXoClient().getDatabase().refreshClientContact(mContact);
            if (mMode == Mode.PROFILE) {
                if (mContact.getAvatarDownload() != null) {
                    XoApplication.get().getXoClient().getDatabase().refreshClientDownload(mContact.getAvatarDownload());
                }
                if (mContact.getAvatarUpload() != null) {
                    XoApplication.get().getXoClient().getDatabase().refreshClientUpload(mContact.getAvatarUpload());
                }
            }
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateViews();
            }
        });
    }

    private void updateActionBar() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mContact.getGroupPresence() == null) {
                    getActivity().getActionBar().setTitle("");
                } else if (mContact.isNearbyGroup()) {
                    getActivity().getActionBar().setTitle(getString(R.string.all_nearby));
                } else if (mContact.isWorldwideGroup()) {
                    getActivity().getActionBar().setTitle(getString(R.string.all_worldwide));
                } else {
                    getActivity().getActionBar().setTitle(mContact.getNickname());
                }
            }
        });
    }

    private void manageGroupMembers() {
        Intent intent = new Intent(getActivity(), GroupContactSelectionResultActivity.class);
        intent.putIntegerArrayListExtra(ContactSelectionFragment.EXTRA_SELECTED_CONTACT_IDS, transformToContactIds(mGroupMemberAdapter.getContacts()));
        startActivityForResult(intent, SELECT_CONTACT_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == SELECT_CONTACT_REQUEST) {
            List<Integer> selectedContactIds = data.getIntegerArrayListExtra(ContactSelectionFragment.EXTRA_SELECTED_CONTACT_IDS);
            try {
                updateMemberships(selectedContactIds);
            } catch (SQLException e) {
                LOG.error("SQL error", e);
            }
        }
    }

    private void updateMemberships(List<Integer> selectedContactIds) throws SQLException {
        List<Integer> contactIdsToKick = (List<Integer>) CollectionUtils.subtract(transformToContactIds(mGroupMemberAdapter.getContacts()), selectedContactIds);
        List<Integer> contactIdsToInvite = (List<Integer>) CollectionUtils.subtract(selectedContactIds, transformToContactIds(mGroupMemberAdapter.getContacts()));

        for (int contactId : contactIdsToInvite) {
            TalkClientContact contact = XoApplication.get().getXoClient().getDatabase().findContactById(contactId);
            XoApplication.get().getXoClient().inviteClientToGroup(mContact.getGroupId(), contact.getClientId());
        }
        for (int contactId : contactIdsToKick) {
            TalkClientContact contact = XoApplication.get().getXoClient().getDatabase().findContactById(contactId);
            XoApplication.get().getXoClient().kickClientFromGroup(mContact.getGroupId(), contact.getClientId());
        }
    }

    private boolean isCurrentGroup(TalkClientContact contact) {
        return mContact == contact || mContact.getClientContactId() == contact.getClientContactId();
    }

    @Override
    public void onClientPresenceChanged(TalkClientContact contact) {

    }

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {
        if (mGroupMemberAdapter == null) {
            return;
        }

        if (mContactsToDisinviteAsFriend.contains(contact)) {
            TalkRelationship relationship = contact.getClientRelationship();
            if (relationship != null && relationship.isFriend()) {
                mContactsToDisinviteAsFriend.remove(contact);
            }
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateFriendsRequestMenuItem();
            }
        });
    }

    @Override
    public void onGroupPresenceChanged(final TalkClientContact contact) {
        if (isCurrentGroup(contact)) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateContact();
                    getActivity().invalidateOptionsMenu();
                }
            });
        }
    }

    @Override
    public void onGroupMembershipChanged(final TalkClientContact contact) {
        if (isCurrentGroup(contact)) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateContact();
                    getActivity().invalidateOptionsMenu();
                }
            });
        }
    }

    private void updateContact() {
        refreshContact();
        updateActionBar();
    }

    private void inviteAllMembersAsFriends() {
        if (mContactsToInviteAsFriend.isEmpty()) {
            return;
        }
        mContactsToDisinviteAsFriend.clear();
        mContactsToDisinviteAsFriend.addAll(mContactsToInviteAsFriend);
        for (TalkClientContact contact : mContactsToInviteAsFriend) {
            XoApplication.get().getXoClient().inviteFriend(contact);
        }
        updateFriendsRequestMenuItem();
    }

    private void disinviteAllMembersAsFriends() {
        for (TalkClientContact contact : mContactsToDisinviteAsFriend) {
            XoApplication.get().getXoClient().disinviteFriend(contact);
        }
        mContactsToDisinviteAsFriend.clear();
        updateFriendsRequestMenuItem();
    }

    private void enterAvatarEditMode() {
        if (mContact.isEditable()) {
            if (mContact.getAvatarFilePath() != null) {
                XoDialogs.showRadioSingleChoiceDialog("AvatarSelection",
                        R.string.dialog_avatar_options_title,
                        new String[]{
                                getResources().getString(R.string.dialog_set_avatar_option),
                                getResources().getString(R.string.dialog_delete_avatar_option)
                        },
                        getActivity(),
                        new XoDialogs.OnSingleSelectionFinishedListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id, int selectedItem) {
                                switch (selectedItem) {
                                    case 0: {
                                        selectAvatar();
                                    }
                                    break;
                                    case 1: {
                                        updateAvatar(null);
                                    }
                                }
                            }
                        });
            } else {
                selectAvatar();
            }
        }
    }

    @Override
    public void onAvatarSelected(SelectedContent avatar) {
        updateAvatar(avatar);
    }

    private void updateAvatar(final SelectedContent avatar) {
        if (avatar != null) {
            LOG.debug("creating avatar upload");
            TalkClientUpload upload = new TalkClientUpload();
            upload.initializeAsAvatar(avatar);
            try {
                XoApplication.get().getXoClient().getDatabase().saveClientUpload(upload);
                XoApplication.get().getXoClient().setGroupAvatar(mContact, upload);
            } catch (SQLException e) {
                LOG.error("sql error", e);
            }
        } else {
            XoApplication.get().getXoClient().setGroupAvatar(mContact, null);
        }
    }

    private void configureActionMenuItems(Menu menu) {
        MenuItem addPerson = menu.findItem(R.id.menu_group_profile_add_person);
        MenuItem deleteGroup = menu.findItem(R.id.menu_group_profile_delete);

        addPerson.setVisible(false);
        deleteGroup.setVisible(false);

        if (mMode == Mode.EDIT_GROUP) {
            deleteGroup.setVisible(true);
            addPerson.setVisible(true);
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        actionMode.getMenuInflater().inflate(R.menu.fragment_group_profile_edit, menu);

        mMode = Mode.EDIT_GROUP;
        configureActionMenuItems(menu);

        mAvatarImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enterAvatarEditMode();
            }
        });

        updateViews();

        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.menu_group_profile_delete:
                XoDialogs.showYesNoDialog("GroupDeleteDialog",
                        R.string.dialog_delete_group_title,
                        R.string.dialog_delete_group_message,
                        getActivity(),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                XoApplication.get().getXoClient().deleteContact(mContact);
                                getActivity().finish();
                            }
                        },
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });
                break;
            case R.id.menu_group_profile_add_person:
                manageGroupMembers();
                break;
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
        // hide softkeyboard
        InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromInputMethod(mNameEditText.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

        if (!mBackPressed) {
            String newGroupName = mNameEditText.getText().toString();
            if (!newGroupName.isEmpty()) {
                saveEditedGroup();
            }
        }

        mBackPressed = false;
        mAvatarImage.setOnClickListener(null);
        mMode = Mode.PROFILE;
        updateViews();

        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        TalkClientContact contact = (TalkClientContact) adapterView.getItemAtPosition(i);
        showChat(contact);
    }

    private void showChat(TalkClientContact contact) {
        Intent intent = new Intent(getActivity(), ChatActivity.class);
        intent.putExtra(IntentHelper.EXTRA_CONTACT_ID, contact.getClientContactId());
        startActivity(intent);
    }
}
