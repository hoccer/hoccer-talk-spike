package com.hoccer.xo.android.profile.client;

import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.XoTransfer;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.profile.ProfileFragment;
import com.hoccer.xo.android.util.UriUtils;
import com.squareup.picasso.Picasso;
import org.apache.log4j.Logger;

import java.sql.SQLException;

public abstract class ClientProfileFragment extends ProfileFragment {

    private static final Logger LOG = Logger.getLogger(ClientProfileFragment.class);

    protected TextView mKeyText;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mKeyText = (TextView) view.findViewById(R.id.tv_profile_key);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshContact();
    }

    @Override
    protected int getClientContactId() {
        return mContact.getClientContactId();
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
}
