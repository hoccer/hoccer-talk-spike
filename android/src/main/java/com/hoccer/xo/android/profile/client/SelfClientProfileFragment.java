package com.hoccer.xo.android.profile.client;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.client.predicates.TalkClientContactPredicates;
import com.hoccer.talk.content.SelectedContent;
import com.hoccer.talk.model.TalkGroupMembership;
import com.hoccer.talk.model.TalkRelationship;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.activity.ChatsActivity;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.List;

public class SelfClientProfileFragment extends ClientProfileFragment
        implements View.OnClickListener, ActionMode.Callback {

    private static final Logger LOG = Logger.getLogger(SelfClientProfileFragment.class);

    private EditText mNameEditText;
    private RelativeLayout mContactStatsContainer;
    private TextView mContactStatsText;
    private Button mAccountDeletionButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_single_profile, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mNameEditText = (EditText) view.findViewById(R.id.et_profile_name);
        mContactStatsContainer = (RelativeLayout) view.findViewById(R.id.inc_profile_contacts);
        mContactStatsText = (TextView) view.findViewById(R.id.tv_profile_contacts_text);
        mAccountDeletionButton = (Button) view.findViewById(R.id.btn_profile_delete_account);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshContact();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        updateMenuItems(menu);
    }

    private void updateMenuItems(Menu menu) {
        menu.findItem(R.id.menu_my_profile).setVisible(false);
        menu.findItem(R.id.menu_profile_edit).setVisible(true);
        menu.findItem(R.id.menu_profile_block).setVisible(false);
        menu.findItem(R.id.menu_profile_unblock).setVisible(false);
        menu.findItem(R.id.menu_profile_delete).setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean isSelectionHandled;
        switch (item.getItemId()) {
            case R.id.menu_profile_edit:
                getActivity().startActionMode(this);
                isSelectionHandled = true;
                break;
            default:
                isSelectionHandled = super.onOptionsItemSelected(item);
        }
        return isSelectionHandled;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.profile_avatar_image:
                onAvatarClick();
                break;
            case R.id.btn_profile_delete_account:
                onAccountDeletionButtonClick();
                break;
        }
    }

    private void onAvatarClick() {
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

    private void onAccountDeletionButtonClick() {
        XoDialogs.showPositiveNegativeDialog("AccountDeletionDialog",
                R.string.button_delete_account_title,
                R.string.dialog_delete_account_warning,
                getActivity(),
                R.string.dialog_delete_account_ok,
                R.string.common_cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getXoClient().deleteAccountAndLocalDatabase(getActivity());
                        exitApplication();
                    }
                }, null);
    }

    private void exitApplication() {
        Intent intent = new Intent(getActivity(), ChatsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(ChatsActivity.INTENT_EXTRA_EXIT, true);
        startActivity(intent);
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
                        getXoClient().setClientAvatar(upload);
                    } catch (SQLException e) {
                        LOG.error("sql error", e);
                    }
                }
            });
        } else {
            getXoClient().setClientAvatar(null);
        }
    }

    @Override
    public void updateActionBar() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getActivity().getActionBar().setTitle(R.string.my_profile_title);
            }
        });
    }

    @Override
    protected void updateView() {
        super.updateView();
        updateAvatarView();
        updateName();
        updateContactsContainer();
        updateFingerprint();
        updateChatContainer(); //todo remove
    }

    private void updateAvatarView() {
        super.updateAvatarView(mContact.getAvatarUpload());
    }

    private void updateName() {
        String name = mContact.getSelf().getRegistrationName();
        mNameText.setText(name);
    }

    private void updateContactsContainer() {
        mContactStatsContainer.setVisibility(View.VISIBLE);

        int friendsCount = 0;
        int blockedCount = 0;
        int groupsCount = 0;
        try {
            friendsCount = getXoDatabase().findClientContactsByState(TalkRelationship.STATE_FRIEND).size();
            blockedCount = getXoDatabase().findClientContactsByState(TalkRelationship.STATE_BLOCKED).size();

            List<TalkClientContact> joinedGroups = getXoDatabase().findGroupContactsByMembershipState(TalkGroupMembership.STATE_JOINED);
            CollectionUtils.filterInverse(joinedGroups, TalkClientContactPredicates.IS_ENVIRONMENT_GROUP_PREDICATE);
            groupsCount = joinedGroups.size();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        int clientContactsCount = friendsCount + blockedCount;
        mContactStatsText.setText(
                clientContactsCount + " " + getResources().getQuantityString(R.plurals.profile_contacts_text_friends, clientContactsCount) + "   "
                        +
                        groupsCount + " " + getResources().getQuantityString(R.plurals.profile_contacts_text_groups, groupsCount));
    }

    private void updateChatContainer() {
        mChatContainer.setVisibility(View.GONE);
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
        mNameEditText.setVisibility(View.VISIBLE);
        mNameText.setVisibility(View.INVISIBLE);
        mNameEditText.setText(mNameText.getText());
        mAccountDeletionButton.setVisibility(View.VISIBLE);

        mAvatarImage.setOnClickListener(this);
        mAccountDeletionButton.setOnClickListener(this);

        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        String nameString = mNameEditText.getText().toString().trim();
        String newUserName = nameString.isEmpty() ? getResources().getString(R.string.profile_self_initial_name) : nameString;

        mNameText.setText(newUserName);
        mNameEditText.setVisibility(View.GONE);
        mNameText.setVisibility(View.VISIBLE);
        mAccountDeletionButton.setVisibility(View.GONE);
        mAccountDeletionButton.setOnClickListener(null);
        mAvatarImage.setOnClickListener(null);

        getXoClient().setClientString(newUserName, "happier");
        refreshContact();
        updateView();
    }
}
