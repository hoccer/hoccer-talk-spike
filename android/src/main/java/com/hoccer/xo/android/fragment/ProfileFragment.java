package com.hoccer.xo.android.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.IXoMessageListener;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.activity.MessagingActivity;
import com.hoccer.xo.android.base.XoFragment;
import com.hoccer.xo.android.util.IntentHelper;
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

        mChatContainer = (RelativeLayout) view.findViewById(R.id.inc_profile_chat_stats);
        mChatMessagesContainer = (RelativeLayout) view.findViewById(R.id.rl_profile_messages);
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

        updateMessageText();
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
                mContact = XoApplication.get().getXoClient().getDatabase().findContactById(clientContactId);
            } catch (SQLException e) {
                LOG.error("SQL error while retrieving contact ", e);
            }
        }

        if (mContact == null) {
            throw new IllegalArgumentException("Contact missing or does not exist.");
        }
    }

    protected void showMessagingActivity() {
        Intent intent = new Intent(getActivity(), MessagingActivity.class);
        intent.putExtra(IntentHelper.EXTRA_CONTACT_ID, getClientContactId());
        if (mContact.isKept() && !(mContact.getClientRelationship().isFriend() || mContact.getClientRelationship().isBlocked())) {
            intent.putExtra(MessagingActivity.EXTRA_CLIENT_HISTORY, true);
        }
        startActivity(intent);
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
        if (count == 0) {
            mChatContainer.setVisibility(View.GONE);
        } else {
            mChatContainer.setVisibility(View.VISIBLE);
            mChatMessagesText.setText(getResources().getQuantityString(R.plurals.message_count, count, count));
        }
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
    public void onClientPresenceChanged(TalkClientContact contact) {
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
