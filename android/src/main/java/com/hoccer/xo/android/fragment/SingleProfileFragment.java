package com.hoccer.xo.android.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.talk.model.TalkPresence;
import com.hoccer.talk.model.TalkRelationship;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.activity.MediaBrowserActivity;
import com.hoccer.xo.android.base.XoFragment;
import com.hoccer.xo.android.content.SelectedContent;
import com.hoccer.xo.android.util.IntentHelper;
import com.hoccer.xo.release.R;
import com.squareup.picasso.Picasso;
import org.apache.log4j.Logger;

import java.io.File;
import java.sql.SQLException;

/**
 * Fragment for display and editing of single-contact profiles.
 */
public class SingleProfileFragment extends XoFragment
        implements View.OnClickListener, IXoContactListener, ActionMode.Callback {

    public enum Mode {
        PROFILE,
        CREATE_SELF
    }

    public static final String ARG_CREATE_SELF = "ARG_CREATE_SELF";

    public static final String ARG_CLIENT_CONTACT_ID = "ARG_CLIENT_CONTACT_ID";
    private static final Logger LOG = Logger.getLogger(SingleProfileFragment.class);

    private Mode mMode;

    private ActionMode mActionMode;

    private TextView mNameText;

    private RelativeLayout mKeyContainer;

    private TextView mKeyText;

    private ImageView mAvatarImage;

    private IContentObject mAvatarToSet;

    private TalkClientContact mContact;

    private EditText mEditName;

    private ImageButton mNicknameEditButton;

    private TextView mNicknameTextView;

    private EditText mNicknameEditText;

    private LinearLayout mInviteButtonContainer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_single_profile, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAvatarImage = (ImageView) view.findViewById(R.id.profile_avatar_image);
        mNameText = (TextView) view.findViewById(R.id.tv_profile_name);
        mKeyContainer = (RelativeLayout) view.findViewById(R.id.inc_profile_key);
        mKeyText = (TextView) view.findViewById(R.id.tv_profile_key);
        mEditName = (EditText) view.findViewById(R.id.et_profile_name);
        mNicknameEditButton = (ImageButton) view.findViewById(R.id.ib_profile_nickname_edit);
        mNicknameTextView = (TextView) view.findViewById(R.id.tv_profile_nickname);
        mNicknameEditText = (EditText) view.findViewById(R.id.et_profile_nickname);
        mInviteButtonContainer = (LinearLayout) view.findViewById(R.id.inc_profile_request);

        if (getArguments() != null) {
            if (getArguments().getBoolean(ARG_CREATE_SELF)) {
                createSelf();
            } else {
                int clientContactId = getArguments().getInt(ARG_CLIENT_CONTACT_ID);
                try {
                    mContact = XoApplication.getXoClient().getDatabase().findClientContactById(clientContactId);
                    showProfile();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        } else {
            LOG.error("Creating SingleProfileFragment without arguments is not supported.");
        }
    }

    private void showNicknameEdit() {
        mNicknameTextView.setVisibility(View.INVISIBLE);
        mNicknameEditText.setVisibility(View.VISIBLE);
        mNicknameEditText.requestFocus();
        mNicknameEditText.setSelection(mNicknameEditText.getText().length());
        mNicknameEditButton.setImageResource(R.drawable.ic_light_navigation_accept);
        mNicknameEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String nickname = mNicknameEditText.getText().toString();
                mContact.setNickname(nickname);
                try {
                    getXoDatabase().saveContact(mContact);
                } catch (SQLException e) {
                    LOG.error("error while saving nickname to contact " + mContact.getClientId(), e);
                }
                updateNickname(mContact);
                hideNicknameEdit();
                toggleSoftKeyboard();

                updateActionBar();
            }
        });
    }

    private void hideNicknameEdit() {
        mNicknameEditText.setVisibility(View.GONE);
        mNicknameTextView.setVisibility(View.VISIBLE);
        mNicknameEditButton.setImageResource(android.R.drawable.ic_menu_edit);
        mNicknameEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showNicknameEdit();
                toggleSoftKeyboard();
            }
        });
    }

    private void toggleSoftKeyboard() {
        InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }

    private void updateInviteButton(final TalkClientContact contact) {
        Button inviteButton = (Button) getView().findViewById(R.id.btn_profile_invite);
        if (contact == null || contact.isSelf()) {
            mInviteButtonContainer.setVisibility(View.GONE);
            return;
        } else {
            mInviteButtonContainer.setVisibility(View.VISIBLE);
        }

        try {
            getXoDatabase().refreshClientContact(contact);
        } catch (SQLException e) {
            LOG.error("Error while refreshing client contact: " + contact.getClientId(), e);
        }

        if (contact.getClientRelationship() == null || (contact.getClientRelationship().getState() != null && contact.getClientRelationship().getState().equals(TalkRelationship.STATE_NONE))) {
            inviteButton.setText(R.string.friend_request_add_as_friend);
            inviteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getXoActivity().getXoClient().inviteFriend(contact);
                }
            });
        } else if (contact.getClientRelationship().getState() != null && contact.getClientRelationship().getState().equals(TalkRelationship.STATE_INVITED)) {
            inviteButton.setText(R.string.friend_request_cancel_invitation);
            inviteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getXoActivity().getXoClient().disinviteFriend(contact);
                }
            });
        } else if (contact.getClientRelationship().getState() != null && contact.getClientRelationship().getState().equals(TalkRelationship.STATE_INVITED_ME)) {
            inviteButton.setText(R.string.friend_request_accept_invitation);
            inviteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getXoActivity().getXoClient().acceptFriend(contact);
                }
            });
        } else {
            mInviteButtonContainer.setVisibility(View.GONE);
        }
    }

    private void updateDeclineButton(final TalkClientContact contact) {

        Button declineButton = (Button) getView().findViewById(R.id.btn_profile_decline);
        if (contact == null || contact.isSelf()) {
            declineButton.setVisibility(View.GONE);
            return;
        } else {
            declineButton.setVisibility(View.VISIBLE);
        }

        try {
            getXoDatabase().refreshClientContact(contact);
        } catch (SQLException e) {
            LOG.error("Error while refreshing client contact: " + contact.getClientId(), e);
        }

        if (contact.getClientRelationship() != null &&
                contact.getClientRelationship().getState() != null &&
                contact.getClientRelationship().getState().equals(TalkRelationship.STATE_INVITED_ME)) {
            declineButton.setText(R.string.friend_request_decline_invitation);
            declineButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getXoActivity().getXoClient().declineFriend(contact);
                }
            });
        } else {
            declineButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        LOG.debug("onResume()");
        super.onResume();
        getXoClient().registerContactListener(this);
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
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);

        menuInflater.inflate(R.menu.fragment_single_profile, menu);

        if (mContact != null) {
            boolean isSelf = mMode == Mode.CREATE_SELF || mContact.isSelf();

            menu.findItem(R.id.menu_my_profile).setVisible(!isSelf);
            if (mContact.isSelf()) {
                menu.findItem(R.id.menu_profile_edit).setVisible(true);
                menu.findItem(R.id.menu_profile_block).setVisible(false);
                menu.findItem(R.id.menu_profile_unblock).setVisible(false);
                menu.findItem(R.id.menu_profile_delete).setVisible(false);
            } else {
                if (mContact.isNearby()) {
                    menu.findItem(R.id.menu_profile_edit).setVisible(false);
                    menu.findItem(R.id.menu_profile_delete).setVisible(false);
                    menu.findItem(R.id.menu_profile_block).setVisible(false);
                    menu.findItem(R.id.menu_profile_unblock).setVisible(false);
                } else {
                    TalkRelationship relationship = mContact.getClientRelationship();
                    if (relationship == null || relationship.isBlocked()) { // todo != null correct
                        menu.findItem(R.id.menu_profile_block).setVisible(false);
                        menu.findItem(R.id.menu_profile_unblock).setVisible(true);
                        menu.findItem(R.id.menu_audio_attachment_list).setVisible(true);
                    } else {
                        menu.findItem(R.id.menu_profile_block).setVisible(true);
                        menu.findItem(R.id.menu_profile_unblock).setVisible(false);
                        menu.findItem(R.id.menu_audio_attachment_list).setVisible(true);
                    }
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean isSelectionHandled;
        switch (item.getItemId()) {
            case R.id.menu_profile_block:
                blockContact();
                isSelectionHandled = true;
                break;
            case R.id.menu_profile_unblock:
                unblockContact();
                isSelectionHandled = true;
                break;
            case R.id.menu_profile_delete:
                if (mContact != null) {
                    XoDialogs.showYesNoDialog("ContactDeleteDialog",
                            R.string.dialog_delete_contact_title,
                            R.string.dialog_delete_contact_message,
                            getActivity(),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    getXoActivity().getXoClient().deleteContact(mContact);
                                    getActivity().finish();
                                }
                            },
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {

                                }
                            }
                    );
                }

                isSelectionHandled = true;
                break;
            case R.id.menu_profile_edit:
                getActivity().startActionMode(this);
                isSelectionHandled = true;
                break;
            case R.id.menu_audio_attachment_list:
                Intent intent = new Intent(getActivity(), MediaBrowserActivity.class);
                intent.putExtra(IntentHelper.EXTRA_CONTACT_ID, mContact.getClientContactId());
                startActivity(intent);
                isSelectionHandled = true;
                break;
            default:
                isSelectionHandled = super.onOptionsItemSelected(item);
        }
        return isSelectionHandled;
    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.profile_avatar_image) {
            if (mContact != null && mContact.isEditable()) {
                if (mContact.getAvatarContentUrl() != null) {
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
                            }
                    );
                } else {
                    getXoActivity().selectAvatar();
                }
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
                        if (mContact.isSelf()) {
                            getXoClient().setClientAvatar(upload);
                        }
                    } catch (SQLException e) {
                        LOG.error("sql error", e);
                    }
                }
            });
        } else {
            getXoClient().setClientAvatar(null);
        }
    }

    public TalkClientContact getContact() {
        return mContact;
    }

    private void showProfile() {
        if (mContact != null) {
            LOG.debug("showProfile(" + mContact.getClientContactId() + ")");
        }
        mMode = Mode.PROFILE;
        refreshContact(mContact);
    }

    private void createSelf() {
        LOG.debug("createSelf()");
        mMode = Mode.CREATE_SELF;
        mKeyContainer.setVisibility(View.GONE);
        mContact = getXoClient().getSelfContact();
        if (mActionMode == null) {
            mActionMode = getActivity().startActionMode(this);
        }
        finishActivityIfContactDeleted();

        update();
        updateActionBar();
    }

    public void updateActionBar() {
        LOG.debug("update(" + mContact.getClientContactId() + ")");
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getActivity().getActionBar().setTitle(mContact.getNickname());
                if (mMode == Mode.CREATE_SELF) {
                    getActivity().getActionBar().setTitle(R.string.welcome_to_title);
                } else {
                    if (mContact.isSelf()) {
                        getActivity().getActionBar().setTitle(R.string.my_profile_title);
                    }
                }
            }
        });
    }

    public void finishActivityIfContactDeleted() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mContact.isDeleted()) {
                    getActivity().finish();
                }
            }
        });
    }

    private void update() {
        LOG.debug("update(" + mContact.getClientContactId() + ")");

        String avatarUrl = null;
        if (mContact.isSelf()) {
            TalkClientUpload avatarUpload = mContact.getAvatarUpload();
            if (avatarUpload != null && avatarUpload.isContentAvailable()) {
                avatarUrl = avatarUpload.getContentDataUrl();
            }
        } else {
            TalkClientDownload avatarDownload = mContact.getAvatarDownload();
            if (avatarDownload != null && avatarDownload.isContentAvailable()) {
                if (avatarDownload.getDataFile() != null) {
                    Uri uri = Uri.fromFile(new File(avatarDownload.getDataFile()));
                    avatarUrl = uri.toString();
                }
            }
        }

        Picasso.with(getActivity())
                .load(avatarUrl)
                .placeholder(R.drawable.avatar_default_contact_large)
                .error(R.drawable.avatar_default_contact_large)
                .into(mAvatarImage);

        // apply data from the contact that needs to recurse
        String name = getResources().getString(R.string.profile_user_name_unknown);
        if (mContact.isClient() || mContact.isSelf()) {
            if (mContact.isSelf() && !mContact.isSelfRegistered()) {
                name = mContact.getSelf().getRegistrationName();
            } else {
                TalkPresence presence = mContact.getClientPresence();
                if (presence != null) {
                    name = presence.getClientName();
                }
            }
        }
        mNameText.setText(name);
        mKeyText.setText(getFingerprint());

        updateInviteButton(mContact);
        updateDeclineButton(mContact);
        hideNicknameEdit();
        View nickNameContainer = getView().findViewById(R.id.inc_profile_nickname);
        if (this.mContact.isSelf()) {
            nickNameContainer.setVisibility(View.GONE);
        } else {
            updateNickname(mContact);
            nickNameContainer.setVisibility(View.VISIBLE);
        }

    }

    private void updateNickname(TalkClientContact contact) {
        mNicknameEditText.setText(contact.getNickname());
        mNicknameTextView.setText(contact.getNickname());
    }

    public String getFingerprint() {
        String keyId = "";
        if (mMode != Mode.CREATE_SELF) {
            keyId = mContact.getPublicKey().getKeyId();
        } else {
            return "";
        }
        keyId = keyId.toUpperCase();

        char[] chars = keyId.toCharArray();
        int length = chars.length;
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            builder.append(chars[i]);
            if ((i % 2) == 1) {
                builder.append(":");
            }

        }
        builder.deleteCharAt(builder.lastIndexOf(":"));
        return builder.toString();
    }

    private void blockContact() {
        LOG.debug("blockContact()");
        XoDialogs.showYesNoDialog("BlockContactDialog",
                R.string.dialog_block_user_title,
                R.string.dialog_block_user_message,
                getActivity(),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        if (mContact != null) {
                            getXoClient().blockContact(mContact);
                        }
                    }
                },
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                    }
                }
        );
    }

    private void unblockContact() {
        LOG.debug("unblockContact()");
        if (mContact != null) {
            getXoClient().unblockContact(mContact);
            getActivity().finish();
        }
    }

    private void refreshContact(TalkClientContact newContact) {
        LOG.debug("refreshContact()");
        if (mMode == Mode.PROFILE) {
            mContact = newContact;
            try {
                XoClientDatabase database = XoApplication.getXoClient().getDatabase();
                database.refreshClientContact(mContact);
                if (mContact.getAvatarDownload() != null) {
                    database.refreshClientDownload(mContact.getAvatarDownload());
                }
                if (mContact.getAvatarUpload() != null) {
                    database.refreshClientUpload(mContact.getAvatarUpload());
                }
            } catch (SQLException e) {
                LOG.error("SQL error", e);
            }
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LOG.debug("updating ui");
                update();
            }
        });
    }

    private boolean isMyContact(TalkClientContact contact) {
        return mContact != null && mContact == contact || mContact.getClientContactId() == contact
                .getClientContactId();
    }

    @Override
    public void onContactAdded(TalkClientContact contact) {
        // we don't care
    }

    @Override
    public void onContactRemoved(TalkClientContact contact) {
        if (isMyContact(contact)) {
            getActivity().finish();
        }
    }

    @Override
    public void onClientPresenceChanged(final TalkClientContact contact) {
        if (isMyContact(contact)) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    refreshContact(contact);
                    getActivity().invalidateOptionsMenu();
                    updateActionBar();
                    finishActivityIfContactDeleted();
                }
            });
        }
    }

    @Override
    public void onClientRelationshipChanged(final TalkClientContact contact) {
        if (isMyContact(contact)) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    refreshContact(contact);
                    getActivity().invalidateOptionsMenu();
                    updateActionBar();
                    finishActivityIfContactDeleted();
                }
            });
        }
    }

    @Override
    public void onGroupPresenceChanged(TalkClientContact contact) {
    }

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) {
    }

    // Actionmode Callbacks
    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        mEditName.setVisibility(View.VISIBLE);
        mNameText.setVisibility(View.INVISIBLE);
        mEditName.setText(mNameText.getText());
        mAvatarImage.setOnClickListener(this);
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {

        mActionMode = null;

        String nameString = mEditName.getText().toString();
        String newUserName = nameString.isEmpty() ? getResources().getString(R.string.profile_self_initial_name) : nameString;

        mNameText.setText(newUserName);
        mEditName.setVisibility(View.GONE);
        mNameText.setVisibility(View.VISIBLE);
        mAvatarImage.setOnClickListener(null);

        if (mMode == Mode.CREATE_SELF) {
            mContact.getSelf().setRegistrationName(newUserName);
            mContact.updateSelfConfirmed();
            getXoClient().register();
            getXoClient().setClientString(newUserName, "happy");
            getActivity().finish();
        } else {
            getXoClient().setClientString(newUserName, "happier");
            refreshContact(mContact);
            update();
        }
    }
}
