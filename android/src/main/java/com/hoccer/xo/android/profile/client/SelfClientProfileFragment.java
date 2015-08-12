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
import com.hoccer.talk.client.XoTransfer;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.client.predicates.TalkClientContactPredicates;
import com.hoccer.talk.content.SelectedContent;
import com.hoccer.talk.model.TalkGroupMembership;
import com.hoccer.talk.model.TalkRelationship;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.activity.ChatsActivity;
import com.hoccer.xo.android.base.XoActivity;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.List;

import static com.hoccer.xo.android.util.UriUtils.getAbsoluteFileUri;

public class SelfClientProfileFragment extends ClientProfileFragment implements ActionMode.Callback {

    private static final Logger LOG = Logger.getLogger(SelfClientProfileFragment.class);

    private EditText mNameEditText;
    private RelativeLayout mContactStatsContainer;
    private TextView mContactStatsText;
    private Button mAccountDeletionButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_self_client_profile, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mNameEditText = (EditText) view.findViewById(R.id.et_profile_name);
        mContactStatsContainer = (RelativeLayout) view.findViewById(R.id.inc_profile_contact_stats);
        mContactStatsText = (TextView) view.findViewById(R.id.tv_profile_contacts_text);
        mAccountDeletionButton = (Button) view.findViewById(R.id.btn_profile_delete_account);

        updateContent();
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
    protected void updateViews() {
        super.updateViews();
        updateName();
        updateContactsContainer();
        updateFingerprint();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.fragment_self_client_profile, menu);
        menu.findItem(R.id.menu_my_profile).setVisible(false);
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

    private void exitApplication() {
        Intent intent = new Intent(getActivity(), ChatsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(ChatsActivity.INTENT_EXTRA_EXIT, true);
        startActivity(intent);
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
            friendsCount = XoApplication.get().getXoClient().getDatabase().findClientContactsByState(TalkRelationship.STATE_FRIEND).size();
            blockedCount = XoApplication.get().getXoClient().getDatabase().findClientContactsByState(TalkRelationship.STATE_BLOCKED).size();

            List<TalkClientContact> joinedGroups = XoApplication.get().getXoClient().getDatabase().findGroupContactsByMembershipState(TalkGroupMembership.STATE_JOINED);
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

    @Override
    protected XoTransfer getAvatarTransfer() {
        return mContact.getAvatarUpload();
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mNameEditText.setVisibility(View.VISIBLE);
        mNameText.setVisibility(View.INVISIBLE);
        mNameEditText.setText(mNameText.getText());
        mAccountDeletionButton.setVisibility(View.VISIBLE);

        mAvatarImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectOrDeleteAvatar();
            }
        });
        mAccountDeletionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAccountDelectionDialog();
            }
        });

        return true;
    }

    private void selectOrDeleteAvatar() {
        if (mContact.getAvatarFilePath() != null) {
            showSetOrDeleteAvatarDialog();
        } else {
            selectAvatar();
        }
    }

    private void showSetOrDeleteAvatarDialog() {
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
                                selectAvatar();
                            }
                            break;
                            case 1: {
                                removeAvatar();
                            }
                        }
                    }
                }
        );
    }

    @Override
    public void onAvatarSelected(final SelectedContent avatar) {
        uploadSelectedAvatar(avatar);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateAvatarView(getAbsoluteFileUri(avatar.getFilePath()));
            }
        });
    }

    private void uploadSelectedAvatar(final SelectedContent avatar) {
        XoApplication.get().getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                LOG.debug("creating avatar upload");
                TalkClientUpload upload = new TalkClientUpload();
                upload.initializeAsAvatar(avatar);
                try {
                    XoApplication.get().getXoClient().getDatabase().saveClientUpload(upload);
                    XoApplication.get().getXoClient().setClientAvatar(upload);
                } catch (SQLException e) {
                    LOG.error("sql error", e);
                }
            }
        });
    }

    private void removeAvatar() {
        XoApplication.get().getXoClient().setClientAvatar(null);
        updateAvatarView(R.drawable.avatar_contact_large);
    }

    private void showAccountDelectionDialog() {
        XoDialogs.showPositiveNegativeDialog("AccountDeletionDialog",
                R.string.button_delete_account_title,
                R.string.dialog_delete_account_warning,
                getActivity(),
                R.string.dialog_delete_account_ok,
                R.string.common_cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        XoApplication.get().getXoClient().deleteAccountAndLocalDatabase(getActivity());
                        exitApplication();
                    }
                }, null);
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

        XoApplication.get().getXoClient().setClientString(newUserName, "happier");

        refreshContactFromDatabase();
        updateContent();
    }
}
