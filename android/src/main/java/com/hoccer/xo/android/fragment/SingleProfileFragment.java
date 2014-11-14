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
import com.hoccer.talk.client.IXoMessageListener;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.talk.model.TalkPresence;
import com.hoccer.talk.model.TalkRelationship;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.activity.MediaBrowserActivity;
import com.hoccer.xo.android.content.SelectedContent;
import com.hoccer.xo.android.util.IntentHelper;
import com.artcom.hoccer.R;
import com.squareup.picasso.Picasso;
import org.apache.log4j.Logger;

import java.io.File;
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

    private IContentObject mAvatarToSet;

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

        if (mContact != null) {
            showProfile();
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);

        menuInflater.inflate(R.menu.fragment_single_profile, menu);

        if (mContact != null) {
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

    private void showProfile() {
        if (mContact != null) {
            LOG.debug("showProfile(" + mContact.getClientContactId() + ")");
        }
        refreshContact(mContact);
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

    @Override
    protected void updateView() {
        updateAvatar();
        updateName();
        updateChatContainer();
        updateFingerprint();
        updateInviteButton(mContact);
        updateDeclineButton(mContact);
        hideNicknameEdit();
        updateNicknameContainer();
    }

    private void updateChatContainer() {
        if (mContact.isSelf()) {
            mChatContainer.setVisibility(View.GONE);
        } else {
            updateMessageText();
            mChatContainer.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected int getClientContactId() {
        return mContact.getClientContactId();
    }

    @Override
    protected void updateMessageText() {
        try {
            int count = (int) XoApplication.getXoClient().getDatabase().getMessageCountByContactId(mContact.getClientContactId());
            super.updateMessageText(count);
        } catch (SQLException e) {
            LOG.error("Error fetching message count from database.");
        }
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

    private void updateAvatar() {
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
    }

    private void updateNickname(TalkClientContact contact) {
        mNicknameEditText.setText(contact.getNickname());
        mNicknameTextView.setText(contact.getNickname());
    }

    private void updateFingerprint() {
        mKeyText.setText(getFingerprint());
    }

    public String getFingerprint() {
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
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LOG.debug("updating ui");
                updateView();
            }
        });
    }

    private boolean isMyContact(TalkClientContact contact) {
        return mContact != null && mContact == contact || mContact.getClientContactId() == contact
                .getClientContactId();
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
        refreshContact(mContact);
        updateView();
    }
}
