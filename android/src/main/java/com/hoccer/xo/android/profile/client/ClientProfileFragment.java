package com.hoccer.xo.android.profile.client;

import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.XoTransfer;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.model.TalkPresence;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.profile.ProfileFragment;
import com.hoccer.xo.android.util.UriUtils;
import com.squareup.picasso.Picasso;
import org.apache.log4j.Logger;

import java.sql.SQLException;

public abstract class ClientProfileFragment extends ProfileFragment {

    private static final Logger LOG = Logger.getLogger(ClientProfileFragment.class);

    protected TextView mNameText;
    protected ImageView mAvatarImage;
    protected TextView mKeyText;

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
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshContact();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.fragment_single_profile, menu);
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

    @Override
    protected void updateView() {
        updateFingerprint();
    }

    protected void updateAvatarView(XoTransfer avatarTransfer) {
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

    protected void updateFingerprint() {
        if (mContact.getPublicKey() != null) {
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
        } else {
            mKeyText.setText("");
        }
    }

    protected void refreshContact() {
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

    protected abstract void updateActionBar();

    private boolean isCurrentContact(TalkClientContact contact) {
        return mContact == contact || mContact.getClientContactId() == contact.getClientContactId();
    }

    @Override
    protected boolean shouldShowChatContainer(int count) {
        return false;
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
}
