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
import com.hoccer.xo.android.activity.MediaBrowserActivity;
import com.hoccer.xo.android.adapter.ContactsAdapter;
import com.hoccer.xo.android.adapter.GroupContactsAdapter;
import com.hoccer.xo.android.base.IProfileFragmentManager;
import com.hoccer.xo.android.base.XoFragment;
import com.hoccer.xo.android.content.SelectedContent;
import com.hoccer.xo.android.dialog.GroupManageDialog;
import com.hoccer.xo.android.util.IntentHelper;
import com.artcom.hoccer.R;
import com.squareup.picasso.Picasso;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Fragment for display and editing of group profiles.
 */
public class GroupProfileCreationFragment extends XoFragment implements IXoContactListener, View.OnClickListener, AdapterView.OnItemClickListener {

    private static final Logger LOG = Logger.getLogger(GroupProfileCreationFragment.class);
    public static final String ARG_CREATE_GROUP = "ARG_CREATE_GROUP";
    public static final String ARG_CLIENT_CONTACT_ID = "ARG_CLIENT_CONTACT_ID";
    public static final String ARG_CLONE_CURRENT_GROUP = "ARG_CLONE_CURRENT_GROUP";

    private TextView mGroupNameText;
    private EditText mGroupNameEdit;
    private Button mGroupCreateButton;
    private TextView mGroupMembersTitle;
    private ListView mGroupMembersList;
    @Nullable
    private ContactsAdapter mGroupMemberAdapter;

    private TalkClientContact mGroup;
    private IContentObject mAvatarToSet;
    private ImageView mAvatarImage;

    private ArrayList<TalkClientContact> mCurrentClientsInGroup = new ArrayList<TalkClientContact>();
    private ArrayList<TalkClientContact> mContactsToInviteToGroup = new ArrayList<TalkClientContact>();

