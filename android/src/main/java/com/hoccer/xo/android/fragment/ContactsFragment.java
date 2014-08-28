package com.hoccer.xo.android.fragment;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.*;
import android.widget.*;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.client.model.TalkClientSmsToken;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.activity.MessagingActivity;
import com.hoccer.xo.android.adapter.BetterContactsAdapter;
import com.hoccer.xo.android.adapter.OnItemCountChangedListener;
import com.hoccer.xo.android.adapter.SearchAdapter;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.util.ColorSchemeManager;
import com.hoccer.xo.android.dialog.TokenDialog;
import com.hoccer.xo.android.view.model.BaseContactItem;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.List;

/**
 * Fragment that shows a list of contacts
 * <p/>
 * This currently shows only contact data but should also be able to show
 * recent conversations for use as a "conversations" view.
 */
public class ContactsFragment extends SearchableListFragment implements OnItemCountChangedListener {

    private static final Logger LOG = Logger.getLogger(ContactsFragment.class);

    private XoClientDatabase mDatabase;
    private BetterContactsAdapter mAdapter;
    private WeakReference<SearchAdapter> mSearchAdapterReference = new WeakReference<SearchAdapter>(null);

    private TextView mPlaceholderText;

    private ImageView mPlaceholderImageFrame;
    private ImageView mPlaceholderImage;
    private MenuItem mPairWithContactMenuItem;
    private MenuItem mAddGroupMenuItem;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDatabase = XoApplication.getXoClient().getDatabase();
        initContactListAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);
        mPlaceholderImageFrame = (ImageView) view.findViewById(R.id.iv_contacts_placeholder_frame);
        mPlaceholderImage= (ImageView) view.findViewById(R.id.iv_contacts_placeholder);
        mPlaceholderText = (TextView) view.findViewById(R.id.tv_contacts_placeholder);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mPlaceholderImageFrame.setBackground(getResources().getDrawable(R.drawable.placeholder_chats));
            mPlaceholderImage.setBackground(ColorSchemeManager.getRepaintedDrawable(getActivity(), R.drawable.placeholder_chats_head, true));
        } else {
            mPlaceholderImageFrame.setBackgroundDrawable(getResources().getDrawable(R.drawable.placeholder_chats));
            mPlaceholderImage.setBackgroundDrawable(ColorSchemeManager.getRepaintedDrawable(getActivity(), R.drawable.placeholder_chats_head, true));
        }

        ListView contactList = (ListView) view.findViewById(android.R.id.list);
        registerForContextMenu(contactList);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        onItemCountChanged(mAdapter.getCount());
        setListAdapter(mAdapter);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
//        inflater.inflate(R.menu.fragment_contacts, menu);
        mAddGroupMenuItem = menu.findItem(R.id.menu_new_group);
        mPairWithContactMenuItem = menu.findItem(R.id.menu_pair);
    }

    @Override
    public void onPause() {
        super.onPause();
        mAdapter.onPause();
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
        if(menuInfo != null) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            Object object = ((BaseContactItem) mAdapter.getItem(info.position)).getContent();
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
        mAddGroupMenuItem.setVisible(false);
        mPairWithContactMenuItem.setVisible(false);
        if (mSearchAdapterReference.get() == null) {
            mSearchAdapterReference = new WeakReference<SearchAdapter>(new SearchAdapter(mAdapter));

        }
    }

    @Override
    protected void onSearchModeDisabled() {
        if (mAddGroupMenuItem != null) {
            mAddGroupMenuItem.setVisible(true);
        }

        if (mPairWithContactMenuItem != null) {
            mPairWithContactMenuItem.setVisible(true);
        }
    }

    private void deleteChatHistoryAt(int position) {
        Object item = ((BaseContactItem) mAdapter.getItem(position)).getContent();
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

    private void initContactListAdapter() {
        BetterContactsAdapter.Filter filter = new BetterContactsAdapter.Filter() {
            @Override
            public boolean shouldShow(TalkClientContact contact) {
                if (contact.isGroup()) {
                    if (contact.isGroupInvolved() && contact.isGroupExisting() && !(contact.getGroupPresence() != null && (contact.getGroupPresence().isTypeNearby() || contact.getGroupPresence().isKept()))) {
                        return true;
                    }
                } else if (contact.isClient()) {
                    if (contact.isClientRelated() && (contact.getClientRelationship().isFriend() || contact.getClientRelationship()
                            .isBlocked())) {
                        return true;
                    }
                } else if (contact.isEverRelated()) {
                    return true;
                }
                return false;
            }
        };

        mAdapter = new BetterContactsAdapter((XoActivity) getActivity(), filter);
        mAdapter.onCreate();
        mAdapter.setOnItemCountChangedListener(this);
    }

    public void onGroupCreationSucceeded(int contactId) {
        try {
            TalkClientContact contact = XoApplication.getXoClient().getDatabase().findClientContactById(contactId);
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

        Object item = ((BaseContactItem)listView.getItemAtPosition(position)).getContent();
        if (item instanceof TalkClientContact) {
            TalkClientContact contact = (TalkClientContact) item;
            if (contact.isGroup() && contact.isGroupInvited()) {
                ((XoActivity) getActivity()).showContactProfile(contact);
            } else {
                ((XoActivity) getActivity()).showContactConversation(contact);
            }
        }
        if (item instanceof TalkClientSmsToken) {
            TalkClientSmsToken token = (TalkClientSmsToken) item;
            new TokenDialog((XoActivity) getActivity(), token).show(getActivity().getFragmentManager(),
                    "TokenDialog");
        }
        if (item instanceof String) { // item can only be an instance of string if the user pressed on the nearby saved option
            Intent intent = new Intent(getActivity(), MessagingActivity.class);
            intent.putExtra(MessagingActivity.EXTRA_NEARBY_ARCHIVE, true);
            startActivity(intent);
        }
    }

    @Override
    public void onItemCountChanged(int count) {
        if (count > 0) {
            hidePlaceholder();
        } else {
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
