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
        mContact = getContactById();
    }

    private TalkClientContact getContactById() {
        if (getArguments() != null && getArguments().containsKey(ARG_CLIENT_CONTACT_ID)) {
            try {
                int contactId = getArguments().getInt(ARG_CLIENT_CONTACT_ID);
                return XoApplication.get().getXoClient().getDatabase().findContactById(contactId);
            } catch (SQLException e) {
                LOG.error("SQL error while retrieving contact ", e);
            }
        }

        return null;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAvatarImage = (ImageView) view.findViewById(R.id.profile_avatar_image);
        mNameText = (TextView) view.findViewById(R.id.tv_profile_name);

        setHasOptionsMenu(true);
    }
}
