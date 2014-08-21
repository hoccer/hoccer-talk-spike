package com.hoccer.xo.android.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.*;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.activity.MediaBrowserActivity;
import com.hoccer.xo.android.activity.MessagingActivity;
import com.hoccer.xo.android.adapter.ChatAdapter;
import com.hoccer.xo.android.base.IMessagingFragmentManager;
import com.hoccer.xo.android.base.XoAdapter;
import com.hoccer.xo.android.base.XoListFragment;
import com.hoccer.xo.android.content.ContentRegistry;
import com.hoccer.xo.android.gesture.Gestures;
import com.hoccer.xo.android.gesture.MotionInterpreter;
import com.hoccer.xo.android.util.IntentHelper;
import com.hoccer.xo.android.util.ThumbnailManager;
import com.hoccer.xo.release.R;
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
                mContact = XoApplication.getXoClient().getDatabase().findClientContactById(clientContactId);
            } catch (SQLException e) {
                LOG.error("sql error", e);
                return;
            }

            // log error if the contact was not found
            if(mContact == null) {
                LOG.error("Client contact with id '" + clientContactId + "' does not exist");
                return;
            }
        } else {
            LOG.error("MessagingFragment requires contactId as argument.");
            return;
        }

        mMotionInterpreter = new MotionInterpreter(Gestures.Transaction.SHARE, getXoActivity(), mCompositionFragment);

        createCompositionFragment();
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

        if (mContact.isDeleted()) {
            getActivity().finish();
        }

        setHasOptionsMenu(true);
        mMessageListView = getListView();

        mAdapter = new ChatAdapter(mMessageListView, getXoActivity(), mContact);
        mAdapter.setAdapterReloadListener(this);
        mAdapter.onCreate();

        mMessageListView.setAdapter(mAdapter);

        configureMotionInterpreterForContact(mContact);
        XoApplication.getXoClient().registerContactListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mAdapter.onPause();
        mMotionInterpreter.deactivate();
        XoApplication.getXoClient().unregisterContactListener(this);
        ImageLoader.getInstance().clearMemoryCache();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mAdapter != null) {
            mAdapter.onDestroy();
            mAdapter = null;
        }

        ThumbnailManager.getInstance(getXoActivity()).clearCache();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        // select client/group profile entry for appropriate icon
        if (mContact != null) {
            MenuItem clientItem = menu.findItem(R.id.menu_profile_single);
            clientItem.setVisible(mContact.isClient());
            MenuItem groupItem = menu.findItem(R.id.menu_profile_group);
            groupItem.setVisible(mContact.isGroup());
            menu.findItem(R.id.menu_audio_attachment_list).setVisible(true);
            getActivity().getActionBar().setTitle(mContact.getName());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        LOG.debug("onOptionsItemSelected(" + item.toString() + ")");

        IMessagingFragmentManager mgr = (IMessagingFragmentManager) getActivity();
        switch (item.getItemId()) {
            case R.id.menu_profile_single:
                if (mContact != null && mgr != null) {
                    mgr.showSingleProfileFragment(mContact.getClientContactId());
                }
                break;
            case R.id.menu_profile_group:
                if (mContact != null && mgr != null) {
                    mgr.showGroupProfileFragment(mContact.getClientContactId());
                }
                break;
            case R.id.menu_audio_attachment_list:
                if (mContact != null) {
                    showAudioAttachmentList();
                }
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
        if (contact != null && (contact.isNearby() || (contact.isGroup() && contact.getGroupPresence().isTypeNearby()))) {
            mMotionInterpreter.activate();
        } else {
            mMotionInterpreter.deactivate();
        }
    }

    @Override
    public void onContactAdded(TalkClientContact contact) {
        // do nothing
    }

    @Override
    public void onContactRemoved(TalkClientContact contact) {
        if (mContact != null && mContact.getClientContactId() == contact.getClientContactId()) {
            getActivity().finish();
        }
    }

    @Override
    public void onClientPresenceChanged(TalkClientContact contact) {
        // do nothing
    }

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {
        // do nothing
    }

    @Override
    public void onGroupPresenceChanged(TalkClientContact contact) {
        // do nothing
    }

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) {
        // do nothing
    }

    public void applicationWillEnterBackground() {
        if (mContact.isGroup() && mContact.getGroupPresence().isTypeNearby()) {
            getActivity().finish();
        } else if (mContact.isClient() && mContact.isNearby()) {
            getActivity().finish();
        }
    }

    public void showAudioAttachmentList() {
        Intent intent = new Intent(getActivity(), MediaBrowserActivity.class);
        intent.putExtra(IntentHelper.EXTRA_CONTACT_ID, mContact.getClientContactId());
        startActivity(intent);
    }

    @Override
    public void onAttachmentSelected(IContentObject co) {
        mCompositionFragment.onAttachmentSelected(co);
    }
}
