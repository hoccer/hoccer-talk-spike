package com.hoccer.xo.android.fragment;

import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.client.predicates.TalkClientContactPredicates;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.talk.model.TalkGroupMembership;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.activity.GroupProfileActivity;
import com.hoccer.xo.android.adapter.ContactsAdapter;
import com.hoccer.xo.android.adapter.GroupContactsAdapter;
import com.hoccer.xo.android.base.XoFragment;
import com.hoccer.xo.android.content.SelectedContent;
import com.hoccer.xo.android.util.UriUtils;
import com.squareup.picasso.Picasso;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fragment for display and editing of group profiles.
 */
public class GroupProfileCreationFragment extends XoFragment implements IXoContactListener {

    private static final Logger LOG = Logger.getLogger(GroupProfileCreationFragment.class);
    public static final String ARG_CLONE_GROUP_ID = "ARG_CLONE_GROUP_ID";

    private EditText mGroupNameEdit;
    private Button mGroupCreateButton;
    private TextView mGroupMembersTitle;
    private ListView mGroupMembersList;
    private ContactsAdapter mGroupMemberAdapter;

    private IContentObject mAvatar;
    private ImageView mAvatarImageView;

    private List<TalkClientContact> mContactsToInvite = Collections.emptyList();
    private String mGroupTag;
    private boolean mIsGroupCreated;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle arguments = getArguments();

        if (arguments != null) {
            String groupId = arguments.getString(ARG_CLONE_GROUP_ID);

            if (groupId != null) {
                try {
                    mContactsToInvite = getXoDatabase().findContactsInGroupByState(groupId, TalkGroupMembership.STATE_JOINED);
                    CollectionUtils.filterInverse(mContactsToInvite, TalkClientContactPredicates.IS_SELF_PREDICATE);
                } catch (SQLException e) {
                    LOG.error("Error finding contacts in group", e);
                }
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_group_profile, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);

        mAvatarImageView = (ImageView) view.findViewById(R.id.profile_group_profile_image);
        mGroupNameEdit = (EditText) view.findViewById(R.id.profile_group_name_edit);
        mGroupMembersTitle = (TextView) view.findViewById(R.id.profile_group_members_title);
        mGroupMembersList = (ListView) view.findViewById(R.id.profile_group_members_list);
        mGroupCreateButton = (Button) view.findViewById(R.id.profile_group_button_create);

        mAvatarImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectAvatar();
            }
        });

        mGroupCreateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createGroup();
            }
        });

        mGroupNameEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    createGroup();
                    return true;
                }

                return false;
            }
        });

        mGroupNameEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                if (charSequence.length() == 0) {
                    mGroupCreateButton.setEnabled(false);
                } else {
                    mGroupCreateButton.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        mGroupMemberAdapter = new GroupContactsAdapter(getXoActivity());
        mGroupMemberAdapter.setFilter(new ContactsAdapter.Filter() {
            @Override
            public boolean shouldShow(TalkClientContact contact) {
                return mContactsToInvite.contains(contact);
            }
        });
        mGroupMemberAdapter.onCreate();
        mGroupMembersList.setAdapter(mGroupMemberAdapter);

        if (mContactsToInvite.isEmpty()) {
            mGroupMembersTitle.setVisibility(View.GONE);
            mGroupMembersList.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        mGroupMemberAdapter.onResume();
        mGroupMemberAdapter.requestReload();
        getXoClient().registerContactListener(this);

        mGroupNameEdit.requestFocus();
    }

    public void onPause() {
        super.onPause();

        getXoClient().unregisterContactListener(this);
        mGroupMemberAdapter.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mGroupMemberAdapter.onDestroy();
    }

    private void createGroup() {
        mGroupNameEdit.setEnabled(false);
        mGroupCreateButton.setEnabled(false);
        String groupName = mGroupNameEdit.getText().toString();
        createGroup(groupName);
    }

    private void createGroup(String groupName) {
        List<String> memberIds = getClientIdsForContacts(mContactsToInvite);
        mGroupTag = getXoClient().createGroupWithContacts(groupName, memberIds);
    }

    public static List<String> getClientIdsForContacts(List<TalkClientContact> contacts) {
        List<String> ids = new ArrayList<String>(contacts.size());
        for (TalkClientContact contact : contacts) {
            ids.add(contact.getClientId());
        }
        return ids;
    }

    private void selectAvatar() {
        if (mAvatar != null) {
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
                                    updateAvatarView(null);
                                }
                            }
                        }
                    });
        } else {
            getXoActivity().selectAvatar();
        }
    }

    @Override
    public void onAvatarSelected(IContentObject contentObject) {
        updateAvatarView(contentObject);
    }

    private void updateAvatarView(final IContentObject avatar) {
        mAvatar = avatar;
        Uri avatarUri = mAvatar == null ? null : UriUtils.getAbsoluteFileUri(mAvatar.getFilePath());
        Picasso.with(getActivity())
                .load(avatarUri)
                .placeholder(R.drawable.avatar_default_group_large)
                .error(R.drawable.avatar_default_group_large)
                .into(mAvatarImageView);
    }

    @Override
    public void onClientPresenceChanged(TalkClientContact contact) {}

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {}

    @Override
    public void onGroupPresenceChanged(TalkClientContact contact) {
        if (!mIsGroupCreated && mGroupTag != null && mGroupTag.equals(contact.getGroupTag())) {
            mIsGroupCreated = true;
            uploadAvatar(contact);
            openGroupProfileFragment(contact.getClientContactId());
        }
    }

    private void uploadAvatar(TalkClientContact group) {
        if (mAvatar != null) {
            TalkClientUpload upload = SelectedContent.createAvatarUpload(mAvatar);

            try {
                getXoDatabase().saveClientUpload(upload);
                getXoClient().setGroupAvatar(group, upload);
            } catch (SQLException e) {
                LOG.error("Error saving group avatar upload", e);
            }
        }
    }

    private void openGroupProfileFragment(final int groupContactId) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    GroupProfileActivity groupProfileActivity = (GroupProfileActivity) getActivity();
                    groupProfileActivity.showGroupProfileFragment(groupContactId, true);
                } catch (ClassCastException e) {
                    LOG.error("Activity does not implement interface IProfileFragmentManager ", e);
                }
            }
        });
    }

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) {}
}
