package com.hoccer.xo.android.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.IXoMessageListener;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.activity.MessagingActivity;
import com.hoccer.xo.android.base.XoFragment;
import com.hoccer.xo.android.util.IntentHelper;
import com.artcom.hoccer.R;
import org.apache.log4j.Logger;

import java.sql.SQLException;

public abstract class ProfileFragment extends XoFragment implements IXoContactListener, IXoMessageListener {

    public static final String ARG_CLIENT_CONTACT_ID = "ARG_CLIENT_CONTACT_ID";

    private static final Logger LOG = Logger.getLogger(ProfileFragment.class);

    protected TalkClientContact mContact;

    protected RelativeLayout mChatContainer;
    private RelativeLayout mChatMessagesContainer;
    private TextView mChatMessagesText;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mChatContainer = (RelativeLayout) view.findViewById(R.id.inc_chat_stats);
        mChatMessagesContainer = (RelativeLayout) view.findViewById(R.id.rl_messages_container);
        mChatMessagesText = (TextView) view.findViewById(R.id.tv_messages_text);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        setContact();
    }

    @Override
    public void onResume() {
        super.onResume();

        setHasOptionsMenu(true);

        mChatMessagesContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMessagingActivity();
            }
        });

        getXoClient().registerContactListener(this);
        getXoClient().registerMessageListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        getXoClient().unregisterContactListener(this);
        getXoClient().unregisterMessageListener(this);
    }

    private void setContact() {
        if (getArguments() != null && getArguments().containsKey(ARG_CLIENT_CONTACT_ID)) {
            int clientContactId = getArguments().getInt(ARG_CLIENT_CONTACT_ID);
            try {
                mContact = XoApplication.getXoClient().getDatabase().findContactById(clientContactId);
            } catch (SQLException e) {
                LOG.error("SQL error while retrieving contact ", e);
            }
        } else {
            LOG.error("Creating ProfileFragment without arguments is not supported.");
        }
    }

    protected void showMessagingActivity() {
        Intent intent = new Intent(getActivity(), MessagingActivity.class);
        intent.putExtra(IntentHelper.EXTRA_CONTACT_ID, getClientContactId());
        getXoActivity().startActivity(intent);
    }

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
    public void onClientPresenceChanged(TalkClientContact contact) {}

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {}

    @Override
    public void onGroupPresenceChanged(TalkClientContact contact) {}

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) {}
}
