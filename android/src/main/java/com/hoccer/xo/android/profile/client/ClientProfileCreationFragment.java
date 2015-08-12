package com.hoccer.xo.android.profile.client;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.*;
import android.widget.EditText;
import android.widget.ImageView;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.XoTransfer;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.content.SelectedContent;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.profile.ProfileFragment;
import com.hoccer.xo.android.util.UriUtils;
import com.squareup.picasso.Picasso;
import org.apache.log4j.Logger;

import java.sql.SQLException;


public class ClientProfileCreationFragment extends ProfileFragment implements IXoContactListener, View.OnClickListener, ActionMode.Callback {

    private static final String HOCCER_CLASSIC_PREFERENCES = "com.artcom.hoccer_preferences";

    private static final Logger LOG = Logger.getLogger(ClientProfileCreationFragment.class);

    private ActionMode mActionMode;

    private ImageView mAvatarImage;
    private EditText mEditName;

    private TalkClientContact mContact;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        showWelcomeDialogIfUpdatedFromHoccerClassic();
    }

    private void showWelcomeDialogIfUpdatedFromHoccerClassic() {
        if (hasUpdatedFromHoccerClassic()) {
            XoDialogs.showOkDialog(null, R.string.hoccer_3_0, R.string.welcome_hoccer_classic_users, getActivity());
        }
    }

    private boolean hasUpdatedFromHoccerClassic() {
        SharedPreferences preferences = getActivity().getSharedPreferences(HOCCER_CLASSIC_PREFERENCES, Context.MODE_PRIVATE);
        return preferences.contains("client_uuid");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_single_profile_creation, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAvatarImage = (ImageView) view.findViewById(R.id.profile_avatar_image);
        mEditName = (EditText) view.findViewById(R.id.et_profile_name);

        mContact = XoApplication.get().getXoClient().getSelfContact();
        if (mActionMode == null) {
            mActionMode = getActivity().startActionMode(this);
        }

        updateAvatarView();
    }

    @Override
    public void onResume() {
        super.onResume();
        XoApplication.get().getXoClient().registerContactListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        XoApplication.get().getXoClient().unregisterContactListener(this);
    }

    @Override
    public void onAvatarSelected(SelectedContent avatar) {
        uploadAvatar(avatar);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.profile_avatar_image) {
            if (mContact != null && mContact.isEditable()) {
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
                                            selectAvatar();
                                        }
                                        break;
                                        case 1: {
                                            uploadAvatar(null);
                                        }
                                    }
                                }
                            }
                    );
                } else {
                    selectAvatar();
                }
            }
        }
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
                .centerCrop()
                .fit()
                .placeholder(R.drawable.avatar_contact_large)
                .error(R.drawable.avatar_contact_large)
                .into(mAvatarImage);
    }

    private void uploadAvatar(final SelectedContent avatar) {
        if (avatar != null) {
            XoApplication.get().getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    LOG.debug("creating avatar upload");
                    TalkClientUpload upload = new TalkClientUpload();
                    upload.initializeAsAvatar(avatar);
                    try {
                        XoApplication.get().getXoClient().getDatabase().saveClientUpload(upload);
                        if (mContact.isSelf()) {
                            XoApplication.get().getXoClient().setClientAvatar(upload);
                        }
                    } catch (SQLException e) {
                        LOG.error("sql error", e);
                    }
                }
            });
        } else {
            XoApplication.get().getXoClient().setClientAvatar(null);
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        // In 5.1 it no longer calls callback.onPrepareActionMode
        mEditName.setVisibility(View.VISIBLE);
        mAvatarImage.setOnClickListener(this);
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

        mActionMode = null;

        String nameString = mEditName.getText().toString().trim();
        String newUserName = nameString.isEmpty() ? getResources().getString(R.string.profile_self_initial_name) : nameString;

        mEditName.setVisibility(View.GONE);
        mAvatarImage.setOnClickListener(null);

        XoApplication.get().getXoClient().setClientString(newUserName, "happy");

        getActivity().finish();
    }

    @Override
    public void onClientPresenceChanged(TalkClientContact contact) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateAvatarView();
            }
        });
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
