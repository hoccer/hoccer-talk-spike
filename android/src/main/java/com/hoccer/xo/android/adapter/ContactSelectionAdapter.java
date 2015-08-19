package com.hoccer.xo.android.adapter;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.profile.client.ClientProfileActivity;
import com.hoccer.xo.android.profile.group.GroupProfileActivity;
import com.hoccer.xo.android.view.avatar.AvatarView;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.*;

public class ContactSelectionAdapter extends BaseAdapter implements IXoContactListener {

    static final Logger LOG = Logger.getLogger(ContactSelectionAdapter.class);

    private final List<TalkClientContact> mContacts = new ArrayList<TalkClientContact>();
    private List<TalkClientContact> mSelectedContacts = new ArrayList<TalkClientContact>();

    private final Set<IContactSelectionListener> mContactSelectionListeners = new HashSet<IContactSelectionListener>();
    private final Context mContext;

    private Filter mFilter;

    public interface IContactSelectionListener {
        public void onContactSelectionChanged(int count);
    }

    public ContactSelectionAdapter(Context context, Filter filter) {
        mContext = context;
        mFilter = filter;
        setContacts(loadContacts());
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

        final TalkClientContact contact = mContacts.get(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact_select, parent, false);

            viewHolder = new ViewHolder();

            updateAvatarView(convertView, contact);

            viewHolder.checkedNameTextView = (CheckedTextView) (convertView.findViewById(R.id.contact_name_checked));
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        if (contact.isNearbyGroup()) {
            viewHolder.checkedNameTextView.setText(R.string.all_nearby);
        } else if (contact.isWorldwideGroup()) {
            viewHolder.checkedNameTextView.setText(R.string.all_worldwide);
        } else {
            viewHolder.checkedNameTextView.setText(contact.getNickname());
        }

        viewHolder.checkedNameTextView.setChecked(mSelectedContacts.contains(contact));

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewHolder viewHolder = (ViewHolder) v.getTag();
                if (mSelectedContacts.contains(contact)) {
                    mSelectedContacts.remove(contact);
                } else {
                    mSelectedContacts.add(contact);
                }
                viewHolder.checkedNameTextView.setChecked(!viewHolder.checkedNameTextView.isChecked());

                for (IContactSelectionListener listener : mContactSelectionListeners) {
                    listener.onContactSelectionChanged(mSelectedContacts.size());
                }
            }
        });

        return convertView;
    }

    private void updateAvatarView(View convertView, final TalkClientContact contact) {
        AvatarView avatarView = (AvatarView) convertView.findViewById(R.id.avatar);
        if (avatarView == null) {
            ViewStub avatarStub = (ViewStub) convertView.findViewById(R.id.vs_avatar);

            int layoutId = AvatarView.getLayoutResource(contact);
            avatarStub.setLayoutResource(layoutId);

            avatarView = (AvatarView) avatarStub.inflate();
            avatarView.setTag(layoutId);
        } else if ((Integer) avatarView.getTag() != AvatarView.getLayoutResource(contact)) {
            ViewGroup viewGroup = (ViewGroup) convertView.findViewById(R.id.avatar_container);
            viewGroup.removeView(avatarView);

            int layoutId = AvatarView.getLayoutResource(contact);
            avatarView = (AvatarView) LayoutInflater.from(convertView.getContext()).inflate(layoutId, viewGroup, false);
            viewGroup.addView(avatarView);

            avatarView.setTag(layoutId);
        }

        avatarView.setContact(contact);
        avatarView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent;
                if (contact.isGroup()) {
                    intent = new Intent(mContext, GroupProfileActivity.class)
                            .setAction(GroupProfileActivity.ACTION_SHOW)
                            .putExtra(GroupProfileActivity.EXTRA_CLIENT_CONTACT_ID, contact.getClientContactId());
                } else {
                    intent = new Intent(mContext, ClientProfileActivity.class)
                            .setAction(ClientProfileActivity.ACTION_SHOW)
                            .putExtra(ClientProfileActivity.EXTRA_CLIENT_CONTACT_ID, contact.getClientContactId());
                }
                mContext.startActivity(intent);
            }
        });
    }

    public void registerListeners() {
        XoApplication.get().getClient().registerContactListener(this);
    }

    public void unregisterListeners() {
        XoApplication.get().getClient().unregisterContactListener(this);
    }

    private List<TalkClientContact> loadContacts() {
        List<TalkClientContact> contacts = new ArrayList<TalkClientContact>();
        try {
            XoClientDatabase database = XoApplication.get().getClient().getDatabase();
            for (TalkClientContact contact : database.findAllContactsExceptSelfOrderedByRecentMessage()) {
                if (mFilter.shouldShow(contact)) {
                    contacts.add(contact);
                }
            }
            for (TalkClientContact selectedContact : mSelectedContacts) {
                if (!contacts.contains(selectedContact)) {
                    contacts.add(selectedContact);
                }
            }
        } catch (SQLException e) {
            LOG.error("Could not load contacts from database", e);
        }

        return contacts;
    }

    private void setContacts(List<TalkClientContact> contacts) {
        mContacts.clear();
        mContacts.addAll(contacts);

        List<TalkClientContact> newSelectedContacts = new ArrayList<TalkClientContact>();
        newSelectedContacts.addAll(CollectionUtils.intersection(mContacts, mSelectedContacts));
        mSelectedContacts.clear();
        mSelectedContacts.addAll(newSelectedContacts);
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
        refreshList();
    }

    public void setSelectedContacts(List<TalkClientContact> selectedContactIds) {
        mSelectedContacts = selectedContactIds;
    }

    public List<TalkClientContact> getSelectedContacts() {
        return mSelectedContacts;
    }

    private void refreshList() {
        final List<TalkClientContact> contacts = loadContacts();
        Handler guiHandler = new Handler(Looper.getMainLooper());
        guiHandler.post(new Runnable() {
            @Override
            public void run() {
                setContacts(contacts);
                for (IContactSelectionListener listener : mContactSelectionListeners) {
                    listener.onContactSelectionChanged(mSelectedContacts.size());
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
        public CheckedTextView checkedNameTextView;
    }

    public interface Filter {
        public boolean shouldShow(TalkClientContact contact);
    }
}
