package com.hoccer.xo.android.adapter;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.base.BaseActivity;
import com.hoccer.xo.android.profile.client.ClientProfileActivity;
import com.hoccer.xo.android.profile.group.GroupProfileActivity;
import com.hoccer.xo.android.view.avatar.AvatarView;
import org.apache.commons.collections4.ListUtils;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for adapters dealing with contacts
 * <p/>
 * This allows base classes to override the UI for various needs.
 * <p/>
 * It also has a filter feature that allows restricting the displayed set of contacts.
 */
public abstract class ContactsAdapter extends BaseAdapter implements IXoContactListener {

    private static final Logger LOG = Logger.getLogger(ContactsAdapter.class);

    protected final static long ITEM_ID_UNKNOWN = -1000;
    protected final static long ITEM_ID_CLIENT_HEADER = -1;
    protected final static long ITEM_ID_GROUP_HEADER = -2;

    protected final static int VIEW_TYPE_SEPARATOR = 0;
    protected final static int VIEW_TYPE_CLIENT = 1;

    protected final static int VIEW_TYPE_COUNT = 2;

    protected final XoClientDatabase mDatabase;
    protected final BaseActivity mActivity;

    protected ContactsAdapter(BaseActivity activity) {
        mActivity = activity;
        mDatabase = XoApplication.get().getClient().getDatabase();
    }

    Filter mFilter;

    List<TalkClientContact> mContacts = new ArrayList<TalkClientContact>();

    public Filter getFilter() {
        return mFilter;
    }

    public void setFilter(Filter filter) {
        this.mFilter = filter;
    }

    public void registerListeners() {
        XoApplication.get().getClient().registerContactListener(this);
    }

    public void unRegisterListeners() {
        XoApplication.get().getClient().unregisterContactListener(this);
    }

    public void loadContacts() {
        List<TalkClientContact> newContacts = new ArrayList<TalkClientContact>();
        synchronized (this) {
            try {
                newContacts = mDatabase.findAllContactsExceptSelfOrderedByRecentMessage();

                if (mFilter != null) {
                    newContacts = filter(newContacts, mFilter);
                }

                for (TalkClientContact contact : newContacts) {
                    TalkClientDownload avatarDownload = contact.getAvatarDownload();
                    if (avatarDownload != null) {
                        mDatabase.refreshClientDownload(avatarDownload);
                    }
                }
            } catch (SQLException e) {
                LOG.error("SQL error", e);
            }
        }

        final List<TalkClientContact> finalNewContacts = newContacts;
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mContacts = finalNewContacts;
                notifyDataSetInvalidated();
            }
        });
    }

    private static List<TalkClientContact> filter(List<TalkClientContact> contacts, Filter filter) {
        ArrayList<TalkClientContact> result = new ArrayList<TalkClientContact>();
        for (TalkClientContact contact : contacts) {
            if (filter.shouldShow(contact)) {
                result.add(contact);
            }
        }
        return result;
    }

    @Override
    public void onClientPresenceChanged(TalkClientContact contact) {
        loadContacts();
    }

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {
        loadContacts();
    }

    @Override
    public void onGroupPresenceChanged(TalkClientContact contact) {
        loadContacts();
    }

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) {
        loadContacts();
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
        return mContacts.size();
    }

    @Override
    public Object getItem(int position) {
        if (position >= 0 && position < mContacts.size()) {
            return mContacts.get(position);
        }
        return null;
    }

    @Override
    public int getItemViewType(int position) {
        if (position >= 0 && position < mContacts.size()) {
            return VIEW_TYPE_CLIENT;
        }
        return VIEW_TYPE_SEPARATOR;
    }

    @Override
    public long getItemId(int position) {
        Object item = getItem(position);
        if (item instanceof TalkClientContact) {
            return ((TalkClientContact) item).getClientContactId();
        }
        return ITEM_ID_UNKNOWN;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int type = getItemViewType(position);

        View view = convertView;
        switch (type) {
            case VIEW_TYPE_CLIENT:
                TalkClientContact contact = (TalkClientContact) getItem(position);

                if (view == null) {
                    view = mActivity.getLayoutInflater().inflate(getClientLayout(), null);
                }
                updateAvatarView(view, contact);
                updateContact(view, contact);
                break;
            case VIEW_TYPE_SEPARATOR:
                if (view == null) {
                    view = mActivity.getLayoutInflater().inflate(getSeparatorLayout(), null);
                }
                updateSeparator(view, position);
                break;
            default:
                view = mActivity.getLayoutInflater().inflate(getSeparatorLayout(), null);
                break;
        }

        return view;
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
                    intent = new Intent(mActivity, GroupProfileActivity.class)
                            .setAction(GroupProfileActivity.ACTION_SHOW)
                            .putExtra(GroupProfileActivity.EXTRA_CLIENT_CONTACT_ID, contact.getClientContactId());
                } else {
                    intent = new Intent(mActivity, ClientProfileActivity.class)
                            .setAction(ClientProfileActivity.ACTION_SHOW)
                            .putExtra(ClientProfileActivity.EXTRA_CLIENT_CONTACT_ID, contact.getClientContactId());
                }
                mActivity.startActivity(intent);
            }
        });
    }

    protected abstract int getClientLayout();

    protected abstract int getSeparatorLayout();

    protected abstract void updateContact(View view, final TalkClientContact contact);

    protected void updateSeparator(View view, int position) {
        TextView separator = (TextView) view;
        separator.setText((String) getItem(position));
    }

    public List<TalkClientContact> getContacts() {
        return ListUtils.unmodifiableList(mContacts);
    }

    public interface Filter {
        public boolean shouldShow(TalkClientContact contact);
    }
}
