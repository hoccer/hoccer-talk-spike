package com.hoccer.xo.android.profile.client;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.IXoMessageListener;
import com.hoccer.talk.client.XoTransfer;
import com.hoccer.talk.client.exceptions.NoClientIdInPresenceException;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.model.TalkPresence;
import com.hoccer.talk.model.TalkRelationship;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.activity.ChatActivity;
import com.hoccer.xo.android.activity.MediaBrowserActivity;
import com.hoccer.xo.android.util.IntentHelper;
import org.apache.log4j.Logger;

import java.sql.SQLException;

public class ContactClientProfileFragment extends ClientProfileFragment implements IXoMessageListener, IXoContactListener {

    private static final Logger LOG = Logger.getLogger(ContactClientProfileFragment.class);

    private RelativeLayout mBlockedContainer;
    private LinearLayout mInviteButtonContainer;
    private TextView mNicknameTextView;
    private EditText mNicknameEditText;
    private ImageButton mNicknameEditButton;
    private RelativeLayout mChatContainer;
    private TextView mChatMessagesText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contact_client_profile, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mBlockedContainer = (RelativeLayout) view.findViewById(R.id.rl_blocked);
        mChatContainer = (RelativeLayout) view.findViewById(R.id.inc_profile_chat_stats);
        mChatMessagesText = (TextView) view.findViewById(R.id.tv_messages_text);
        mNicknameTextView = (TextView) view.findViewById(R.id.tv_profile_nickname);
        mNicknameEditText = (EditText) view.findViewById(R.id.et_profile_nickname);
        mNicknameEditButton = (ImageButton) view.findViewById(R.id.ib_profile_nickname_edit);
        mInviteButtonContainer = (LinearLayout) view.findViewById(R.id.inc_profile_friend_request);

