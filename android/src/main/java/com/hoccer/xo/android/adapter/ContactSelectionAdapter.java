package com.hoccer.xo.android.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.view.AvatarView;
import com.artcom.hoccer.R;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ContactSelectionAdapter extends BaseAdapter {

    static final Logger LOG = Logger.getLogger(ContactSelectionAdapter.class);

    private List<TalkClientContact> mContacts;

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

        TalkClientContact contact = mContacts.get(position);
        viewHolder.contactAvatarView.setContact(contact);

        if (contact.isGroup() && contact.getGroupPresence() != null && contact.getGroupPresence().isTypeNearby()) {
            viewHolder.checkedNameTextView.setText(R.string.nearby_text);
        } else {
            viewHolder.checkedNameTextView.setText(contact.getNickname());
        }

        return convertView;
    }

    private void loadContacts() {
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
    }

    private boolean shouldShow(TalkClientContact contact) {
        boolean shouldShow = false;
        if (contact.isGroup()) {
            if (contact.isGroupInvolved() && contact.isGroupExisting() && !(contact.getGroupPresence() != null && contact.getGroupPresence().isKept())) {
                shouldShow = true;
            }
        } else if (contact.isClient()) {
            if (contact.isClientRelated() && (contact.getClientRelationship().isFriend() || contact.getClientRelationship()
                    .isBlocked())) {
                shouldShow = true;
            }
        }

        return shouldShow;
    }

    private class ViewHolder {
        public AvatarView contactAvatarView;
        public CheckedTextView checkedNameTextView;
    }
}
