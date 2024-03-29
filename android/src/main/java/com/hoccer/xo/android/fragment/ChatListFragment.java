package com.hoccer.xo.android.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.*;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.model.TalkEnvironment;
import com.hoccer.talk.model.TalkGroupMembership;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.activity.ChatActivity;
import com.hoccer.xo.android.adapter.ChatListAdapter;
import com.hoccer.xo.android.adapter.SearchAdapter;
import com.hoccer.xo.android.base.FlavorBaseActivity;
import com.hoccer.xo.android.base.SearchablePagerListFragment;
import com.hoccer.xo.android.util.IntentHelper;
import com.hoccer.xo.android.view.Placeholder;
import com.hoccer.xo.android.view.model.*;
import com.hoccer.xo.android.view.model.ContactChatListItem;
import com.hoccer.xo.android.view.model.WorldwideGroupHistoryChatListItem;
import org.apache.log4j.Logger;

import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.List;

public class ChatListFragment extends SearchablePagerListFragment {

    private static final Logger LOG = Logger.getLogger(ChatListFragment.class);

    private static final Placeholder PLACEHOLDER = new Placeholder(R.drawable.placeholder_chats, R.string.placeholder_conversations_text);

    private XoClientDatabase mDatabase;
    private ChatListAdapter mAdapter;
    private WeakReference<SearchAdapter> mSearchAdapterReference = new WeakReference<SearchAdapter>(null);

    private MenuItem mMyProfileMenuItem;
    private MenuItem mContactsMenuItem;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDatabase = XoApplication.get().getClient().getDatabase();

