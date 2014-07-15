package com.hoccer.xo.android.fragment;

import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.widget.RelativeLayout;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.adapter.NearbyContactsAdapter;
import com.hoccer.xo.android.base.XoListFragment;
import com.hoccer.xo.release.R;
import com.sun.javafx.binding.StringFormatter;

import org.apache.log4j.Logger;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.sql.SQLException;
import java.util.List;


public class NearbyContactsFragment extends XoListFragment implements IXoContactListener {
    private static final Logger LOG = Logger.getLogger(NearbyContactsFragment.class);
    private TalkClientContact mCurrentNearbyGroup;
    private NearbyContactsAdapter mNearbyAdapter;
    private ListView mContactList;
    private ImageView mPlaceholderImage;
    private TextView mPlaceholderText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);
        mContactList = (ListView) view.findViewById(android.R.id.list);
        mPlaceholderImage = (ImageView) view.findViewById(R.id.iv_contacts_placeholder);
        mPlaceholderImage.setImageResource(R.drawable.placeholder_nearby);
        mPlaceholderText = (TextView) view.findViewById(R.id.tv_contacts_placeholder);
        mPlaceholderText.setMovementMethod(LinkMovementMethod.getInstance());
        setPlaceholderText();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mCurrentNearbyGroup == null) {
            if (isNearbyConversationPossible()) {
                activateNearbyChat();
            } else {
                deactivateNearbyChat();
            }
        }
        getXoActivity().getXoClient().registerContactListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (mNearbyAdapter != null) {
            mNearbyAdapter.unregisterListeners();
        }
        super.onDestroy();
    }

    private void setPlaceholderText() {
        String link = "<a href=\"" + getResources().getString(R.string.link_tutorial) + "#nearby\">"
                + getResources().getString(R.string.placeholder_nearby_link_text) + "</a>";
        String text = String.format(getString(R.string.placeholder_nearby_text), link);

        mPlaceholderText.setText(Html.fromHtml(text));
    }

    private boolean isNearbyConversationPossible() {
        try {
            final List<TalkClientContact> allNearbyContacts = getXoDatabase().findAllNearbyContacts();
            return (allNearbyContacts.size() > 0);
        } catch (SQLException e) {
            LOG.error("SQL Exception while retrieving current nearby group: ", e);
        }
        return false;
    }

    private void activateNearbyChat() {
        mCurrentNearbyGroup = getXoActivity().getXoClient().getCurrentNearbyGroup();
        createAdapter();
        hidePlaceholder();
    }

    private void deactivateNearbyChat() {
        showPlaceholder();
        mCurrentNearbyGroup = null;
    }

    private void createAdapter() {
        mNearbyAdapter = new NearbyContactsAdapter(getXoDatabase(), getXoActivity());
        mNearbyAdapter.retrieveDataFromDb();
        mNearbyAdapter.registerListeners();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setListAdapter(mNearbyAdapter);
            }
        });
    }

    private void updateViews() {
        if (mNearbyAdapter != null) {
            mContactList.post(new Runnable() {
                @Override
                public void run() {
                    mNearbyAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    public void showPlaceholder() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPlaceholderImage.setVisibility(View.VISIBLE);
                mPlaceholderText.setVisibility(View.VISIBLE);
                mContactList.setVisibility(View.GONE);
            }
        });
    }

    private void hidePlaceholder() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPlaceholderImage.setVisibility(View.GONE);
                mPlaceholderText.setVisibility(View.GONE);
                mContactList.setVisibility(View.VISIBLE);
            }
        });
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

    }

    @Override
    public void onContactRemoved(TalkClientContact contact) {

    }

    @Override
    public void onClientPresenceChanged(TalkClientContact contact) {

    }

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {

    }

    @Override
    public void onGroupPresenceChanged(TalkClientContact contact) { // enter first
        if (contact.isGroup()) {
            if (mCurrentNearbyGroup == null || mCurrentNearbyGroup != contact) {
                if (isNearbyConversationPossible()) {
                    activateNearbyChat();
                    updateViews();
                }
            }
        }
    }

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) { // enter - second
        updateViews();
        if (!isNearbyConversationPossible()) {
            deactivateNearbyChat();
        }
    }
}
