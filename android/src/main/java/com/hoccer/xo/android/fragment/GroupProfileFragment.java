package com.hoccer.xo.android.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.talk.model.TalkGroup;
import com.hoccer.talk.model.TalkGroupMember;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.adapter.ContactsAdapter;
import com.hoccer.xo.android.adapter.GroupContactsAdapter;
import com.hoccer.xo.android.base.XoFragment;
import com.hoccer.xo.android.content.SelectedContent;
import com.hoccer.xo.android.dialog.GroupManageDialog;
import com.hoccer.xo.release.R;
import com.nostra13.universalimageloader.core.ImageLoader;
import org.apache.log4j.Logger;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Fragment for display and editing of group profiles.
 */
public class GroupProfileFragment extends XoFragment
        implements View.OnClickListener, IXoContactListener, ActionMode.Callback,
        AdapterView.OnItemClickListener {

    private static final Logger LOG = Logger.getLogger(GroupProfileFragment.class);
    private boolean mBackPressed = false;
    private boolean mFromNearby = false;
    public static final String ARG_CREATE_GROUP = "ARG_CREATE_GROUP";
    public static final String ARG_CLIENT_CONTACT_ID = "ARG_CLIENT_CONTACT_ID";


    public enum Mode {
        PROFILE,
        CREATE_GROUP,
        EDIT_GROUP
    }

    private Mode mMode;

    private TextView mGroupNameText;
    private EditText mGroupNameEdit;
    private Button mGroupCreateButton;
    private Button mMakePermanentButton;
    private LinearLayout mGroupMembersContainer;
    private TextView mGroupMembersTitle;
    private ListView mGroupMembersList;
    private ContactsAdapter mGroupMemberAdapter;

    private TalkClientContact mGroup;
    private IContentObject mAvatarToSet;
    private ImageView mAvatarImage;

    private Menu mOptionsMenu;

    private ArrayList<TalkClientContact> mCurrentClientsInGroup = new ArrayList<TalkClientContact>();
    private ArrayList<TalkClientContact> mContactsToInvite = new ArrayList<TalkClientContact>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_group_profile, container, false);
        view.setFocusableInTouchMode(true);
        view.setOnKeyListener(new BackPressListener());

        mAvatarImage = (ImageView) view.findViewById(R.id.profile_group_profile_image);
        mGroupMembersContainer = (LinearLayout) view.findViewById(R.id.profile_group_members_container);
        mGroupMembersTitle = (TextView) mGroupMembersContainer.findViewById(R.id.profile_group_members_title);
        mGroupMembersList = (ListView) mGroupMembersContainer.findViewById(R.id.profile_group_members_list);
        mGroupCreateButton = (Button) view.findViewById(R.id.profile_group_button_create);
        mGroupCreateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveCreatedGroup();
                mGroupCreateButton.setEnabled(false);
            }
        });

        mGroupNameText = (TextView) view.findViewById(R.id.profile_group_name);
        mGroupNameEdit = (EditText) view.findViewById(R.id.profile_group_name_edit);
        mGroupNameEdit.setFocusableInTouchMode(true);
        mGroupNameEdit.setOnKeyListener(new BackPressListener());
        mGroupNameEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                if (charSequence.length() == 0) {
                    mGroupCreateButton.setEnabled(false);
                } else {
                    mGroupCreateButton.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        mMakePermanentButton = (Button) view.findViewById(R.id.profile_group_button_make_permanent);
        mMakePermanentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMakePermanentButton.setVisibility(View.GONE);
                createGroupFromNearby(mGroupMemberAdapter.getMembersIds());
            }
        });
        mGroupMembersContainer = (LinearLayout) view.findViewById(R.id.profile_group_members_container);
        mGroupMembersTitle = (TextView) mGroupMembersContainer.findViewById(R.id.profile_group_members_title);
        mGroupMembersList = (ListView) mGroupMembersContainer.findViewById(R.id.profile_group_members_list);

        if (getArguments() != null) {
            if (getArguments().getBoolean(ARG_CREATE_GROUP)) {
                createGroup();
            } else {
                int clientContactId = getArguments().getInt(ARG_CLIENT_CONTACT_ID);
                try {
                    mGroup = XoApplication.getXoClient().getDatabase().findClientContactById(clientContactId);
                    if(!mGroup.isGroup()) {
                        LOG.error("The given contact is not a group.");
                    }

                    mMode = Mode.PROFILE;
                    refreshContact(mGroup);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        } else {
            LOG.error("Creating GroupProfileFragment without arguments is not supported.");
        }

        return view;
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
    public void onResume() {
        super.onResume();
        getXoClient().registerContactListener(this);

        if (mGroupMemberAdapter == null) {
            mGroupMemberAdapter = new GroupContactsAdapter(getXoActivity(), mGroup);
            mGroupMemberAdapter.onCreate();
            mGroupMemberAdapter.onResume();

            if(mGroup.getGroupPresence() != null && mGroup.getGroupPresence().isTypeNearby()) {
                mGroupMemberAdapter.setFilter(new ContactsAdapter.Filter() {
                    @Override
                    public boolean shouldShow(TalkClientContact contact) {
                        return contact.isClientGroupInvited(mGroup) || contact.isClientGroupJoined(mGroup);
                    }
                });
            } else if (!mContactsToInvite.isEmpty()){
                mGroupMemberAdapter.setFilter(new ContactsAdapter.Filter() {
                    @Override
                    public boolean shouldShow(TalkClientContact contact) {
                        return mContactsToInvite.contains(contact);
                    }
                });
            } else {
                mGroupMemberAdapter.setFilter(new ContactsAdapter.Filter() {
                    @Override
                    public boolean shouldShow(TalkClientContact contact) {
                        return contact.isClientGroupInvited(mGroup) || contact.isClientGroupJoined(mGroup);
                    }
                });
            }
            mGroupMembersList.setAdapter(mGroupMemberAdapter);
        }
        mGroupMemberAdapter.requestReload();
        if(mGroup != null && mGroup.getGroupPresence() != null && mGroup.getGroupPresence().isTypeNearby()) {
            mGroupMembersList.setOnItemClickListener(this);
        } else {
            mGroupMembersList.setOnItemClickListener(null);
        }
        setHasOptionsMenu(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        getXoClient().unregisterContactListener(this);
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
        MenuItem rejectInvitationItem = menu.findItem(R.id.menu_group_profile_reject_invitation);
        MenuItem joinGroupItem = menu.findItem(R.id.menu_group_profile_join);
        MenuItem leaveGroupItem = menu.findItem(R.id.menu_group_profile_leave);

        editGroupItem.setVisible(false);
        rejectInvitationItem.setVisible(false);
        joinGroupItem.setVisible(false);
        leaveGroupItem.setVisible(false);

        if (mMode == Mode.CREATE_GROUP) {
            editGroupItem.setVisible(false);

        } else {
            if (mGroup.getGroupPresence() != null && !mGroup.getGroupPresence().isTypeNearby()) {
                if (mGroup.isEditable()) {
                    editGroupItem.setVisible(true);
                } else {
                    editGroupItem.setVisible(false);
                    if (mGroup.isGroupInvited()) {
                        rejectInvitationItem.setVisible(true);
                        joinGroupItem.setVisible(true);
                    } else if (mGroup.isGroupJoined()) {
                        leaveGroupItem.setVisible(true);
                    }
                }
            }
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_group_profile_edit:
                getActivity().startActionMode(this);
                break;
            case R.id.menu_group_profile_reject_invitation:
                XoDialogs.showYesNoDialog("RejectGroupInvitationDialog",
                        R.string.dialog_reject_group_invitation_title,
                        R.string.dialog_reject_group_invitation_message,
                        getXoActivity(),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                getXoActivity().getXoClient().leaveGroup(mGroup.getGroupId());
                                getXoActivity().finish();
                            }
                        },
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });
                break;
            case R.id.menu_group_profile_join:
                joinGroup();
                break;
            case R.id.menu_group_profile_leave:
                XoDialogs.showYesNoDialog("LeaveGroupDialog",
                        R.string.dialog_leave_group_title,
                        R.string.dialog_leave_group_message,
                        getXoActivity(),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                getXoActivity().getXoClient().leaveGroup(mGroup.getGroupId());
                                getXoActivity().finish();
                            }
                        },
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public TalkClientContact getContact() {
        return mGroup;
    }

    public void createGroupFromNearby(String[] clientIds) {
        mMode = Mode.EDIT_GROUP;
        mCurrentClientsInGroup.addAll(getCurrentContactsFromGroup(Arrays.asList(clientIds)));
        mContactsToInvite.addAll(mCurrentClientsInGroup);

        mGroup = TalkClientContact.createGroupContact();
        TalkGroup groupPresence = new TalkGroup();
        groupPresence.setGroupTag(mGroup.getGroupTag());
        groupPresence.setGroupType(TalkGroup.GROUP_TYPE_USER);
        mGroup.updateGroupPresence(groupPresence);
        update(mGroup);
        mFromNearby = true;

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mGroupCreateButton.setVisibility(View.GONE);
            }
        });

        getActivity().startActionMode(this);
    }

    private void saveCreatedGroup() {
        String newGroupName = mGroupNameEdit.getText().toString();
        if (newGroupName.isEmpty()) {
            newGroupName = "";
        }
        mGroupNameText.setText(newGroupName);

        if (mGroup != null && !mGroup.isGroupRegistered()) {
            if (mGroup.getGroupPresence() != null) {
                mGroup.getGroupPresence().setGroupName(newGroupName);
                getXoClient().createGroup(mGroup);
                mMode = Mode.EDIT_GROUP;
            }
        }
    }

    private void saveEditedGroup() {
        String newGroupName = mGroupNameEdit.getText().toString();
        if (newGroupName.isEmpty()) {
            newGroupName = "";
        }
        mGroupNameText.setText(newGroupName);

        getXoClient().setGroupName(mGroup, newGroupName);
    }

    private void updateAvatar(TalkClientContact contact) {
        String avatarUrl = "drawable://" + R.drawable.avatar_default_group_large;

        TalkClientUpload avatarUpload;
        TalkClientDownload avatarDownload;

        avatarUpload = contact.getAvatarUpload();
        if (avatarUpload != null) {
            if (avatarUpload.isContentAvailable()) {
                avatarUrl = avatarUpload.getContentDataUrl();
            }
        }

        if (avatarUpload == null) {
            avatarDownload = contact.getAvatarDownload();
            if (avatarDownload != null) {
                if (avatarDownload.isContentAvailable()) {
                    avatarUrl = avatarDownload.getDataFile();
                    Uri uri = Uri.fromFile(new File(avatarUrl));
                    avatarUrl = uri.toString();
                }
            }
        }

        ImageLoader.getInstance().displayImage(avatarUrl, mAvatarImage);
    }

    private void update(TalkClientContact contact) {
        updateAvatar(contact);

        mGroupMembersTitle.setVisibility(contact.isGroupRegistered() ? View.VISIBLE : View.GONE);
        mGroupMembersList.setVisibility(contact.isGroupRegistered() || !mCurrentClientsInGroup.isEmpty() ? View.VISIBLE : View.GONE);

        String name = null;

        TalkGroup groupPresence = contact.getGroupPresence();
        if (groupPresence != null) {
            name = groupPresence.getGroupName();
        }

        if (mMode == Mode.CREATE_GROUP) {
            name = mGroupNameEdit.getText().toString();

        } else if (mMode == Mode.PROFILE) {
            if (name == null) {
                name = "";
            }
        } else if (mMode == Mode.EDIT_GROUP) {
            name = mGroupNameEdit.getText().toString();
        }

        if(mGroup.getGroupPresence() != null && mGroup.getGroupPresence().isTypeNearby()) {
            mGroupNameText.setText(R.string.nearby_text);
            mMakePermanentButton.setVisibility(View.VISIBLE);
        } else {
            mGroupNameText.setText(name);
        }

        mGroupNameEdit.setText(name);

        switch (mMode) {
            case PROFILE:
                mGroupNameText.setVisibility(View.VISIBLE);
                mGroupNameEdit.setVisibility(View.GONE);
                mGroupCreateButton.setVisibility(View.GONE);
                mGroupMembersContainer.setVisibility(View.VISIBLE);
                break;
            case CREATE_GROUP:
                mGroupNameText.setVisibility(View.GONE);
                mGroupNameEdit.setVisibility(View.VISIBLE);
                mGroupCreateButton.setVisibility(View.VISIBLE);
                mGroupMembersContainer.setVisibility(View.GONE);
                break;
            case EDIT_GROUP:
                mGroupNameText.setVisibility(View.GONE);
                mGroupNameEdit.setVisibility(View.VISIBLE);
                mGroupCreateButton.setVisibility(View.GONE);
                mGroupMembersContainer.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    private void refreshContact(TalkClientContact newContact) {
        mGroup = newContact;

        try {
            getXoDatabase().refreshClientContact(mGroup);
            if (mMode == Mode.PROFILE) {
                if (mGroup.getAvatarDownload() != null) {
                    getXoDatabase().refreshClientDownload(mGroup.getAvatarDownload());
                }
                if (mGroup.getAvatarUpload() != null) {
                    getXoDatabase().refreshClientUpload(mGroup.getAvatarUpload());
                }
            }
        } catch (SQLException e) {
            LOG.error("SQL error", e);
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mGroup.isDeleted()) {
                    getXoActivity().finish();
                } else {
                    update(mGroup);
                }
            }
        });
    }

    private void createGroup() {
        mMode = Mode.CREATE_GROUP;

        mGroup = TalkClientContact.createGroupContact();
        TalkGroup groupPresence = new TalkGroup();
        groupPresence.setGroupTag(mGroup.getGroupTag());
        mGroup.updateGroupPresence(groupPresence);
        update(mGroup);
    }

    public void updateContactList(ArrayList<TalkClientContact> mContactsToInvite) {
        this.mContactsToInvite.clear();
        this.mContactsToInvite.addAll(mContactsToInvite);
        mGroupMemberAdapter.requestReload();
    }

    private void updateActionBar() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getActivity().getActionBar().setTitle(mGroup.getName());
            }
        });
    }

    private void manageGroupMembers() {
        if (mCurrentClientsInGroup.isEmpty()) {
           mCurrentClientsInGroup.addAll(getCurrentContactsFromGroup(Arrays.asList(mGroupMemberAdapter.getMembersIds())));
        }
        GroupManageDialog dialog = new GroupManageDialog(mGroup, mCurrentClientsInGroup, mContactsToInvite, mFromNearby);
        dialog.setTargetFragment(this, 0);
        dialog.show(getXoActivity().getSupportFragmentManager(), "GroupManageDialog");
    }

    private void joinGroup() {
        getXoClient().joinGroup(mGroup.getGroupId());
        getXoActivity().finish();
    }

    private boolean isCurrentGroup(TalkClientContact contact) {
        return mGroup != null && mGroup == contact || mGroup.getClientContactId() == contact.getClientContactId();
    }

    private List<TalkClientContact> getCurrentContactsFromGroup(List<String> ids) {
        List<TalkClientContact> result = new ArrayList<TalkClientContact>();
        try {
            List<TalkClientContact> allContacts = getXoDatabase().findAllClientContacts();
            for (TalkClientContact contact: allContacts) {
                if (contact.isClient() && ids.contains(contact.getClientId())) {
                    result.add(contact);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public void onContactAdded(TalkClientContact contact) {
        LOG.debug("onContactAdded");
        if (isCurrentGroup(contact)) {

            saveEditedGroup();

            final GroupProfileFragment fragment = this;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getXoActivity().startActionMode(fragment);
                }
            });
        }
    }

    @Override
    public void onContactRemoved(TalkClientContact contact) {
        if (isCurrentGroup(contact)) {
            getActivity().finish();
        }
    }

    @Override
    public void onClientPresenceChanged(TalkClientContact contact) {
    }

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {
    }

    @Override
    public void onGroupPresenceChanged(TalkClientContact contact) {
        if (isCurrentGroup(contact)) {
            refreshContact(contact);
            updateActionBar();
        }
    }

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) {
        if (isCurrentGroup(contact)) {
            refreshContact(contact);
            updateActionBar();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.profile_group_profile_image) {
            if (mGroup != null && mGroup.isEditable()) {
                getXoActivity().selectAvatar();
            }
        }
    }

    @Override
    public void onAvatarSelected(IContentObject contentObject) {
        mAvatarToSet = contentObject;
    }

    @Override
    public void onServiceConnected() {
        final IContentObject newAvatar = mAvatarToSet;
        mAvatarToSet = null;
        if (newAvatar != null) {
            XoApplication.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    TalkClientUpload upload = SelectedContent.createAvatarUpload(newAvatar);
                    try {
                        getXoDatabase().saveClientUpload(upload);
                        getXoClient().setGroupAvatar(mGroup, upload);
                    } catch (SQLException e) {
                        LOG.error("sql error", e);
                    }
                }
            });
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
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        actionMode.getMenuInflater().inflate(R.menu.fragment_group_profile_edit, menu);

        mMode = Mode.EDIT_GROUP;
        configureActionMenuItems(menu);
        update(mGroup);

        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.menu_group_profile_delete:
                XoDialogs.showYesNoDialog("GroupDeleteDialog",
                        R.string.dialog_delete_group_title,
                        R.string.dialog_delete_group_message,
                        getXoActivity(),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                XoApplication.getXoClient().deleteContact(mGroup);
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
        inputManager.hideSoftInputFromInputMethod(mGroupNameEdit.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

        if (!mBackPressed) {
            if (!mContactsToInvite.isEmpty()) {
                String newGroupName = mGroupNameEdit.getText().toString();
                if (mGroup != null && !mGroup.isGroupRegistered()) {
                    if (newGroupName.isEmpty()) {
                        newGroupName = "Permanent nearby";
                    }
                    mGroup.getGroupPresence().setGroupName(newGroupName);
                    getXoClient().createGroupWithContacts(mGroup, getMembersIds(), getMembersRoles());
                }
            } else {
                String newGroupName = mGroupNameEdit.getText().toString();
                if (!newGroupName.isEmpty()) {
                    saveEditedGroup();
                }
            }
        }
        mBackPressed = false;
        mAvatarImage.setOnClickListener(null);
        mMode = Mode.PROFILE;
        update(mGroup);

        configureOptionsMenuItems(mOptionsMenu);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        TalkClientContact contact = (TalkClientContact) adapterView.getItemAtPosition(i);
        getXoActivity().showContactConversation(contact);
    }

    private String[] getMembersIds() {
        String[] ids = new String[mContactsToInvite.size()];
        int i = 0;
        for (TalkClientContact contact: mContactsToInvite) {
            ids[i++] = contact.getClientId();
        }
        return ids;
    }

    private String[] getMembersRoles() {
        String[] roles = new String[mContactsToInvite.size()];
        for (int i = 0;  i < mContactsToInvite.size(); ++i) {
            roles[i] = TalkGroupMember.ROLE_MEMBER;
        }

        return roles;
    }
}
