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
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.XoTransfer;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.content.SelectedContent;
import com.hoccer.talk.model.TalkPresence;
import com.hoccer.talk.model.TalkRelationship;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.activity.MediaBrowserActivity;
import com.hoccer.xo.android.util.IntentHelper;
import com.hoccer.xo.android.util.UriUtils;
import com.squareup.picasso.Picasso;
import org.apache.log4j.Logger;

import java.sql.SQLException;

/**
 * Fragment for display and editing of single-contact profiles.
 */
public class SingleProfileFragment extends ProfileFragment
        implements View.OnClickListener, ActionMode.Callback {

    private static final Logger LOG = Logger.getLogger(SingleProfileFragment.class);

    private TextView mNameText;
    private EditText mNameEditText;
    private ImageView mAvatarImage;
    private TextView mKeyText;
    private TextView mNicknameTextView;
    private EditText mNicknameEditText;
    private ImageButton mNicknameEditButton;
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
        mKeyText = (TextView) view.findViewById(R.id.tv_profile_key);
        mNameEditText = (EditText) view.findViewById(R.id.et_profile_name);
        mNicknameEditButton = (ImageButton) view.findViewById(R.id.ib_profile_nickname_edit);
        mNicknameTextView = (TextView) view.findViewById(R.id.tv_profile_nickname);
        mNicknameEditText = (EditText) view.findViewById(R.id.et_profile_nickname);
        mInviteButtonContainer = (LinearLayout) view.findViewById(R.id.inc_profile_request);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshContact();
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

    private void toggleSoftKeyboard() {
        InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);

        menuInflater.inflate(R.menu.fragment_single_profile, menu);

        boolean isSelf = mContact.isSelf();

        menu.findItem(R.id.menu_my_profile).setVisible(!isSelf);
        if (mContact.isSelf()) {
            menu.findItem(R.id.menu_profile_edit).setVisible(true);
            menu.findItem(R.id.menu_profile_block).setVisible(false);
            menu.findItem(R.id.menu_profile_unblock).setVisible(false);
            menu.findItem(R.id.menu_profile_delete).setVisible(false);
        } else {
            TalkRelationship relationship = mContact.getClientRelationship();
            if ((relationship == null || relationship.isInvited() || relationship.invitedMe() || relationship.isNone()) && mContact.isNearby()) {
                menu.findItem(R.id.menu_profile_edit).setVisible(false);
                menu.findItem(R.id.menu_profile_delete).setVisible(false);
                menu.findItem(R.id.menu_profile_block).setVisible(false);
                menu.findItem(R.id.menu_profile_unblock).setVisible(false);
            } else {
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
                            }
                    );
                } else {
                    getXoActivity().selectAvatar();
                }
            }
        }
    }

    @Override
    public void onAvatarSelected(SelectedContent avatar) {
        LOG.debug("onAvatarSelected(" + avatar.getFilePath() + ")");
        updateAvatar(avatar);
    }

    @Override
    protected int getClientContactId() {
        return mContact.getClientContactId();
    }

    @Override
    protected void updateMessageText() {
        try {
            int count = (int) XoApplication.get().getXoClient().getDatabase().getMessageCountByContactId(mContact.getClientContactId());
            super.updateMessageText(count);
        } catch (SQLException e) {
            LOG.error("Error fetching message count from database.");
        }
    }

    private void updateAvatar(final SelectedContent avatar) {
        if (avatar != null) {
            XoApplication.get().getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    LOG.debug("creating avatar upload");
                    TalkClientUpload upload = new TalkClientUpload();
                    upload.initializeAsAvatar(avatar);
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

    public void updateActionBar() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getActivity().getActionBar().setTitle(mContact.getNickname());
                if (mContact.isSelf()) {
                    getActivity().getActionBar().setTitle(R.string.my_profile_title);
                }
            }
        });
    }

    @Override
    protected void updateView() {
        updateAvatarView();
        updateName();
        updateChatContainer();
        updateFingerprint();
        updateInviteButton(mContact);
        updateDeclineButton(mContact);
        hideNicknameEdit();
        updateNicknameContainer();
    }

    private void updateAvatarView() {
        XoTransfer avatarTransfer;
        if (mContact.isSelf()) {
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
                .placeholder(R.drawable.avatar_default_contact_large)
                .error(R.drawable.avatar_default_contact_large)
                .into(mAvatarImage);
    }

    private void updateName() {
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
    }

    private void updateChatContainer() {
        if (mContact.isSelf()) {
            mChatContainer.setVisibility(View.GONE);
        } else {
            updateMessageText();
            mChatContainer.setVisibility(View.VISIBLE);
        }
    }

    private void updateFingerprint() {
        String keyId = mContact.getPublicKey().getKeyId();

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

        mKeyText.setText(builder.toString());
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

    private void updateNicknameContainer() {
        View nickNameContainer = getView().findViewById(R.id.inc_profile_nickname);
        if (mContact.isSelf()) {
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

    private void blockContact() {
        LOG.debug("blockContact()");
        XoDialogs.showYesNoDialog("BlockContactDialog",
                R.string.dialog_block_user_title,
                R.string.dialog_block_user_message,
                getActivity(),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        getXoClient().blockContact(mContact);
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
        getXoClient().unblockContact(mContact);
        getActivity().finish();
    }

    private void refreshContact() {
        LOG.debug("refreshContact()");

        try {
            XoClientDatabase database = XoApplication.get().getXoClient().getDatabase();
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

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateView();
                updateActionBar();
            }
        });
    }

    private boolean isCurrentContact(TalkClientContact contact) {
        return mContact == contact || mContact.getClientContactId() == contact.getClientContactId();
    }

    @Override
    public void onClientPresenceChanged(final TalkClientContact contact) {
        if (isCurrentContact(contact)) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    refreshContact();
                    getActivity().invalidateOptionsMenu();
                    updateActionBar();
                }
            });
        }
    }

    @Override
    public void onClientRelationshipChanged(final TalkClientContact contact) {
        if (isCurrentContact(contact)) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    refreshContact();
                    getActivity().invalidateOptionsMenu();
                    updateActionBar();
                }
            });
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        mNameEditText.setVisibility(View.VISIBLE);
        mNameText.setVisibility(View.INVISIBLE);
        mNameEditText.setText(mNameText.getText());
        mAvatarImage.setOnClickListener(this);
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {

        String nameString = mNameEditText.getText().toString();
        String newUserName = nameString.isEmpty() ? getResources().getString(R.string.profile_self_initial_name) : nameString;

        mNameText.setText(newUserName);
        mNameEditText.setVisibility(View.GONE);
        mNameText.setVisibility(View.VISIBLE);
        mAvatarImage.setOnClickListener(null);

        getXoClient().setClientString(newUserName, "happier");
        refreshContact();
        updateView();
    }
}
