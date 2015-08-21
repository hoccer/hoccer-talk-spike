package com.hoccer.xo.android.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.*;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.model.TalkRelationship;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.activity.MediaBrowserActivity;
import com.hoccer.xo.android.adapter.ChatMessagesAdapter;
import com.hoccer.xo.android.base.BaseActivity;
import com.hoccer.xo.android.base.MessagesAdapter;
import com.hoccer.xo.android.gesture.Gestures;
import com.hoccer.xo.android.gesture.MotionInterpreter;
import com.hoccer.xo.android.profile.client.ClientProfileActivity;
import com.hoccer.xo.android.profile.group.GroupProfileActivity;
import com.hoccer.xo.android.util.IntentHelper;
import com.hoccer.xo.android.view.chat.MessageItem;
import com.nostra13.universalimageloader.core.ImageLoader;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.List;

/**
 * Fragment for conversations
 */
public class ChatFragment extends XoChatListFragment
        implements SearchView.OnQueryTextListener,
        MessagesAdapter.AdapterReloadListener, IXoContactListener {

    public static final String ARG_CLIENT_CONTACT_ID = "com.hoccer.xo.android.fragment.ARG_CLIENT_CONTACT_ID";

    private static final Logger LOG = Logger.getLogger(ChatFragment.class);

    private static final String KEY_SCROLL_POSITION = "scroll_position:";

    private ListView mMessageListView;

    private MotionInterpreter mMotionInterpreter;

    private TextView mEmptyText;

    private TalkClientContact mContact;

    private ChatMessagesAdapter mAdapter;

    private CompositionFragment mCompositionFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            int clientContactId = getArguments().getInt(ARG_CLIENT_CONTACT_ID);
            try {
                mContact = XoApplication.get().getClient().getDatabase().findContactById(clientContactId);
            } catch (SQLException e) {
                LOG.error("sql error", e);
                return;
            }

            // log error if the contact was not found
            if (mContact == null) {
                LOG.error("Client contact with id '" + clientContactId + "' does not exist");
                return;
            }
        } else {
            LOG.error("MessagingFragment requires clientContactId as argument.");
            return;
        }

        createCompositionFragment();

        mMotionInterpreter = new MotionInterpreter(Gestures.Transaction.SHARE, getActivity(), mCompositionFragment);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_messaging, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mEmptyText = (TextView) view.findViewById(R.id.messaging_empty);
    }

    private void createCompositionFragment() {
        Bundle bundle = new Bundle();
        bundle.putInt(CompositionFragment.ARG_CLIENT_CONTACT_ID, mContact.getClientContactId());

        mCompositionFragment = new CompositionFragment();
        mCompositionFragment.setArguments(bundle);

        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.add(R.id.fragment_container, mCompositionFragment).commit();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        List<Fragment> fragments = getChildFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                fragment.onActivityResult(requestCode, resultCode, intent);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        setHasOptionsMenu(true);
        mMessageListView = getListView();

        mAdapter = new ChatMessagesAdapter(mMessageListView, (BaseActivity) getActivity(), mContact);
        mAdapter.setAdapterReloadListener(this);
        mAdapter.onCreate();

        mMessageListView.setAdapter(mAdapter);

        configureMotionInterpreterForContact(mContact);
        XoApplication.get().getClient().registerContactListener(this);

        // send intent to XoClientService that we are conversing with the contact
        Intent intent = new Intent();
        intent.setAction(IntentHelper.ACTION_CONTACT_ID_IN_CONVERSATION);
        intent.putExtra(IntentHelper.EXTRA_CONTACT_ID, mContact.getClientContactId());
        getActivity().sendBroadcast(intent);
        applySavedScrollPosition();
    }

    @Override
    public void onPause() {
        super.onPause();
        mAdapter.onPause();
        mMotionInterpreter.deactivate();
        XoApplication.get().getClient().unregisterContactListener(this);
        ImageLoader.getInstance().clearMemoryCache();

        // send intent to XoClientService that we are not conversing any longer
        Intent intent = new Intent();
        intent.setAction(IntentHelper.ACTION_CONTACT_ID_IN_CONVERSATION);
        intent.putExtra(IntentHelper.EXTRA_CONTACT_ID, -1);
        getActivity().sendBroadcast(intent);
        saveScrollPosition();
    }

    @Override
    protected String getScrollPositionId() {
        return mContact.isClient() ? mContact.getClientId() : mContact.getGroupId();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mAdapter != null) {
            mAdapter.onDestroy();
            mAdapter = null;
        }

        // Ensure that all items receive the detach call
        if (mMessageListView != null) {
            for (int i = 0; i < mMessageListView.getChildCount(); i++) {
                MessageItem item = (MessageItem) mMessageListView.getChildAt(i).getTag();
                if (item != null) {
                    item.detachView();
                }
            }
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem mMuteItem = menu.findItem(R.id.menu_mute_contact);
        if (mContact.isNotificationsDisabled()) {
            mMuteItem.setIcon(R.drawable.ic_action_notifications_disabled);
        } else {
            mMuteItem.setIcon(R.drawable.ic_action_notifications_enabled);
        }
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.fragment_messaging, menu);
        // select client/group profile entry for appropriate icon
        if (mContact != null) {
            if (mContact.isNearbyGroup()) {
                getActivity().getActionBar().setTitle(getActivity().getResources().getString(R.string.all_nearby));
            } else if (mContact.isWorldwideGroup()) {
                getActivity().getActionBar().setTitle(getActivity().getResources().getString(R.string.all_worldwide));
            } else {
                getActivity().getActionBar().setTitle(mContact.getNickname());
                MenuItem muteItem = menu.findItem(R.id.menu_mute_contact);
                muteItem.setVisible(true);
            }

            MenuItem clientItem = menu.findItem(R.id.menu_profile_single);
            clientItem.setVisible(mContact.isClient());

            MenuItem groupItem = menu.findItem(R.id.menu_profile_group);
            groupItem.setVisible(mContact.isGroup());

            MenuItem musicItem = menu.findItem(R.id.menu_audio_attachment_list);
            musicItem.setVisible(true);

            MenuItem createPermanentGroupItem = menu.findItem(R.id.menu_group_profile_create_permanent_group);
            createPermanentGroupItem.setVisible(mContact.isNearbyGroup() || mContact.isWorldwideGroup());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        LOG.debug("onOptionsItemSelected(" + item + ")");

        switch (item.getItemId()) {
            case R.id.menu_profile_single:
                if (mContact != null) {
                    startActivity(new Intent(getActivity(), ClientProfileActivity.class)
                            .setAction(ClientProfileActivity.ACTION_SHOW)
                            .putExtra(ClientProfileActivity.EXTRA_CLIENT_CONTACT_ID, mContact.getClientContactId()));
                }
                break;
            case R.id.menu_profile_group:
                if (mContact != null) {
                    startActivity(new Intent(getActivity(), GroupProfileActivity.class)
                            .setAction(GroupProfileActivity.ACTION_SHOW)
                            .putExtra(GroupProfileActivity.EXTRA_CLIENT_CONTACT_ID, mContact.getClientContactId()));
                }
                break;
            case R.id.menu_audio_attachment_list:
                if (mContact != null) {
                    showAudioAttachmentList();
                }
                break;
            case R.id.menu_group_profile_create_permanent_group:
                if (mContact != null) {
                    startActivity(new Intent(getActivity(), GroupProfileActivity.class)
                            .setAction(GroupProfileActivity.ACTION_CLONE)
                            .putExtra(GroupProfileActivity.EXTRA_GROUP_ID, mContact.getGroupId()));
                }
                break;
            case R.id.menu_mute_contact:
                onMuteItemClick();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onAdapterReloadStarted(MessagesAdapter adapter) {
        LOG.debug("onAdapterReloadStarted()");
        mEmptyText.setText(R.string.messaging_loading);
    }

    @Override
    public void onAdapterReloadFinished(MessagesAdapter adapter) {
        LOG.debug("onAdapterReloadFinished()");
        mEmptyText.setText(R.string.messaging_no_messages);
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        LOG.debug("onQueryTextChange(\"" + newText + "\")");
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        LOG.debug("onQueryTextSubmit(\"" + query + "\")");
        return true;
    }

    public void configureMotionInterpreterForContact(TalkClientContact contact) {
        // react on gestures only when contact is nearby
        if (contact != null && (contact.isNearby() ||
                (contact.isGroup() && contact.getGroupPresence() != null && contact.getGroupPresence().isTypeNearby()))) {
            mMotionInterpreter.activate();
        } else {
            mMotionInterpreter.deactivate();
        }
    }

    @Override
    public void onClientPresenceChanged(TalkClientContact contact) {
    }

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {
        if (contact.equals(mContact)) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getActivity().invalidateOptionsMenu();
                }
            });
        }
    }

    @Override
    public void onGroupPresenceChanged(TalkClientContact contact) {
    }

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) {
        if (contact.equals(mContact)) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getActivity().invalidateOptionsMenu();
                }
            });
        }
    }

    public void showAudioAttachmentList() {
        Intent intent = new Intent(getActivity(), MediaBrowserActivity.class);
        intent.putExtra(IntentHelper.EXTRA_CONTACT_ID, mContact.getClientContactId());
        startActivity(intent);
    }

    public void onKeyboardOpen() {
        mMessageListView.setSelection(mMessageListView.getCount() - 1);
    }

    private void onMuteItemClick() {
        if (XoApplication.get().getClient().isReady()) {
            boolean muted = toggleMute();
            int toastText = muted ? R.string.toast_mute_chat : R.string.toast_unmute_chat;
            Toast.makeText(getActivity(), toastText, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getActivity(), R.string.no_connection_available, Toast.LENGTH_LONG).show();
        }
    }

    private boolean toggleMute(){
        boolean mute = !mContact.isNotificationsDisabled();
        String notificationPreference = mute ? TalkRelationship.NOTIFICATIONS_DISABLED : TalkRelationship.NOTIFICATIONS_ENABLED;

        if (mContact.isGroup()) {
            XoApplication.get().getClient().getServerRpc().setGroupNotifications(mContact.getGroupId(), notificationPreference);
        } else {
            XoApplication.get().getClient().getServerRpc().setClientNotifications(mContact.getClientId(), notificationPreference);
        }

        return mute;
    }

}
