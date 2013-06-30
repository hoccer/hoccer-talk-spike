package com.hoccer.talk.android.adapter;

import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.hoccer.talk.android.R;
import com.hoccer.talk.android.TalkActivity;
import com.hoccer.talk.android.TalkAdapter;
import com.hoccer.talk.client.model.TalkClientContact;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ContactsAdapter extends TalkAdapter {

    protected final static long ITEM_ID_UNKNOWN = -1000;
    protected final static long ITEM_ID_CLIENT_HEADER = -1;
    protected final static long ITEM_ID_GROUP_HEADER = -2;

    protected final static int VIEW_TYPE_SEPARATOR = 0;
    protected final static int VIEW_TYPE_CLIENT    = 1;
    protected final static int VIEW_TYPE_GROUP     = 2;

    protected final static int VIEW_TYPE_COUNT = 3;

    public ContactsAdapter(TalkActivity activity) {
        super(activity);
    }

    List<TalkClientContact> mClientContacts = new ArrayList<TalkClientContact>();
    List<TalkClientContact> mGroupContacts = new ArrayList<TalkClientContact>();

    @Override
    public void reload() {
        try {
            mClientContacts = mDatabase.findAllClientContacts();
            mGroupContacts = mDatabase.findAllGroupContacts();
            notifyDataSetInvalidated();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onContactAdded(int contactId) throws RemoteException {
        try {
            TalkClientContact contact = mDatabase.findClientContactById(contactId);
            if(contact.isClient()) {
                mClientContacts.add(contact);
            }
            if(contact.isGroup()) {
                mGroupContacts.add(contact);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onContactRemoved(int contactId) throws RemoteException {
    }

    @Override
    public void onClientPresenceChanged(int contactId) throws RemoteException {
    }

    @Override
    public void onClientRelationshipChanged(int contactId) throws RemoteException {
    }

    @Override
    public void onGroupPresenceChanged(int contactId) throws RemoteException {
    }

    @Override
    public void onGroupMembershipChanged(int contactId) throws RemoteException {
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public int getCount() {
        int count = 0;

        count += mClientContacts.size();
        count += mGroupContacts.size();

        if(!mClientContacts.isEmpty()) {
            count += 1;
        }
        if(!mGroupContacts.isEmpty()) {
            count += 1;
        }

        return count;
    }

    @Override
    public Object getItem(int position) {
        int offset = 0;
        if(!mClientContacts.isEmpty()) {
            if(position == offset) {
                return "Clients";
            }
            offset += 1;
            int clientPos = position - offset;
            if(clientPos >= 0 && clientPos < mClientContacts.size()) {
                return mClientContacts.get(clientPos);
            }
            offset += mClientContacts.size();
        }
        if(!mGroupContacts.isEmpty()) {
            if(position == offset) {
                return "Groups";
            }
            offset += 1;
            int groupPos = position - offset;
            if(groupPos >= 0 && groupPos < mGroupContacts.size()) {
                return mGroupContacts.get(groupPos);
            }
            offset += mGroupContacts.size();
        }
        return "XXX";
    }

    @Override
    public int getItemViewType(int position) {
        int offset = 0;
        if(!mClientContacts.isEmpty()) {
            if(position == offset) {
                return VIEW_TYPE_SEPARATOR;
            }
            offset += 1;
            int clientPos = position - offset;
            if(clientPos >= 0 && clientPos < mClientContacts.size()) {
                return VIEW_TYPE_CLIENT;
            }
            offset += mClientContacts.size();
        }
        if(!mGroupContacts.isEmpty()) {
            if(position == offset) {
                return VIEW_TYPE_SEPARATOR;
            }
            offset += 1;
            int groupPos = position - offset;
            if(groupPos >= 0 && groupPos < mGroupContacts.size()) {
                return VIEW_TYPE_GROUP;
            }
            offset += mGroupContacts.size();
        }
        return VIEW_TYPE_SEPARATOR;
    }

    @Override
    public long getItemId(int position) {
        Object item = getItem(position);
        if(item instanceof TalkClientContact) {
            return ((TalkClientContact)item).getClientContactId();
        }

        int offset = 0;
        if(!mClientContacts.isEmpty()) {
            if(position == offset) {
                return ITEM_ID_CLIENT_HEADER;
            }
            offset += 1;
            offset += mClientContacts.size();
        }
        if(!mGroupContacts.isEmpty()) {
            if(position == offset) {
                return ITEM_ID_GROUP_HEADER;
            }
            offset += 1;
            offset += mGroupContacts.size();
        }
        return ITEM_ID_UNKNOWN;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int type = getItemViewType(position);

        View v = convertView;

        switch(type) {
        case VIEW_TYPE_CLIENT:
            if(v == null) {
                v = mInflater.inflate(R.layout.item_contact_client, null);
            }
            updateContact(v, position);
            break;
        case VIEW_TYPE_GROUP:
            if(v == null) {
                v = mInflater.inflate(R.layout.item_contact_group, null);
            }
            updateContact(v, position);
            break;
        case VIEW_TYPE_SEPARATOR:
            if(v == null) {
                v = mInflater.inflate(R.layout.item_contact_separator, null);
            }
            updateSeparator(v, position);
            break;
        default:
            v = mInflater.inflate(R.layout.item_contact_separator, null);
            break;
        }

        return v;
    }

    private void updateSeparator(View view, int position) {
        TextView separator = (TextView)view;
        separator.setText((String)getItem(position));
    }

    private void updateContact(View view, int position) {
        final TalkClientContact contact = (TalkClientContact)getItem(position);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.showContactConversation(contact);
            }
        });
        TextView nameView = (TextView)view.findViewById(R.id.contact_name);
        nameView.setText(contact.getName());
        TextView statusView = (TextView)view.findViewById(R.id.contact_status);
        statusView.setText(contact.getStatus());
        Button profileButton = (Button)view.findViewById(R.id.contact_profile_button);
        profileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.showContactProfile(contact);
            }
        });
    }

}
