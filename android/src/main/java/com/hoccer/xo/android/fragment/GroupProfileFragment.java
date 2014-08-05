package com.hoccer.xo.android.fragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
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
import com.hoccer.xo.android.activity.GroupProfileActivity;
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

    private static final Logger LOG = Logger.getLogger(SingleProfileFragment.class);
    private boolean mBackPressed = false;
    private boolean mFromNearby = false;


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
    private ArrayList<TalkClientContact> mClientsFromNearbyToInvite = new ArrayList<TalkClientContact>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        LOG.debug("onCreate()");
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        LOG.debug("onCreateView()");

        View v = inflater.inflate(R.layout.fragment_group_profile, container, false);
        v.setFocusableInTouchMode(true);
        v.setOnKeyListener(new onBackPressListener());

        mAvatarImage = (ImageView) v.findViewById(R.id.profile_group_profile_image);
        mGroupNameText = (TextView) v.findViewById(R.id.profile_group_name);
        mGroupNameEdit = (EditText) v.findViewById(R.id.profile_group_name_edit);
        mGroupNameEdit.setFocusableInTouchMode(true);
        mGroupNameEdit.setOnKeyListener(new onBackPressListener());
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

        mGroupCreateButton = (Button) v.findViewById(R.id.profile_group_button_create);
        mGroupCreateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveCreatedGroup();
                mGroupCreateButton.setEnabled(false);
            }
        });
        mMakePermanentButton = (Button) v.findViewById(R.id.profile_group_button_make_permanent);
        mMakePermanentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                makeNearbyPermanent();
                mGroupCreateButton.setEnabled(false);
            }
        });
        mGroupMembersContainer = (LinearLayout) v.findViewById(R.id.profile_group_members_container);
        mGroupMembersTitle = (TextView) mGroupMembersContainer.findViewById(R.id.profile_group_members_title);
        mGroupMembersList = (ListView) mGroupMembersContainer.findViewById(R.id.profile_group_members_list);
        return v;
    }

    class onBackPressListener implements View.OnKeyListener {

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
        LOG.debug("onResume()");
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
                        return contact.isClientGroupInvited(mGroup) || contact.isClientGroupJoined(mGroup) && !contact.isSelf();
                    }
                });
            } else if (!mClientsFromNearbyToInvite.isEmpty()){
                mGroupMemberAdapter.setFilter(new ContactsAdapter.Filter() {
                    @Override
                    public boolean shouldShow(TalkClientContact contact) {
                        return mClientsFromNearbyToInvite.contains(contact);
                    }
                });
            } else {
                mGroupMemberAdapter.setFilter(new ContactsAdapter.Filter() {
                    @Override
                    public boolean shouldShow(TalkClientContact contact) {
                        return contact.isClientGroupInvited(mGroup) || contact.isClientGroupJoined(mGroup) || contact.isSelf();
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
        LOG.debug("onPause()");
        super.onPause();
        getXoClient().unregisterContactListener(this);
    }

    @Override
    public void onDestroy() {
        LOG.debug("onDestroy()");
        super.onDestroy();
        if (mGroupMemberAdapter != null) {
            mGroupMemberAdapter.onPause();
            mGroupMemberAdapter.onDestroy();
            mGroupMemberAdapter = null;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        LOG.debug("onCreateOptionsMenu()");
        super.onCreateOptionsMenu(menu, menuInflater);

        mOptionsMenu = menu;
        configureOptionsMenuItems(menu);
    }

    private void configureOptionsMenuItems(Menu menu) {
        MenuItem myProfile = menu.findItem(R.id.menu_my_profile);
        myProfile.setVisible(true);

        MenuItem editGroup = menu.findItem(R.id.menu_group_profile_edit);
        MenuItem rejectInvitation = menu.findItem(R.id.menu_group_profile_reject_invitation);
        MenuItem joinGroup = menu.findItem(R.id.menu_group_profile_join);
        MenuItem leaveGroup = menu.findItem(R.id.menu_group_profile_leave);

        editGroup.setVisible(false);
        rejectInvitation.setVisible(false);
        joinGroup.setVisible(false);
        leaveGroup.setVisible(false);

        if (mMode == Mode.CREATE_GROUP) {
            editGroup.setVisible(false);

        } else {
            if (mGroup.getGroupPresence() != null && !mGroup.getGroupPresence().isTypeNearby()) {
                if (mGroup.isEditable()) {
                    editGroup.setVisible(true);
                } else {
                    editGroup.setVisible(false);
                    if (mGroup.isGroupInvited()) {
                        rejectInvitation.setVisible(true);
                        joinGroup.setVisible(true);
                    } else if (mGroup.isGroupJoined()) {
                        leaveGroup.setVisible(true);
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

    public void saveCreatedGroup() {
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

    public void saveEditedGroup() {
        String newGroupName = mGroupNameEdit.getText().toString();
        if (newGroupName.isEmpty()) {
            newGroupName = "";
        }
        mGroupNameText.setText(newGroupName);

        getXoClient().setGroupName(mGroup, newGroupName);
    }

    private void makeNearbyPermanent() {
        Intent intent = new Intent(getActivity(), GroupProfileActivity.class);
        intent.putExtra(GroupProfileActivity.EXTRA_MAKE_FROM_NEARBY, mGroupMemberAdapter.getMembersIds());
        getActivity().startActivity(intent);
    }

    private void updateAvatar(TalkClientContact contact) {

        String avatarUrl = "drawable://" + R.drawable.avatar_default_group_large;

        TalkClientUpload avatarUpload;
        TalkClientDownload avatarDownload;

        avatarUpload = contact.getAvatarUpload();
        if (avatarUpload != null) {
            if (avatarUpload.isContentAvailable()) {
                avatarUrl = avatarUpload.getDataFile();
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
        LOG.debug("avatar is " + avatarUrl);
        ImageLoader.getInstance().displayImage(avatarUrl, mAvatarImage);
    }

    private void update(TalkClientContact contact) {
        LOG.debug("update(" + contact.getClientContactId() + ")");

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

    public void refreshContact(TalkClientContact newContact) {
        LOG.debug("refreshContact()");

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
                LOG.debug("updating ui");
                if (mGroup.isDeleted()) {
                    getXoActivity().finish();
                } else {
                    update(mGroup);
                }
            }
        });
    }

    public void showProfile(TalkClientContact contact) {
        mMode = Mode.PROFILE;
        if (contact != null) {
            LOG.debug("showProfile(" + contact.getClientContactId() + ")");
        }
        refreshContact(contact);
    }

    public void createGroup() {
        mMode = Mode.CREATE_GROUP;
        LOG.debug("createGroup()");

        mGroup = TalkClientContact.createGroupContact();
        TalkGroup groupPresence = new TalkGroup();
        groupPresence.setGroupTag(mGroup.getGroupTag());
        mGroup.updateGroupPresence(groupPresence);
        update(mGroup);
    }

    public void createGroupFromNearby(String[] clientIds) {
        mMode = Mode.EDIT_GROUP;
        LOG.debug("createGroupFromNearby()");
        mCurrentClientsInGroup.addAll(getCurrentContactsFromGroup(Arrays.asList(clientIds)));
        mClientsFromNearbyToInvite.addAll(mCurrentClientsInGroup);

        mGroup = TalkClientContact.createGroupContact();
        TalkGroup groupPresence = new TalkGroup();
        groupPresence.setGroupTag(mGroup.getGroupTag());
        mGroup.updateGroupPresence(groupPresence);
        update(mGroup);
        mFromNearby = true;
        getActivity().startActionMode(this);
    }

    public void updateContactLis(ArrayList<TalkClientContact> mContactsToInvite) {
        mClientsFromNearbyToInvite.clear();
        mClientsFromNearbyToInvite.addAll(mContactsToInvite);
        mGroupMemberAdapter.requestReload();
    }

    private void manageGroupMembers() {
        LOG.debug("manageGroupMembers()");
        if (mCurrentClientsInGroup.isEmpty()) {
           mCurrentClientsInGroup.addAll(getCurrentContactsFromGroup(Arrays.asList(mGroupMemberAdapter.getMembersIds())));
        }
        GroupManageDialog dialog = new GroupManageDialog(mGroup, mCurrentClientsInGroup, mClientsFromNearbyToInvite, mFromNearby);
        dialog.setTargetFragment(this, 0);
        dialog.show(getXoActivity().getSupportFragmentManager(), "GroupManageDialog");
    }

    private void joinGroup() {
        getXoClient().joinGroup(mGroup.getGroupId());
        getXoActivity().finish();
    }

    public TalkClientContact getContact() {
        return mGroup;
    }

    private boolean isMyContact(TalkClientContact contact) {
        return mGroup != null && mGroup == contact || mGroup.getClientContactId() == contact.getClientContactId();
    }

    private List<TalkClientContact> getCurrentContactsFromGroup(List<String> ids) {
        List<TalkClientContact> result = new ArrayList<TalkClientContact>();
        try {
            List<TalkClientContact> allContacts = getXoDatabase().findAllClientContacts();
            for (TalkClientContact c: allContacts) {
                if (c.isClient() && ids.contains(c.getClientId())) {
                    result.add(c);
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
        if (isMyContact(contact)) {

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
        LOG.debug("onContactRemoved");
        if (isMyContact(contact)) {
            getXoActivity().finish();
        }
    }

    @Override
    public void onClientPresenceChanged(TalkClientContact contact) {
        if (isMyContact(contact)) {
            refreshContact(contact);
        }
    }

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {
        if (isMyContact(contact)) {
            refreshContact(contact);
        }
    }

    @Override
    public void onGroupPresenceChanged(TalkClientContact contact) {
        if (isMyContact(contact)) {
            refreshContact(contact);
        }
    }

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) {
        if (isMyContact(contact)) {
            refreshContact(contact);
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
        LOG.debug("onAvatarSelected(" + contentObject.getContentDataUrl() + ")");
        mAvatarToSet = contentObject;
    }

    @Override
    public void onServiceConnected() {
        LOG.debug("onServiceConnected()");

        final IContentObject newAvatar = mAvatarToSet;
        mAvatarToSet = null;
        if (newAvatar != null) {
            XoApplication.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    LOG.debug("creating avatar upload");
                    TalkClientUpload upload = SelectedContent.createAvatarUpload(newAvatar);
                    try {
                        getXoDatabase().saveClientUpload(upload);
                        if (mGroup.isSelf()) {
                            getXoClient().setClientAvatar(upload);
                        }
                        if (mGroup.isGroup()) {
                            getXoClient().setGroupAvatar(mGroup, upload);
                        }
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
        LOG.debug("onOptionsItemSelected(" + menuItem.toString() + ")");
        switch (menuItem.getItemId()) {
            case R.id.menu_group_profile_delete:
                XoDialogs.showYesNoDialog("GroupDeleteDialog",
                        R.string.dialog_delete_group_title,
                        R.string.dialog_delete_group_message,
                        getXoActivity(),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                getXoActivity().getXoClient().deleteContact(mGroup);
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
        if (!mBackPressed) {
            if (!mClientsFromNearbyToInvite.isEmpty()) {
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
        String[] ids = new String[mClientsFromNearbyToInvite.size()];
        int i = 0;
        for (TalkClientContact c: mClientsFromNearbyToInvite) {
            ids[i++] = c.getClientId();
        }
        return ids;
    }

    private String[] getMembersRoles() {
        String[] ids = new String[mClientsFromNearbyToInvite.size()];
        int i = 0;
        for (TalkClientContact c: mClientsFromNearbyToInvite) {
            ids[i++] = TalkGroupMember.ROLE_MEMBER;
        }
        return ids;
    }

}