    private boolean mCloneGroupContact = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_group_profile, container, false);
        view.setFocusableInTouchMode(true);

        mAvatarImage = (ImageView) view.findViewById(R.id.profile_group_profile_image);
        LinearLayout mGroupMembersContainer = (LinearLayout) view.findViewById(R.id.profile_group_members_container);
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

        initialize();

        return view;
    }

    private void initialize() {
        Bundle arguments = getArguments();
        if (arguments == null) {
            return;
        }

        if (arguments.getBoolean(ARG_CREATE_GROUP)) {
            createGroup(null);

        } else if (arguments.getBoolean(ARG_CLONE_CURRENT_GROUP)) {
            mCloneGroupContact = true;
            if (arguments.getInt(ARG_CLIENT_CONTACT_ID) == 0) {
                LOG.error("Cloning a group without valid id is not supported.");
            }
            int clientContactId = arguments.getInt(ARG_CLIENT_CONTACT_ID);
            try {
                mGroup = XoApplication.getXoClient().getDatabase().findContactById(clientContactId);
            } catch (SQLException e) {
                LOG.error("SQL error while retrieving group contact ", e);
            }
            if (mGroup != null) {
                if (!mGroup.isGroup()) {
                    LOG.error("The given contact is not a group.");
                    return;
                }
                createGroup(mGroup);
            }
        } else {
            LOG.error("Creating GroupProfileFragment without arguments is not supported.");
        }
    }

    public void onPause() {
        super.onPause();

        getXoClient().unregisterContactListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mGroupMemberAdapter == null) {
            mGroupMemberAdapter = new GroupContactsAdapter(getXoActivity(), mGroup);
            mGroupMemberAdapter.onCreate();
            mGroupMemberAdapter.onResume();

            if (mGroup.getGroupPresence() != null && mGroup.getGroupPresence().isTypeNearby()) {
                mGroupMemberAdapter.setFilter(new ContactsAdapter.Filter() {
                    @Override
                    public boolean shouldShow(TalkClientContact contact) {
                        return contact.isClientGroupInvited(mGroup) || contact.isClientGroupJoined(mGroup);
                    }
                });
            } else if (!mContactsToInviteToGroup.isEmpty()) {
                mGroupMemberAdapter.setFilter(new ContactsAdapter.Filter() {
                    @Override
                    public boolean shouldShow(TalkClientContact contact) {
                        return mContactsToInviteToGroup.contains(contact);
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
        if (mGroup != null && mGroup.getGroupPresence() != null && mGroup.getGroupPresence().isTypeNearby()) {
            mGroupMembersList.setOnItemClickListener(this);
        } else {
            mGroupMembersList.setOnItemClickListener(null);
        }
        setHasOptionsMenu(true);

        getXoClient().registerContactListener(this);

        // request focus
        mGroupNameEdit.requestFocus();
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

        menuInflater.inflate(R.menu.fragment_group_profile_create, menu);
        MenuItem listAttachmentsItem = menu.findItem(R.id.menu_audio_attachment_list);
        listAttachmentsItem.setVisible(true);

        MenuItem addPerson = menu.findItem(R.id.menu_group_profile_add_person);
        if (mContactsToInviteToGroup.isEmpty()) {
            addPerson.setVisible(false);
        } else {
            addPerson.setVisible(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean isSelectionHandled;
        switch (item.getItemId()) {
            case R.id.menu_audio_attachment_list:
                Intent intent = new Intent(getActivity(), MediaBrowserActivity.class);
                intent.putExtra(IntentHelper.EXTRA_CONTACT_ID, mGroup.getClientContactId());
                startActivity(intent);
                isSelectionHandled = true;
                break;
            case R.id.menu_group_profile_add_person:
                manageGroupMembers();
                isSelectionHandled = true;
                break;
            default:
                isSelectionHandled = super.onOptionsItemSelected(item);
        }
        return isSelectionHandled;
    }

    public TalkClientContact getContact() {
        return mGroup;
    }

    public List<String> getMembersIdsFromGroupContacts(List<TalkClientContact> groupContacts) {
        List<String> memberIds = new ArrayList<String>();
        for (TalkClientContact contact : groupContacts) {
            memberIds.add(contact.getClientId());
        }
        return memberIds;
    }

    private List<String> getMembersRoles(List<TalkClientContact> groupContacts) {
        List<String> roles = new ArrayList<String>();
        for (TalkClientContact contact : groupContacts) {
            roles.add(TalkGroupMember.ROLE_MEMBER);
        }
        return roles;
    }

    private void createGroup(TalkClientContact group) {
        if (group != null) {
            List<TalkClientContact> groupContacts = null;

            try {
                groupContacts = getXoDatabase().findClientsInGroup(group);
            } catch (SQLException e) {
                LOG.error("SQL error while retrieving group members ", e);
            }

            if (groupContacts != null) {
                List<String> clientIds = getMembersIdsFromGroupContacts(groupContacts);
                mCurrentClientsInGroup.addAll(getCurrentContactsFromGroup(clientIds));
                mContactsToInviteToGroup.addAll(mCurrentClientsInGroup);
            }
        }

        mGroup = TalkClientContact.createGroupContact();
        TalkGroup groupPresence = new TalkGroup();
        groupPresence.setGroupTag(mGroup.getGroupTag());
        groupPresence.setGroupName("");
        mGroup.updateGroupPresence(groupPresence);
        update();
    }

    private void saveCreatedGroup() {
        String newGroupName = mGroupNameEdit.getText().toString();
        if (newGroupName.isEmpty()) {
            newGroupName = "";
        }

        updateActionBar();

        if (mGroup != null && !mGroup.isGroupRegistered()) {
            if (mGroup.getGroupPresence() != null) {
                mGroup.getGroupPresence().setGroupName(newGroupName);

                if (mContactsToInviteToGroup.isEmpty()) {
                    getXoClient().createGroup(mGroup);
                } else {
                    String[] memberIds = getMembersIdsFromGroupContacts(mContactsToInviteToGroup).toArray(new String[mContactsToInviteToGroup.size()]);
                    String[] memberRoles = getMembersRoles(mContactsToInviteToGroup).toArray(new String[mContactsToInviteToGroup.size()]);
                    getXoClient().createGroupWithContacts(mGroup, memberIds, memberRoles);
                }
            }
        }
    }

    private void update() {
        LOG.debug("update(" + mGroup.getClientContactId() + ")");

        String avatarUrl = null;
        if (mGroup.isGroupAdmin()) {
            TalkClientUpload avatarUpload = mGroup.getAvatarUpload();
            if (avatarUpload != null && avatarUpload.isContentAvailable()) {
                avatarUrl = avatarUpload.getContentDataUrl();
            }
        } else {
            TalkClientDownload avatarDownload = mGroup.getAvatarDownload();
            if (avatarDownload != null && avatarDownload.isContentAvailable()) {
                if (avatarDownload.getDataFile() != null) {
                    Uri uri = Uri.fromFile(new File(avatarDownload.getDataFile()));
                    avatarUrl = uri.toString();
                }
            }
        }
        Picasso.with(getActivity())
                .load(avatarUrl)
                .placeholder(R.drawable.avatar_default_group_large)
                .error(R.drawable.avatar_default_group_large)
                .into(mAvatarImage);

        mGroupMembersTitle.setVisibility(!mCurrentClientsInGroup.isEmpty() ? View.VISIBLE : View.GONE);
        mGroupMembersList.setVisibility(!mCurrentClientsInGroup.isEmpty() ? View.VISIBLE : View.GONE);

        String name = mGroupNameEdit.getText().toString();
        mGroupNameEdit.setText(name);

        mGroupNameText.setVisibility(View.GONE);
        mGroupNameEdit.setVisibility(View.VISIBLE);
        mGroupCreateButton.setVisibility(View.VISIBLE);
    }

    public void updateContactList(ArrayList<TalkClientContact> contactsToInvite) {
        this.mContactsToInviteToGroup.clear();
        this.mContactsToInviteToGroup.addAll(contactsToInvite);
        mGroupMemberAdapter.requestReload();
    }

    private void updateActionBar() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getActivity().getActionBar().setTitle(mGroup.getNickname());
            }
        });
    }

    private void manageGroupMembers() {
        if (mCurrentClientsInGroup.isEmpty()) {
            mCurrentClientsInGroup.addAll(getCurrentContactsFromGroup(Arrays.asList(mGroupMemberAdapter.getMembersIds())));
        }
        GroupManageDialog dialog = new GroupManageDialog(mGroup, mCurrentClientsInGroup, mContactsToInviteToGroup, mCloneGroupContact);
        dialog.setTargetFragment(this, 0);
        dialog.show(getActivity().getSupportFragmentManager(), "GroupManageDialog");
    }

    private boolean isCurrentGroup(TalkClientContact contact) {
        return mGroup != null && mGroup == contact || mGroup.getClientContactId() == contact.getClientContactId();
    }

    private List<TalkClientContact> getCurrentContactsFromGroup(List<String> ids) {
        List<TalkClientContact> result = new ArrayList<TalkClientContact>();
        try {
            List<TalkClientContact> allContacts = getXoDatabase().findAllClientContacts();
            for (TalkClientContact contact : allContacts) {
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
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.profile_group_profile_image:
                enterAvatarEditMode();
                break;
        }
    }

    private void enterAvatarEditMode() {
        if (mGroup != null && mGroup.isEditable()) {
            if (mGroup.getAvatarContentUrl() != null) {
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
    public void onAvatarSelected(IContentObject contentObject) {
        mAvatarToSet = contentObject;
    }

    @Override
    public void onServiceConnected() {
        LOG.debug("onServiceConnected()");
        if (mAvatarToSet != null) {
            updateAvatar(mAvatarToSet);
            mAvatarToSet = null;
        }
    }

    private void updateAvatar(final IContentObject avatar) {
        if (avatar != null) {
            XoApplication.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    LOG.debug("creating avatar upload");
                    TalkClientUpload upload = SelectedContent.createAvatarUpload(avatar);
                    try {
                        getXoDatabase().saveClientUpload(upload);
                        getXoClient().setGroupAvatar(mGroup, upload);
                    } catch (SQLException e) {
                        LOG.error("sql error", e);
                    }
                }
            });
        } else {
            getXoClient().setGroupAvatar(mGroup, null);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        TalkClientContact contact = (TalkClientContact) adapterView.getItemAtPosition(i);
        getXoActivity().showContactConversation(contact);
    }

    @Override
    public void onContactAdded(TalkClientContact contact) {
        if (isCurrentGroup(contact)) {
            openGroupProfileFragment();
        }
    }

    private void openGroupProfileFragment() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    IProfileFragmentManager profileFragmentManager = (IProfileFragmentManager) getActivity();
                    profileFragmentManager.showGroupProfileFragment(mGroup.getClientContactId(), true, false);
                } catch (ClassCastException e) {
                    LOG.error("Activity does not implement interface IProfileFragmentManager ", e);
                }
            }
        });
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

    }
}
