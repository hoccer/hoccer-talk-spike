package com.hoccer.xo.android.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.view.AvatarView;
import com.hoccer.xo.release.R;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ContactSelectionAdapter extends BaseAdapter {

    private Context mContext;

    private List<TalkClientContact> mContacts = new ArrayList<TalkClientContact>();

    public static final int CLIENT_CONTACT_MODE = 0;
    public static final int GROUP_CONTACT_MODE = 1;

    private ContactSelectionAdapter(List<TalkClientContact> contacts, Context context) {
        mContacts = contacts;
        mContext = context;
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
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_contact_checked, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.contactAvatarView = (AvatarView) (convertView.findViewById(R.id.contact_icon));
            viewHolder.checkedtNameTextView = (CheckedTextView) (convertView.findViewById(R.id.contact_name_checked));
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        TalkClientContact contact = mContacts.get(position);

        viewHolder.contactAvatarView.setContact(contact);
        viewHolder.checkedtNameTextView.setText(contact.getName());

        return convertView;
    }

    private class ViewHolder {
        public AvatarView contactAvatarView;
        public CheckedTextView checkedtNameTextView;
    }

    public static ContactSelectionAdapter create(Context context, int mode) throws SQLException, InvalidContactModeException {
        List<TalkClientContact> contacts;
        switch (mode) {
            case CLIENT_CONTACT_MODE:
                contacts = XoApplication.getXoClient().getDatabase().findAllClientContacts();
                break;
            case GROUP_CONTACT_MODE:
                contacts = XoApplication.getXoClient().getDatabase().findAllGroupContacts();
                break;
            default:
                throw new InvalidContactModeException(mode);
        }
        return new ContactSelectionAdapter(contacts, context);
    }

    public static class InvalidContactModeException extends Throwable {
        public InvalidContactModeException(int mode) {
            super(String.format("%s is not supported", mode));
        }
    }
}