        mAdapter = createAdapter();
        mAdapter.loadChatItems();
        mAdapter.registerListeners();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contacts, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        PLACEHOLDER.applyToView(view, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((FlavorBaseActivity) getActivity()).showPairing();
            }
        });

        setListAdapter(mAdapter);
        registerForContextMenu(getListView());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        mContactsMenuItem = menu.findItem(R.id.menu_contacts);
        mMyProfileMenuItem = menu.findItem(R.id.menu_my_profile);
    }

    @Override
    public void onResume() {
        super.onResume();
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mAdapter.unregisterListeners();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        if (menuInfo != null) {
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

    @Override
    protected ListAdapter searchInAdapter(String query) {
        return mSearchAdapterReference.get().query(query);
    }

    @Override
    protected void onSearchModeEnabled() {
        mContactsMenuItem.setVisible(false);
        mMyProfileMenuItem.setVisible(false);
        if (mSearchAdapterReference.get() == null) {
            mSearchAdapterReference = new WeakReference<SearchAdapter>(new SearchAdapter(mAdapter));

        }
    }

    @Override
    protected void onSearchModeDisabled() {
        if (mContactsMenuItem != null) {
            mContactsMenuItem.setVisible(true);
        }

        if (mMyProfileMenuItem != null) {
            mMyProfileMenuItem.setVisible(true);
        }
    }

    private void deleteChatHistoryAt(int position) {
        ChatListItem item = ((ChatListItem) mAdapter.getItem(position));
        if (item instanceof ContactChatListItem) {
            clearConversationForContact(((ContactChatListItem) item).getContact());
        } else if (item instanceof NearbyGroupHistoryChatListItem) {
            clearNearbyGroupHistory();
        } else if (item instanceof WorldwideGroupHistoryChatListItem) {
            clearWorldwideGroupHistory();
        }
    }

    private void clearConversationForContact(TalkClientContact contact) {
        try {
            mDatabase.deleteAllMessagesFromContactId(contact.getClientContactId());
            mAdapter.loadChatItems();
        } catch (SQLException e) {
            LOG.error("SQLException while clearing conversation with contact " + contact.getClientContactId(), e);
        }
    }

    private void clearWorldwideGroupHistory() {
        try {
            List<TalkClientMessage> messages = mDatabase.getAllWorldwideGroupMessages();
            deleteMessages(messages);
        } catch (SQLException e) {
            LOG.error("SQLException while clearing worldwide group history", e);
        }
    }

    private void clearNearbyGroupHistory() {
        try {
            List<TalkClientMessage> messages = mDatabase.getAllNearbyGroupMessages();
            deleteMessages(messages);
        } catch (SQLException e) {
            LOG.error("SQLException while clearing nearby group history", e);
        }
    }

    private void deleteMessages(List<TalkClientMessage> messages) throws SQLException {
        for (TalkClientMessage message : messages) {
            message.markAsDeleted();
            mDatabase.saveClientMessage(message);
        }
        mAdapter.loadChatItems();
    }

    private ChatListAdapter createAdapter() {
        ChatListAdapter.Filter filter = new ChatListAdapter.Filter() {
            @Override
            public boolean shouldShow(TalkClientContact contact) {
                if (contact.isGroup()) {
                    return contact.isKeptGroup() || contact.isGroupJoined() && contact.isGroupExisting();
                } else if (contact.isClient()) {
                    return !contact.isSelf() && ((contact.isWorldwide() && !isSuspendedGroupMember(contact)) || contact.isKept() || contact.isFriendOrBlocked());
                }
                return false;
            }
        };

        return new ChatListAdapter((FlavorBaseActivity) getActivity(), filter);
    }

    private boolean isSuspendedGroupMember(TalkClientContact contact) {
        TalkClientContact worldwideGroup = XoApplication.get().getClient().getCurrentWorldwideGroup();
        if (worldwideGroup != null) {
            try {
                TalkGroupMembership groupMembership = mDatabase.findMembershipInGroupByClientId(worldwideGroup.getGroupId(), contact.getClientId());
                if (groupMembership == null || groupMembership.isSuspended()) {
                    return true;
                }
            } catch (SQLException e) {
                LOG.error("SQL error", e);
            }
        }

        return false;
    }

    public void onGroupCreationSucceeded(int contactId) {
        try {
            TalkClientContact contact = mDatabase.findContactById(contactId);
            if (contact != null) {
                ((FlavorBaseActivity) getActivity()).showContactProfile(contact);
            }
        } catch (SQLException e) {
            LOG.error("SQL error while creating group ", e);
        }
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);

        ChatListItem item = ((ChatListItem) listView.getItemAtPosition(position));
        if (shouldShowChat(item)) {
            TalkClientContact contact = ((ContactChatListItem) item).getContact();
            showChat(contact);
        } else if (shouldShowContactHistory(item)) {
            TalkClientContact contact = ((ContactChatListItem) item).getContact();
            showHistory(contact);
        } else if (item instanceof NearbyGroupHistoryChatListItem) {
            showNearbyGroupHistory();
        } else if (item instanceof WorldwideGroupHistoryChatListItem) {
            showWorldwideGroupHistory();
        }
    }

    private boolean shouldShowChat(ChatListItem item) {
        if (item instanceof ContactChatListItem) {
            TalkClientContact contact = ((ContactChatListItem) item).getContact();
            return contact.isInEnvironment()
                    || contact.isEnvironmentGroup()
                    || contact.isClientFriend()
                    || contact.isGroupJoined();
        }
        return false;
    }

    private boolean shouldShowContactHistory(ChatListItem item) {
        if (item instanceof ContactChatListItem) {
            ContactChatListItem contactChatItem = (ContactChatListItem) item;
            TalkClientContact contact = contactChatItem.getContact();
            return contact.isKept() || contact.isKeptGroup();
        }
        return false;
    }

    public void showChat(TalkClientContact contact) {
        Intent intent = new Intent(getActivity(), ChatActivity.class);
        intent.putExtra(IntentHelper.EXTRA_CONTACT_ID, contact.getClientContactId());
        startActivity(intent);
    }

    private void showHistory(TalkClientContact contact) {
        Intent intent = new Intent(getActivity(), ChatActivity.class);
        intent.putExtra(IntentHelper.EXTRA_CONTACT_ID, contact.getClientContactId());
        intent.putExtra(ChatActivity.EXTRA_CLIENT_HISTORY, true);
        startActivity(intent);
    }

    private void showNearbyGroupHistory() {
        Intent intent = new Intent(getActivity(), ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_ENVIRONMENT_GROUP_HISTORY, TalkEnvironment.TYPE_NEARBY);
        startActivity(intent);
    }

    private void showWorldwideGroupHistory() {
        Intent intent = new Intent(getActivity(), ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_ENVIRONMENT_GROUP_HISTORY, TalkEnvironment.TYPE_WORLDWIDE);
        startActivity(intent);
    }

    @Override
    public View getCustomTabView(Context context) {
        return null;
    }

    @Override
    public String getTabName(Resources resources) {
        return resources.getString(R.string.chats_tab_name);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    @Override
    public void onPageSelected() {
    }

    @Override
    public void onPageUnselected() {
        leaveSearchMode();
    }
}
