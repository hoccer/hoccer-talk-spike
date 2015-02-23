package com.hoccer.xo.android.adapter;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.model.TalkGroupMembership;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.view.AvatarView;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ContactSelectionAdapter extends BaseAdapter implements IXoContactListener {

    static final Logger LOG = Logger.getLogger(ContactSelectionAdapter.class);

    private final List<TalkClientContact> mContacts;
    private final List<TalkClientContact> mSelectedContacts = new ArrayList<TalkClientContact>();

    private final Set<IContactSelectionListener> mContactSelectionListeners = new HashSet<IContactSelectionListener>();

    public interface IContactSelectionListener {
        public void onContactSelectionChanged();
    }

    public ContactSelectionAdapter() {
        mContacts = new ArrayList<TalkClientContact>();
        loadContacts();
    }

    @Override
    public int getCount() {
        return mContacts.size();
    }

    @Override
    public Object getItem(int position) {
        return mContacts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mContacts.get(position).getClientContactId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;

        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact_checked, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.contactAvatarView = (AvatarView) (convertView.findViewById(R.id.contact_icon));
            viewHolder.checkedNameTextView = (CheckedTextView) (convertView.findViewById(R.id.contact_name_checked));
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final TalkClientContact contact = mContacts.get(position);
        viewHolder.contact = contact;
        viewHolder.contactAvatarView.setContact(contact);

        if (contact.isGroup() && contact.getGroupPresence() != null && contact.getGroupPresence().isTypeNearby()) {
            viewHolder.checkedNameTextView.setText(R.string.nearby_text);
        } else {
            viewHolder.checkedNameTextView.setText(contact.getNickname());
            if (!contact.isClientRelated() && contact.isNearby()) {
                viewHolder.checkedNameTextView.setText(contact.getNickname() + " (" + parent.getContext().getString(R.string.nearby) + ")");
            }
        }

        viewHolder.checkedNameTextView.setChecked(mSelectedContacts.contains(contact));

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewHolder viewHolder = (ViewHolder) v.getTag();
                if (mSelectedContacts.contains(viewHolder.contact)) {
                    mSelectedContacts.remove(viewHolder.contact);
                } else {
                    mSelectedContacts.add(viewHolder.contact);
                }
                viewHolder.checkedNameTextView.setChecked(!viewHolder.checkedNameTextView.isChecked());

                for (IContactSelectionListener listener : mContactSelectionListeners) {
                    listener.onContactSelectionChanged();
                }
            }
        });

        return convertView;
    }

    public void registerListeners() {
        XoApplication.get().getXoClient().registerContactListener(this);
    }

    public void unregisterListeners() {
        XoApplication.get().getXoClient().unregisterContactListener(this);
    }

    private void loadContacts() {
        mContacts.clear();
        try {
            XoClientDatabase database = XoApplication.get().getXoClient().getDatabase();
            for (TalkClientContact contact : database.findAllContacts()) {
                if (shouldShow(contact)) {
                    mContacts.add(contact);
                }
            }
        } catch (SQLException e) {
            LOG.error("Could not load contacts from database", e);
        }

        List<TalkClientContact> newSelectedContacts = new ArrayList<TalkClientContact>();
        newSelectedContacts.addAll(CollectionUtils.intersection(mContacts, mSelectedContacts));
        mSelectedContacts.clear();
        mSelectedContacts.addAll(newSelectedContacts);
    }

    private static boolean shouldShow(TalkClientContact contact) {
        boolean shouldShow = false;
        if (contact.isGroup()) {
            if (contact.isGroupInvolved() && contact.isGroupExisting() && groupHasOtherContacts(contact.getGroupId())) {
                shouldShow = true;
            }
        } else if (contact.isClient()) {
            if (contact.isClientFriend() || contact.isNearby() || (contact.isClientRelated() && contact.getClientRelationship().isBlocked())) {
                shouldShow = true;
            }
        }

        return shouldShow;
    }

    private static boolean groupHasOtherContacts(String groupId) {
        try {
            return XoApplication.get().getXoClient().getDatabase().findMembershipsInGroupByState(groupId, TalkGroupMembership.STATE_JOINED).size() > 1;
        } catch (SQLException e) {
            LOG.error(e);
        }
        return false;
    }

    @Override
    public void onClientPresenceChanged(TalkClientContact contact) {
    }

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {
    }

    @Override
    public void onGroupPresenceChanged(TalkClientContact contact) {
    }

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) {
        LOG.info("onGroupMembershipChanged");
        refreshList();
    }

    public List<TalkClientContact> getSelectedContacts() {
        return mSelectedContacts;
    }

    private void refreshList() {
        loadContacts();
        Handler guiHandler = new Handler(Looper.getMainLooper());
        guiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (IContactSelectionListener listener : mContactSelectionListeners) {
                    listener.onContactSelectionChanged();
                }

                notifyDataSetChanged();
            }
        });
    }

    public void addContactSelectionListener(IContactSelectionListener l) {
        mContactSelectionListeners.add(l);
    }

    public void removeContactSelectionListener(IContactSelectionListener l) {
        mContactSelectionListeners.remove(l);
    }

    private class ViewHolder {
        public AvatarView contactAvatarView;
        public CheckedTextView checkedNameTextView;
        public TalkClientContact contact;
    }
}
