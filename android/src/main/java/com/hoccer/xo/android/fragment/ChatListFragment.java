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
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.activity.ChatActivity;
import com.hoccer.xo.android.activity.ChatsActivity;
import com.hoccer.xo.android.adapter.ChatListAdapter;
import com.hoccer.xo.android.adapter.SearchAdapter;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.util.IntentHelper;
import com.hoccer.xo.android.view.Placeholder;
import com.hoccer.xo.android.view.model.ChatItem;
import org.apache.log4j.Logger;

import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.List;

public class ChatListFragment extends SearchableListFragment implements IPagerFragment {

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
        mDatabase = XoApplication.get().getXoClient().getDatabase();
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
                ((XoActivity) getActivity()).showPairing();
            }
        });

        initAdapter();
        ListView listView = (ListView) view.findViewById(android.R.id.list);
        registerForContextMenu(listView);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        mContactsMenuItem = menu.findItem(R.id.menu_contacts);
        mMyProfileMenuItem = menu.findItem(R.id.menu_my_profile);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mAdapter != null) {
            mAdapter.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdapter != null) {
            mAdapter.onResume();
            mAdapter.loadChatItems();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mAdapter != null) {
            mAdapter.onDestroy();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        if (menuInfo != null) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            Object object = ((ChatItem) mAdapter.getItem(info.position)).getContent();
            if (object instanceof TalkClientContact) {
                MenuInflater inflater = getActivity().getMenuInflater();
                inflater.inflate(R.menu.context_menu_contacts, menu);
            } else if (object instanceof String) {
                MenuInflater inflater = getActivity().getMenuInflater();
                inflater.inflate(R.menu.context_menu_contacts, menu);
            }
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
        Object item = ((ChatItem) mAdapter.getItem(position)).getContent();
        if (item instanceof TalkClientContact) {
            clearConversationForContact((TalkClientContact) item);
        } else if (item instanceof String) {
            clearNearbyHistory();
        }
    }

    private void clearConversationForContact(TalkClientContact contact) {
        try {
            mDatabase.deleteAllMessagesFromContactId(contact.getClientContactId());
            mAdapter.requestReload();
        } catch (SQLException e) {
            LOG.error("SQLException while clearing conversation with contact " + contact.getClientContactId(), e);
        }
    }

    private void clearNearbyHistory() {
        try {
            if (mDatabase == null) {
                return;
            }
            List<TalkClientMessage> messages = mDatabase.getAllNearbyGroupMessages();
            for (TalkClientMessage message : messages) {
                message.markAsDeleted();
                mDatabase.saveClientMessage(message);
            }
            mAdapter.requestReload();
        } catch (SQLException e) {
            LOG.error("SQLException while clearing nearby history", e);
        }
    }

    private void initAdapter() {
        ChatListAdapter.Filter filter = new ChatListAdapter.Filter() {
            @Override
            public boolean shouldShow(TalkClientContact contact) {
                if (contact.isGroup()) {
                    return (contact.isKeptGroup() || (contact.isGroupJoined() && contact.isGroupExisting())) && !(contact.isNearbyGroup() || contact.isWorldwideGroup()) ;
                } else if (contact.isClient()) {
                    return contact.isKept() || contact.isFriendOrBlocked();
                }
                return false;
            }
        };

        mAdapter = new ChatListAdapter((XoActivity) getActivity(), filter);

        setListAdapter(mAdapter);
    }

    public void onGroupCreationSucceeded(int contactId) {
        try {
            TalkClientContact contact = mDatabase.findContactById(contactId);
            if (contact != null) {
                ((XoActivity) getActivity()).showContactProfile(contact);
            }
        } catch (SQLException e) {
            LOG.error("SQL error while creating group ", e);
        }
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);

        ChatItem item = ((ChatItem) listView.getItemAtPosition(position));
        if (item.getType() == ChatItem.TYPE_CLIENT_NEARBY_HISTORY || item.getType() == ChatItem.TYPE_CLIENT_HISTORY) {
            TalkClientContact contact = (TalkClientContact) item.getContent();
            showHistory(contact);
        } else if (item.getType() == ChatItem.TYPE_RELATED) {
            TalkClientContact contact = (TalkClientContact) item.getContent();
            showChat(contact);
        } else if (item.getType() == ChatItem.TYPE_GROUP_NEARBY_HISTORY) {
            showNearbyGroupHistory();
        } else if (item.getType() == ChatItem.TYPE_GROUP_WORLDWIDE_HISTORY) {
            showWorldwideGroupHistory();
        }
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
    public void onPageSelected() {
    }

    @Override
    public void onPageUnselected() {
        leaveSearchMode();
    }

    @Override
    public void onPageResume() {
    }

    @Override
    public void onPagePause() {
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }
}
