package com.hoccer.xo.android.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.IXoMessageListener;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.activity.MessagingActivity;
import com.hoccer.xo.android.base.XoFragment;
import com.hoccer.xo.android.util.IntentHelper;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

import java.sql.SQLException;

public abstract class ProfileFragment extends XoFragment implements IXoContactListener, IXoMessageListener {

    public static final String ARG_CLIENT_CONTACT_ID = "ARG_CLIENT_CONTACT_ID";

    private static final Logger LOG = Logger.getLogger(ProfileFragment.class);

    protected TextView mNameText;
    protected EditText mNameEditText;

    protected RelativeLayout mChatContainer;
    protected RelativeLayout mChatMessagesContainer;
    protected TextView mChatMessagesText;
    protected ImageView mAvatarImage;

    protected IContentObject mAvatarToSet;

    protected TalkClientContact mContact;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        setContact();

        startMessagingActivityOnChatMessagesContainerClick();

        getXoClient().registerContactListener(this);
        getXoClient().registerMessageListener(this);
        setHasOptionsMenu(true);
    }

    private void setContact() {
        if (getArguments() != null && getArguments().containsKey(ARG_CLIENT_CONTACT_ID)) {
            int clientContactId = getArguments().getInt(ARG_CLIENT_CONTACT_ID);
            try {
                mContact = XoApplication.getXoClient().getDatabase().findClientContactById(clientContactId);
            } catch (SQLException e) {
                LOG.error("SQL error while retrieving contact ", e);
            }
        } else {
            LOG.error("Creating ProfileFragment without arguments is not supported.");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        getXoClient().unregisterContactListener(this);
        getXoClient().unregisterMessageListener(this);
    }

    private void startMessagingActivityOnChatMessagesContainerClick() {
        mChatMessagesContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), MessagingActivity.class);
                intent.putExtra(IntentHelper.EXTRA_CONTACT_ID, getClientContactId());
                getXoActivity().startActivity(intent);
            }
        });
    }

    protected abstract int getClientContactId();

    protected abstract void updateMessageText();

    protected abstract void updateView();

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
    public void onContactAdded(TalkClientContact contact) {}

    @Override
    public void onContactRemoved(TalkClientContact contact) {}

    @Override
    public void onClientPresenceChanged(TalkClientContact contact) {}

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {}

    @Override
    public void onGroupPresenceChanged(TalkClientContact contact) {}

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) {}

    private void updateMessageTextOnUiThread() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateMessageText();
            }
        });
    }

    protected void updateMessageText(int count) {
        mChatMessagesText.setText(getResources().getQuantityString(R.plurals.message_count, count, count));
    }

}
