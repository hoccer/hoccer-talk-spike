package com.hoccer.xo.android.fragment;

import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.adapter.NearbyChatListAdapter;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.base.XoListFragment;
import com.hoccer.xo.android.util.ColorSchemeManager;
import com.hoccer.xo.android.view.Placeholder;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;


public class NearbyChatListFragment extends XoListFragment implements IXoContactListener {
    private static final Logger LOG = Logger.getLogger(NearbyChatListFragment.class);
    private static final Placeholder PLACEHOLDER = new Placeholder(
            R.drawable.placeholder_nearby,
            R.drawable.placeholder_nearby_point,
            R.string.placeholder_nearby_text);

    private NearbyChatListAdapter mNearbyAdapter;
    private TalkClientContact mCurrentNearbyGroup;
    private ListView mContactList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);
        mContactList = (ListView) view.findViewById(android.R.id.list);

        PLACEHOLDER.applyToView(view);

        TextView placeholderText = (TextView) view.findViewById(R.id.tv_contacts_placeholder);
        placeholderText.setMovementMethod(LinkMovementMethod.getInstance());
        setPlaceholderText(placeholderText);

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        getXoActivity().getXoClient().unregisterContactListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        createAdapter();

        if (!isNearbyConversationPossible(mCurrentNearbyGroup)) {
            deactivateNearbyChat();
        }

        getXoActivity().getXoClient().registerContactListener(this);
        if (mNearbyAdapter != null) {
            mNearbyAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDestroy() {
        if (mNearbyAdapter != null) {
            mNearbyAdapter.unregisterListeners();
        }
        destroyAdapter();
        super.onDestroy();
    }

    private void setPlaceholderText(TextView placeholderText) {
        String anchorName = getString(R.string.tutorial_nearby_anchor);
        String link = "<a href=\"" + getResources().getString(R.string.link_tutorial) + "#" + anchorName + "\">" +
                getResources().getString(R.string.placeholder_nearby_link_text) + "</a>";
        String text = String.format(getString(R.string.placeholder_nearby_text), link);
        placeholderText.setText(Html.fromHtml(text));
    }

    private boolean isNearbyConversationPossible(TalkClientContact groupContact) {
        if (mCurrentNearbyGroup == null || groupContact == null) {
            return false;
        }
        try {
            if (groupContact.getGroupId().equals(mCurrentNearbyGroup.getGroupId())) {
                int groupMembershipCount = getXoDatabase().findGroupMemberCountForGroup(groupContact);
                return (groupMembershipCount > 1);
            }
        } catch (SQLException e) {
            LOG.error("SQL Exception while retrieving current nearby group: ", e);
        }
        return false;
    }

    private
    @Nullable
    TalkClientContact getActiveNearbyGroup() {
        XoActivity activity = getXoActivity();
        if (activity != null) {
            return activity.getXoClient().getCurrentNearbyGroup();
        }
        return null;
    }

    private void deactivateNearbyChat() {
        mCurrentNearbyGroup = null;
    }

    public void shutdownNearbyChat() {
        if (mActivity != null) {
            deactivateNearbyChat();
        }
    }

    private void createAdapter() {
        if (mNearbyAdapter == null) {
            mNearbyAdapter = new NearbyChatListAdapter(getXoDatabase(), getXoActivity(), mCurrentNearbyGroup);
            mNearbyAdapter.registerListeners();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setListAdapter(mNearbyAdapter);
                }
            });
        }
    }

    private void destroyAdapter() {
        if (mNearbyAdapter != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setListAdapter(null);
                }
            });
            mNearbyAdapter.unregisterListeners();
            mNearbyAdapter = null;
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        if (l == mContactList) {
            LOG.debug("onListItemClick(contactList," + position + ")");
            Object item = mContactList.getItemAtPosition(position);
            if (item instanceof TalkClientContact) {
                TalkClientContact contact = (TalkClientContact) item;
                getXoActivity().showContactConversation(contact);
            }
        }
    }

    @Override
    public void onContactAdded(TalkClientContact contact) {
        LOG.info("onContactAdded()" + contact.getClientContactId());
    }

    @Override
    public void onContactRemoved(TalkClientContact contact) {

    }

    @Override
    public void onClientPresenceChanged(TalkClientContact contact) {
        LOG.info("onClientPresenceChanged()" + contact.getClientContactId());
    }

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {

    }

    @Override
    public void onGroupPresenceChanged(TalkClientContact contact) {
        if (mCurrentNearbyGroup != null && contact.getGroupId().equals(mCurrentNearbyGroup.getGroupId())) {
            if (!isNearbyConversationPossible(contact)) {
                deactivateNearbyChat();
            }
        }
    }

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) {
        TalkClientContact currentNearbyGroup = getActiveNearbyGroup();
        if (currentNearbyGroup != null) {
            mCurrentNearbyGroup = currentNearbyGroup;
        }
        if (mCurrentNearbyGroup != null && contact.getGroupId().equals(mCurrentNearbyGroup.getGroupId())) {
            if (!isNearbyConversationPossible(contact)) {
                deactivateNearbyChat();
            }
        }
    }
}
