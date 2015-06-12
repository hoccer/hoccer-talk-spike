package com.hoccer.xo.android.profile;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.base.XoFragment;
import org.apache.log4j.Logger;

import java.sql.SQLException;

public abstract class ProfileFragment extends XoFragment {

    public static final String ARG_CLIENT_CONTACT_ID = "ARG_CLIENT_CONTACT_ID";

    private static final Logger LOG = Logger.getLogger(ProfileFragment.class);

    protected TalkClientContact mContact;

    protected TextView mNameText;
    protected ImageView mAvatarImage;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        setContact();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAvatarImage = (ImageView) view.findViewById(R.id.profile_avatar_image);
        mNameText = (TextView) view.findViewById(R.id.tv_profile_name);
    }

    private void setContact() {
        if (getArguments() != null && getArguments().containsKey(ARG_CLIENT_CONTACT_ID)) {
            int clientContactId = getArguments().getInt(ARG_CLIENT_CONTACT_ID);

            try {
                mContact = XoApplication.get().getXoClient().getDatabase().findContactById(clientContactId);
            } catch (SQLException e) {
                LOG.error("SQL error while retrieving contact ", e);
            }
        }

        if (mContact == null) {
            throw new IllegalArgumentException("Contact missing or does not exist.");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setHasOptionsMenu(true);
    }

    protected abstract int getClientContactId();

    protected abstract void updateView();
}
