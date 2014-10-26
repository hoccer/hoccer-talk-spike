package com.hoccer.xo.android.fragment;

import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.base.XoFragment;
import com.hoccer.xo.android.content.SelectedContent;
import com.hoccer.xo.release.R;
import com.squareup.picasso.Picasso;
import org.apache.log4j.Logger;

import javax.xml.soap.Text;
import java.io.File;
import java.sql.SQLException;

public class SingleProfileCreationFragment extends XoFragment implements IXoContactListener, View.OnClickListener, ActionMode.Callback {

    private static final Logger LOG = Logger.getLogger(SingleProfileFragment.class);

    private ActionMode mActionMode;

    private ImageView mAvatarImage;
    private EditText mEditName;

    private IContentObject mAvatarToSet;

    private TalkClientContact mContact;
    private String mAvatarUrl;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_single_profile_creation, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAvatarImage = (ImageView) view.findViewById(R.id.profile_avatar_image);
        mEditName = (EditText) view.findViewById(R.id.et_profile_name);

        hideView(view.findViewById(R.id.tv_profile_name));

        init();
    }

    private void hideView(View view) {
        view.setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        getXoClient().registerContactListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getXoClient().unregisterContactListener(this);
    }

    @Override
    public void onAvatarSelected(IContentObject contentObject) {
        mAvatarToSet = contentObject;
    }

    @Override
    public void onServiceConnected() {
        if (mAvatarToSet != null) {
            updateAvatar(mAvatarToSet);
            mAvatarToSet = null;
        }
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

    private void init() {
        mContact = getXoClient().getSelfContact();
        if (mActionMode == null) {
            mActionMode = getActivity().startActionMode(this);
        }
        finishActivityIfContactDeleted();

        updateView();
        updateActionBar();
    }

    public void updateActionBar() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getActivity().getActionBar().setTitle(R.string.welcome_to_title);
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

    private void updateView() {
        updateAvatarView();
    }

    private void updateAvatarView() {
        if (mContact.isSelf()) {
            TalkClientUpload avatarUpload = mContact.getAvatarUpload();
            if (avatarUpload != null && avatarUpload.isContentAvailable()) {
                 mAvatarUrl = avatarUpload.getContentDataUrl();
            }
        } else {
            TalkClientDownload avatarDownload = mContact.getAvatarDownload();
            if (avatarDownload != null && avatarDownload.isContentAvailable()) {
                if (avatarDownload.getDataFile() != null) {
                    Uri uri = Uri.fromFile(new File(avatarDownload.getDataFile()));
                    mAvatarUrl = uri.toString();
                }
            }
        }

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Picasso.with(getActivity())
                        .load(mAvatarUrl)
                        .placeholder(R.drawable.avatar_default_contact_large)
                        .error(R.drawable.avatar_default_contact_large)
                        .into(mAvatarImage);
            }
        });
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

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        mEditName.setVisibility(View.VISIBLE);
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

        mEditName.setVisibility(View.GONE);
        mAvatarImage.setOnClickListener(null);

        mContact.getSelf().setRegistrationName(newUserName);
        mContact.updateSelfConfirmed();
        getXoClient().register();
        getXoClient().setClientString(newUserName, "happy");
        getActivity().finish();
    }

    @Override
    public void onContactAdded(TalkClientContact contact) {
    }

    @Override
    public void onContactRemoved(TalkClientContact contact) {
    }

    @Override
    public void onClientPresenceChanged(TalkClientContact contact) {
        updateView();
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
