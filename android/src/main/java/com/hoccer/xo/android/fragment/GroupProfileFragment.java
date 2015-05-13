package com.hoccer.xo.android.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.artcom.hoccer.R;
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
import com.hoccer.xo.android.activity.MediaBrowserActivity;
import com.hoccer.xo.android.adapter.ContactsAdapter;
import com.hoccer.xo.android.adapter.GroupMemberContactsAdapter;
import com.hoccer.xo.android.dialog.GroupManageDialog;
import com.hoccer.xo.android.util.IntentHelper;
import com.hoccer.xo.android.util.UriUtils;
import com.squareup.picasso.Picasso;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


/**
 * Fragment for display and editing of group profiles.
 */
public class GroupProfileFragment extends ProfileFragment
        implements View.OnClickListener, ActionMode.Callback, AdapterView.OnItemClickListener {

    private static final Logger LOG = Logger.getLogger(GroupProfileFragment.class);

    private boolean mBackPressed;
    public static final String ARG_START_IN_ACTION_MODE = "ARG_START_IN_ACTION_MODE";

    public enum Mode {
        PROFILE,
        EDIT_GROUP
    }

    private Mode mMode;

    private TextView mNameText;
    private EditText mNameEditText;
    private ImageView mAvatarImage;
    private Button mInviteAllButton;
    private LinearLayout mGroupMembersContainer;
    private ListView mGroupMembersList;

    @Nullable
    private ContactsAdapter mGroupMemberAdapter;

    private Menu mOptionsMenu;

    private final List<TalkClientContact> mCurrentClientsInGroup = new ArrayList<TalkClientContact>();
    private final ArrayList<TalkClientContact> mContactsToDisinviteAsFriend = new ArrayList<TalkClientContact>();
    private ArrayList<TalkClientContact> mContactsToInviteAsFriend = new ArrayList<TalkClientContact>();

    private final ContactsAdapter.Filter mInvitedOrJoinedClientFilter = new ContactsAdapter.Filter() {
        @Override
        public boolean shouldShow(TalkClientContact contact) {
            try {
                if (contact.isClient()) {
                    TalkGroupMembership membership = getXoActivity().getXoDatabase().findMembershipInGroupByClientId(mContact.getGroupId(), contact.getClientId());
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

        mAvatarImage = (ImageView) view.findViewById(R.id.profile_group_profile_image);
        mGroupMembersContainer = (LinearLayout) view.findViewById(R.id.profile_group_members_container);
        mGroupMembersList = (ListView) mGroupMembersContainer.findViewById(R.id.profile_group_members_list);

        mNameText = (TextView) view.findViewById(R.id.profile_group_name);
        mNameEditText = (EditText) view.findViewById(R.id.profile_group_name_edit);
        mNameEditText.setFocusableInTouchMode(true);
        mNameEditText.setOnKeyListener(new BackPressListener());

        mInviteAllButton = (Button) view.findViewById(R.id.profile_group_button_invite_all);
        mInviteAllButton.setOnClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mMode = Mode.PROFILE;

        if (mGroupMemberAdapter == null) {
            mGroupMemberAdapter = new GroupMemberContactsAdapter(getXoActivity(), mContact.getGroupId());
            mGroupMemberAdapter.setFilter(mInvitedOrJoinedClientFilter);
            mGroupMemberAdapter.onCreate();
            mGroupMemberAdapter.onResume();
            mGroupMembersList.setAdapter(mGroupMemberAdapter);
        }
        mGroupMemberAdapter.requestReload();

        if (mContact.getGroupPresence() != null && mContact.getGroupPresence().isTypeNearby()) {
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
    protected int getClientContactId() {
        return mContact.getClientContactId();
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
            mGroupMemberAdapter.onPause();
            mGroupMemberAdapter.onDestroy();
            mGroupMemberAdapter = null;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);

        menuInflater.inflate(R.menu.fragment_group_profile, menu);

        mOptionsMenu = menu;
        configureOptionsMenuItems(menu);
    }

    private void configureOptionsMenuItems(Menu menu) {
        MenuItem editGroupItem = menu.findItem(R.id.menu_group_profile_edit);
        MenuItem leaveGroupItem = menu.findItem(R.id.menu_group_profile_leave);
        MenuItem listAttachmentsItem = menu.findItem(R.id.menu_audio_attachment_list);
        MenuItem removeGroupItem = menu.findItem(R.id.menu_group_profile_remove);

        editGroupItem.setVisible(false);
        leaveGroupItem.setVisible(false);

        if (mContact.getGroupPresence() != null && !mContact.getGroupPresence().isTypeNearby()) {
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
                        getXoActivity(),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                getXoActivity().getXoClient().leaveGroup(mContact.getGroupId());
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
                        getXoActivity(),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                removeGroup();
                                getActivity().finish();
                            }
                        });
                isSelectionHandled = true;
                break;
            default:
                isSelectionHandled = super.onOptionsItemSelected(item);
        }
        return isSelectionHandled;
    }

    private void removeGroup() {
        if (mContact.getGroupPresence() != null) {
            mContact.getGroupPresence().setKept(false);
            try {
                getXoDatabase().saveGroupPresence(mContact.getGroupPresence());
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

        getXoClient().setGroupName(mContact, newGroupName);
    }

    @Override
    protected void updateView() {
        updateAvatarView();
        updateChatContainer();
        updateGroupName();
        updateContentVisibility();
        updateMemberContentVisibility();
    }

    private void updateMemberContentVisibility() {
        if (mGroupMemberAdapter == null || mGroupMemberAdapter.getContacts().isEmpty()) {
            mGroupMembersContainer.setVisibility(View.GONE);
            mInviteAllButton.setVisibility(View.GONE);
        }
    }

    private void updateChatContainer() {
        mChatContainer.setVisibility(View.VISIBLE);
        updateMessageText();
    }

    @Override
    protected void updateMessageText() {
        super.updateMessageText(getMessageCount());
    }

    private int getMessageCount() {
        int count = 0;
        try {
            count = (int) XoApplication.get().getXoClient().getDatabase().getMessageCountByContactId(mContact.getClientContactId());
        } catch (SQLException e) {
            LOG.error("SQL Error fetching message count from database.", e);
        }
        return count;
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

        if (mContact.getGroupPresence() != null && mContact.getGroupPresence().isTypeNearby()) {
            mNameText.setText(R.string.all_nearby);
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

        Picasso.with(getActivity())
                .load(avatarUri)
                .centerCrop()
                .fit()
                .placeholder(R.drawable.avatar_group_large)
                .error(R.drawable.avatar_group_large)
                .into(mAvatarImage);
    }

    private void refreshContact() {
        try {
            getXoDatabase().refreshClientContact(mContact);
            if (mMode == Mode.PROFILE) {
                if (mContact.getAvatarDownload() != null) {
                    getXoDatabase().refreshClientDownload(mContact.getAvatarDownload());
                }
                if (mContact.getAvatarUpload() != null) {
                    getXoDatabase().refreshClientUpload(mContact.getAvatarUpload());
                }
            }
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateView();
            }
        });
    }

    private void updateActionBar() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mContact.getGroupPresence() == null) {
                    getActivity().getActionBar().setTitle("");
                } else if (mContact.getGroupPresence().isTypeNearby()) {
                    getActivity().getActionBar().setTitle(getActivity().getResources().getString(R.string.all_nearby));
                } else {
                    getActivity().getActionBar().setTitle(mContact.getNickname());
                }
            }
        });
    }

    private void manageGroupMembers() {
        if (mCurrentClientsInGroup.isEmpty()) {
            mCurrentClientsInGroup.addAll(mGroupMemberAdapter.getContacts());
        }
        GroupManageDialog dialog = new GroupManageDialog(mContact, mCurrentClientsInGroup);
        dialog.setTargetFragment(this, 0);
        dialog.show(getActivity().getSupportFragmentManager(), "GroupManageDialog");
    }

    private boolean isCurrentGroup(TalkClientContact contact) {
        return mContact == contact || mContact.getClientContactId() == contact.getClientContactId();
    }

    @Override
    protected boolean shouldShowChatContainer(int count) {
        if (mContact.isKeptGroup() && getMessageCount() > 0) {
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
        getXoActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateInviteButton();
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
                    configureOptionsMenuItems(mOptionsMenu);
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
                    configureOptionsMenuItems(mOptionsMenu);
                }
            });
        }
    }

    private void updateContact() {
        refreshContact();
        updateActionBar();
        updateInviteButton();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.profile_group_profile_image:
                enterAvatarEditMode();
                break;
            case R.id.profile_group_button_invite_all:
                if (mContactsToDisinviteAsFriend.isEmpty()) {
                    inviteAllMembersAsFriends();
                } else {
                    disinviteAllMembersAsFriends();
                }
                break;
        }
    }

    private void updateInviteButton() {
        if (mGroupMemberAdapter == null) {
            return;
        }
        updateContactsToInviteAsFriend();
        String buttonText = "";
        if (mContactsToDisinviteAsFriend.isEmpty()) {
            int numberOfClients = mContactsToInviteAsFriend.size();
            buttonText = String.format(getString(R.string.nearby_invite_all), numberOfClients);
            if (numberOfClients > 0) {
                mInviteAllButton.setEnabled(true);
            } else {
                mInviteAllButton.setEnabled(false);
            }
        } else {
            mInviteAllButton.setEnabled(true);
            int numberOfInvitedClients = mContactsToDisinviteAsFriend.size();
            buttonText = String.format(getString(R.string.nearby_disinvite_all), numberOfInvitedClients);
        }
        mInviteAllButton.setText(buttonText);
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

    private void inviteAllMembersAsFriends() {
        if (mContactsToInviteAsFriend.isEmpty()) {
            return;
        }
        mContactsToDisinviteAsFriend.clear();
        mContactsToDisinviteAsFriend.addAll(mContactsToInviteAsFriend);
        for (TalkClientContact contact : mContactsToInviteAsFriend) {
            getXoClient().inviteFriend(contact);
        }
        updateInviteButton();
    }

    private void disinviteAllMembersAsFriends() {
        for (TalkClientContact contact : mContactsToDisinviteAsFriend) {
            getXoClient().disinviteFriend(contact);
        }
        mContactsToDisinviteAsFriend.clear();
        updateInviteButton();
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
                                        getXoActivity().selectAvatar();
                                    }
                                    break;
                                    case 1: {
                                        updateAvatar(null);
                                    }
                                }
                            }
                        });
            } else {
                getXoActivity().selectAvatar();
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
                getXoDatabase().saveClientUpload(upload);
                getXoClient().setGroupAvatar(mContact, upload);
            } catch (SQLException e) {
                LOG.error("sql error", e);
            }
        } else {
            getXoClient().setGroupAvatar(mContact, null);
        }
    }

    private void configureActionMenuItems(Menu menu) {

        MenuItem addPerson = menu.findItem(R.id.menu_group_profile_add_person);
        MenuItem deleteGroup = menu.findItem(R.id.menu_group_profile_delete);

        addPerson.setVisible(false);
        deleteGroup.setVisible(false);
        mAvatarImage.setOnClickListener(null);

        if (mMode == Mode.EDIT_GROUP) {
            mAvatarImage.setOnClickListener(this);
            deleteGroup.setVisible(true);
            addPerson.setVisible(true);
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        actionMode.getMenuInflater().inflate(R.menu.fragment_group_profile_edit, menu);

        mMode = Mode.EDIT_GROUP;
        configureActionMenuItems(menu);
        updateView();

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
        updateView();

        configureOptionsMenuItems(mOptionsMenu);
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