        RelativeLayout chatMessagesContainer = (RelativeLayout) view.findViewById(R.id.rl_profile_messages);
        chatMessagesContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMessagingActivity();
            }
        });

        updateContent();
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
    public void updateActionBar() {
        getActivity().getActionBar().setTitle(mContact.getNickname());
    }

    @Override
    protected void updateAvatarView(XoTransfer avatarTransfer) {
        super.updateAvatarView(avatarTransfer);
    }

    @Override
    public void onResume() {
        super.onResume();

        getXoClient().registerContactListener(this);
        getXoClient().registerMessageListener(this);

        updateMessageText();
    }

    @Override
    public void onPause() {
        super.onPause();

        getXoClient().unregisterContactListener(this);
        getXoClient().unregisterMessageListener(this);
    }

    private void updateMessageTextOnUiThread() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateMessageText();
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.fragment_contact_client_profile, menu);
        updateMenuItems(menu);
    }

    private void updateMenuItems(Menu menu) {
        menu.findItem(R.id.menu_my_profile).setVisible(true);
        TalkRelationship relationship = mContact.getClientRelationship();
        if ((relationship == null || !relationship.isFriend()) && mContact.isInEnvironment()) {
            menu.findItem(R.id.menu_profile_delete).setVisible(false);
            menu.findItem(R.id.menu_profile_block).setVisible(false);
            menu.findItem(R.id.menu_profile_unblock).setVisible(false);
        } else if (!mContact.isFriendOrBlocked()) {
            menu.findItem(R.id.menu_profile_delete).setVisible(true);
            menu.findItem(R.id.menu_profile_block).setVisible(false);
            menu.findItem(R.id.menu_profile_unblock).setVisible(false);
        }

        if (relationship != null && relationship.isBlocked()) {
            menu.findItem(R.id.menu_profile_block).setVisible(false);
            menu.findItem(R.id.menu_profile_unblock).setVisible(true);
        } else {
            menu.findItem(R.id.menu_profile_block).setVisible(true);
            menu.findItem(R.id.menu_profile_unblock).setVisible(false);
        }
        menu.findItem(R.id.menu_audio_attachment_list).setVisible(true);
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
                if (mContact.isKept()) {
                    showDiscardContactDialog();
                } else {
                    showDeleteContactDialog();
                }
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

    private void showDeleteContactDialog() {
        XoDialogs.showYesNoDialog("ContactDeleteDialog",
                R.string.dialog_delete_contact_title,
                R.string.dialog_delete_contact_message,
                getActivity(),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        deleteContact();
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

    private void showDiscardContactDialog() {
        XoDialogs.showYesNoDialog("RemoveContactFromListDialog",
                R.string.dialog_discard_contact_title,
                R.string.dialog_discard_contact_message,
                getActivity(),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        declineAndDiscardContact();
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

    private void deleteContact() {
        getXoClient().deleteContact(mContact);
    }

    private void declineAndDiscardContact() {
        if (mContact.getClientRelationship() != null && mContact.getClientRelationship().invitedMe()) {
            getXoClient().declineFriend(mContact);
        } else if (mContact.getClientRelationship() != null && mContact.getClientRelationship().isInvited()) {
            getXoClient().disinviteFriend(mContact);
        }
        if (mContact.isClientBlocked() && mContact.isKept()) {
            getXoClient().deleteContact(mContact);
        }
        discardContact();
    }

    private void discardContact() {
        mContact.getClientPresence().setKept(false);
        try {
            getXoClient().getDatabase().savePresence(mContact.getClientPresence());
        } catch (SQLException e) {
            LOG.error("sql error", e);
        } catch (NoClientIdInPresenceException e) {
            LOG.error(e.getMessage(), e);
        }
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
        return (mContact.isClient() && mContact.getClientRelationship() != null && mContact.isFriendOrBlocked()) || mContact.isKept() && count > 0 || mContact.isNearby();
    }

    @Override
    protected XoTransfer getAvatarTransfer() {
        return mContact.getAvatarDownload();
    }

    @Override
    public void onMessageCreated(TalkClientMessage message) {
        updateMessageTextOnUiThread();
    }

    @Override
    public void onMessageUpdated(TalkClientMessage message) {
        updateMessageTextOnUiThread();
    }

    @Override
    public void onMessageDeleted(TalkClientMessage message) {
        updateMessageTextOnUiThread();
    }

    @Override
    protected void updateViews() {
        super.updateViews();
        updateBlockedContainer();
        updateName();
        updateMessageText();
        updateInviteButton(mContact);
        updateDeclineButton(mContact);
        hideNicknameEdit();
        updateNicknameContainer();
    }

    private void updateBlockedContainer() {
        if (mContact.isClientBlocked()) {
            mBlockedContainer.setVisibility(View.VISIBLE);
        } else {
            mBlockedContainer.setVisibility(View.GONE);
        }
    }

    private void updateName() {
        TalkPresence presence = mContact.getClientPresence();
        if (presence != null) {
            mNameText.setText(presence.getClientName());
        }
    }

    private void updateInviteButton(final TalkClientContact contact) {
        Button inviteButton = (Button) getView().findViewById(R.id.btn_profile_invite);
        mInviteButtonContainer.setVisibility(View.VISIBLE);

        if (contact.getClientRelationship() == null || contact.getClientRelationship().isNone()) {
            inviteButton.setText(R.string.friend_request_add_as_friend);
            inviteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getXoActivity().getXoClient().inviteFriend(contact);
                }
            });
        } else if (contact.getClientRelationship().isInvited()) {
            inviteButton.setText(R.string.friend_request_cancel_invitation);
            inviteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getXoActivity().getXoClient().disinviteFriend(contact);
                }
            });
        } else if (contact.getClientRelationship().invitedMe()) {
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
        declineButton.setVisibility(View.VISIBLE);

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
                }
        );
    }

    private void unblockContact() {
        LOG.debug("unblockContact()");
        getXoClient().unblockContact(mContact);
        getActivity().finish();
    }

    @Override
    public void onClientPresenceChanged(final TalkClientContact contact) {
        if (isCurrentContact(contact)) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    refreshContactFromDatabase();
                    updateContent();
                    getActivity().invalidateOptionsMenu();
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
                    refreshContactFromDatabase();
                    updateContent();
                    getActivity().invalidateOptionsMenu();
                }
            });
        }
    }

    private boolean isCurrentContact(TalkClientContact contact) {
        return mContact == contact || mContact.getClientContactId() == contact.getClientContactId();
    }

    @Override
    public void onGroupPresenceChanged(TalkClientContact contact) {

    }

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) {

    }
}
