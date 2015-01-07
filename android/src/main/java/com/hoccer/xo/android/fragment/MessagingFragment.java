package com.hoccer.xo.android.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.*;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.activity.GroupProfileActivity;
import com.hoccer.xo.android.activity.MediaBrowserActivity;
import com.hoccer.xo.android.activity.SingleProfileActivity;
import com.hoccer.xo.android.adapter.ChatAdapter;
import com.hoccer.xo.android.base.XoAdapter;
import com.hoccer.xo.android.base.XoListFragment;
import com.hoccer.xo.android.gesture.Gestures;
import com.hoccer.xo.android.gesture.MotionInterpreter;
import com.hoccer.xo.android.util.IntentHelper;
import com.hoccer.xo.android.view.chat.ChatMessageItem;
import com.nostra13.universalimageloader.core.ImageLoader;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.List;

/**
 * Fragment for conversations
 */
public class MessagingFragment extends XoListFragment
        implements SearchView.OnQueryTextListener,
        XoAdapter.AdapterReloadListener, IXoContactListener {

    public static final String ARG_CLIENT_CONTACT_ID = "com.hoccer.xo.android.fragment.ARG_CLIENT_CONTACT_ID";

    private static final Logger LOG = Logger.getLogger(MessagingFragment.class);

    private ListView mMessageListView;

    private MotionInterpreter mMotionInterpreter;

    private TextView mEmptyText;

    private TalkClientContact mContact;

    private ChatAdapter mAdapter;

    private CompositionFragment mCompositionFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            int clientContactId = getArguments().getInt(ARG_CLIENT_CONTACT_ID);
            try {
                mContact = XoApplication.getXoClient().getDatabase().findContactById(clientContactId);
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
            LOG.error("MessagingFragment requires contactId as argument.");
            return;
        }

        createCompositionFragment();

        mMotionInterpreter = new MotionInterpreter(Gestures.Transaction.SHARE, getXoActivity(), mCompositionFragment);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        LOG.debug("onCreateView()");
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_messaging, container, false);
        return view;
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

        mAdapter = new ChatAdapter(mMessageListView, getXoActivity(), mContact);
        mAdapter.setAdapterReloadListener(this);
        mAdapter.onCreate();

        mMessageListView.setAdapter(mAdapter);

        configureMotionInterpreterForContact(mContact);
        XoApplication.getXoClient().registerContactListener(this);

        // send intent to XoClientService that we are conversing with the contact
        Intent intent = new Intent();
        intent.setAction(IntentHelper.ACTION_CONTACT_ID_IN_CONVERSATION);
        intent.putExtra(IntentHelper.EXTRA_CONTACT_ID, mContact.getClientContactId());
        getActivity().sendBroadcast(intent);
    }

    @Override
    public void onPause() {
        super.onPause();
        mAdapter.onPause();
        mMotionInterpreter.deactivate();
        XoApplication.getXoClient().unregisterContactListener(this);
        ImageLoader.getInstance().clearMemoryCache();

        // send intent to XoClientService that we are not conversing any longer
        Intent intent = new Intent();
        intent.setAction(IntentHelper.ACTION_CONTACT_ID_IN_CONVERSATION);
        intent.putExtra(IntentHelper.EXTRA_CONTACT_ID, -1);
        getActivity().sendBroadcast(intent);
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
                ChatMessageItem item = (ChatMessageItem) mMessageListView.getChildAt(i).getTag();
                if (item != null) {
                    item.detachView();
                }
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.fragment_messaging, menu);

        // select client/group profile entry for appropriate icon
        if (mContact != null) {
            if (mContact.isGroup() && mContact.getGroupPresence() != null && mContact.getGroupPresence().isTypeNearby()) {
                getActivity().getActionBar().setTitle(getActivity().getResources().getString(R.string.nearby_text));
            } else {
                getActivity().getActionBar().setTitle(mContact.getNickname());
            }

            MenuItem clientItem = menu.findItem(R.id.menu_profile_single);
            clientItem.setVisible(mContact.isClient());

            MenuItem groupItem = menu.findItem(R.id.menu_profile_group);
            groupItem.setVisible(mContact.isGroup());

            MenuItem musicItem = menu.findItem(R.id.menu_audio_attachment_list);
            musicItem.setVisible(true);

            MenuItem createPermanentGroupItem = menu.findItem(R.id.menu_group_profile_create_permanent_group);
            boolean shouldShow = (mContact.isGroup() && mContact.getGroupPresence() != null && mContact.getGroupPresence().isTypeNearby());
            createPermanentGroupItem.setVisible(shouldShow);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        LOG.debug("onOptionsItemSelected(" + item.toString() + ")");

        switch (item.getItemId()) {
            case R.id.menu_profile_single:
                if (mContact != null) {
                    startActivity(new Intent(getActivity(), SingleProfileActivity.class)
                            .setAction(SingleProfileActivity.ACTION_SHOW)
                            .putExtra(SingleProfileActivity.EXTRA_CLIENT_CONTACT_ID, mContact.getClientContactId()));
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
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onAdapterReloadStarted(XoAdapter adapter) {
        LOG.debug("onAdapterReloadStarted()");
        mEmptyText.setText(R.string.messaging_loading);
    }

    @Override
    public void onAdapterReloadFinished(XoAdapter adapter) {
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
    public void onClientPresenceChanged(TalkClientContact contact) {}

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {}

    @Override
    public void onGroupPresenceChanged(TalkClientContact contact) {}

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) {}

    public void showAudioAttachmentList() {
        Intent intent = new Intent(getActivity(), MediaBrowserActivity.class);
        intent.putExtra(IntentHelper.EXTRA_CONTACT_ID, mContact.getClientContactId());
        startActivity(intent);
    }
}
