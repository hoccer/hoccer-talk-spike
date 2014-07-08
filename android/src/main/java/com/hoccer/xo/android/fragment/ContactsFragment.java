package com.hoccer.xo.android.fragment;

import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.client.model.TalkClientSmsToken;
import com.hoccer.xo.android.activity.NearbyHistoryMessagingActivity;
import com.hoccer.xo.android.adapter.ContactsAdapter;
import com.hoccer.xo.android.adapter.OnItemCountChangedListener;
import com.hoccer.xo.android.adapter.RichContactsAdapter;
import com.hoccer.xo.android.base.XoListFragment;
import com.hoccer.xo.android.util.ColorSchemeManager;
import com.hoccer.xo.android.dialog.TokenDialog;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.sql.SQLException;
import java.util.List;

/**
 * Fragment that shows a list of contacts
 * <p/>
 * This currently shows only contact data but should also be able to show
 * recent conversations for use as a "conversations" view.
 */
public class ContactsFragment extends XoListFragment implements OnItemCountChangedListener {

    private static final Logger LOG = Logger.getLogger(ContactsFragment.class);

    private XoClientDatabase mDatabase;
    private ContactsAdapter mAdapter;

    private ListView mContactList;

    private TextView mPlaceholderText;

    private ImageView mPlaceholderImageFrame;
    private ImageView mPlaceholderImage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDatabase = getXoActivity().getXoDatabase();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        LOG.debug("onCreateView()");
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);
        mContactList = (ListView) view.findViewById(android.R.id.list);
        mPlaceholderImageFrame = (ImageView) view.findViewById(R.id.iv_contacts_placeholder_frame);
        mPlaceholderImageFrame.setBackgroundDrawable(getResources().getDrawable(R.drawable.placeholder_chats));
        mPlaceholderImage= (ImageView) view.findViewById(R.id.iv_contacts_placeholder);
        mPlaceholderImage.setBackgroundDrawable(ColorSchemeManager.fillBackground(getXoActivity(), R.drawable.placeholder_chats_head, true));
        mPlaceholderText = (TextView) view.findViewById(R.id.tv_contacts_placeholder);

        registerForContextMenu(mContactList);
        return view;
    }

    @Override
    public void onPause() {
        LOG.debug("onPause()");
        super.onPause();
        if (mAdapter != null) {
            mAdapter.onPause();
            mAdapter.onDestroy();
            mAdapter = null;
        }
    }

    @Override
    public void onResume() {
        LOG.debug("onResume()");
        super.onResume();
        initContactListAdapter();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Object object = mAdapter.getItem(info.position);
        if (object instanceof TalkClientContact) {
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.context_menu_contacts, menu);
        } else if(object instanceof String) {
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.context_menu_contacts, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.menu_clear_conversation:
                deleteChatHistoryAt(info.position);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void deleteChatHistoryAt(int position) {
        Object item = mAdapter.getItem(position);
        if(item instanceof TalkClientContact) {
            clearConversationForContact((TalkClientContact) item);
        } else if(item instanceof String) {
            clearNearbyHistory();
        }
    }

    private void clearConversationForContact(TalkClientContact contact) {
        try {
            mDatabase.deleteAllMessagesFromContactId(contact.getClientContactId());
            mAdapter.notifyDataSetChanged();
        } catch (SQLException e) {
            LOG.error("SQLException while clearing conversation with contact " + contact.getClientContactId(), e);
        }
    }

    private void clearNearbyHistory() {
        try {
            List<TalkClientMessage> messages = mDatabase.getAllNearbyGroupMessages();
            for(TalkClientMessage message : messages) {
                message.markAsDeleted();
                mDatabase.saveClientMessage(message);
            }
            mAdapter.notifyDataSetChanged();
        } catch (SQLException e) {
            LOG.error("SQLException while clearing nearby history", e);
        }
    }

    private void initContactListAdapter() {
        if (mAdapter == null) {
            mAdapter = new RichContactsAdapter(getXoActivity(), true);
            mAdapter.onCreate();
            // filter out never-related contacts (which we know only via groups)
            mAdapter.setFilter(new ContactsAdapter.Filter() {
                @Override
                public boolean shouldShow(TalkClientContact contact) {
                    if (contact.isGroup()) {
                        if (contact.isGroupInvolved() && contact.isGroupExisting() && !contact.getGroupPresence().isTypeNearby()) {
                            return true;
                        }
                    } else if (contact.isClient()) {
                        if (contact.isClientRelated() && (contact.getClientRelationship().isFriend() || contact.getClientRelationship().isBlocked())) {
                            return true;
                        }
                    } else if (contact.isEverRelated()) {
                        return true;
                    }
                    return false;
                }
            });

            mAdapter.setOnItemCountChangedListener(this);
            mAdapter.requestReload();
            mContactList.setAdapter(mAdapter);
            onItemCountChanged(mAdapter.getCount());
        }
        mAdapter.requestReload();
        mAdapter.onResume();
    }

    public void onGroupCreationSucceeded(int contactId) {
        LOG.debug("onGroupCreationSucceeded(" + contactId + ")");
        try {
            TalkClientContact contact = getXoDatabase().findClientContactById(contactId);
            if (contact != null) {
                getXoActivity().showContactProfile(contact);
            }
        } catch (SQLException e) {
            e.printStackTrace();
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
                if (contact.isGroup() && contact.isGroupInvited()) {
                    getXoActivity().showContactProfile(contact);
                } else {
                    getXoActivity().showContactConversation(contact);
                }
            }
            if (item instanceof TalkClientSmsToken) {
                TalkClientSmsToken token = (TalkClientSmsToken) item;
                new TokenDialog(getXoActivity(), token).show(getXoActivity().getFragmentManager(),
                        "TokenDialog");
            }
            if (item instanceof String) { // item can only be an instance of string if the user pressed on the nearby saved option
                Intent intent = new Intent(getXoActivity(), NearbyHistoryMessagingActivity.class);
                startActivity(intent);
            }
        }
    }

    @Override
    public void onItemCountChanged(int count) {
        if (count > 0) {
            hidePlaceholder();
        } else if (count < 1) {
            showPlaceholder();
        }
    }

    private void showPlaceholder() {
        mPlaceholderImageFrame.setVisibility(View.VISIBLE);
        mPlaceholderImage.setVisibility(View.VISIBLE);
        mPlaceholderText.setVisibility(View.VISIBLE);
    }

    private void hidePlaceholder() {
        mPlaceholderImageFrame.setVisibility(View.GONE);
        mPlaceholderImage.setVisibility(View.GONE);
        mPlaceholderText.setVisibility(View.GONE);
    }
}
