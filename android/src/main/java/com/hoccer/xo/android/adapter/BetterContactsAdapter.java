package com.hoccer.xo.android.adapter;

import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.IXoMessageListener;
import com.hoccer.talk.client.IXoTokenListener;
import com.hoccer.talk.client.IXoTransferListenerOld;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.client.model.TalkClientSmsToken;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.model.TalkDelivery;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.base.XoAdapter;
import com.hoccer.xo.android.fragment.ContactsFragment;
import com.hoccer.xo.android.util.SortedList;
import com.hoccer.xo.android.view.model.BaseContactItem;
import com.hoccer.xo.android.view.model.SmsContactItem;
import com.hoccer.xo.android.view.model.TalkClientContactItem;

import org.jetbrains.annotations.Nullable;

import android.view.View;
import android.view.ViewGroup;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class BetterContactsAdapter extends XoAdapter implements IXoContactListener, IXoMessageListener, IXoTokenListener, IXoTransferListenerOld {


    private static final Comparator<BaseContactItem> LATEST_MESSAGE_COMPARATOR = new Comparator<BaseContactItem>() {
        @Override
        public int compare(BaseContactItem o1, BaseContactItem o2) {
            if(o1.getTimeStamp() == o2.getTimeStamp()) {
                return 0;
            } else if(o1.getTimeStamp() > o2.getTimeStamp()) {
                return -1;
            }
            return 1;
        }
    };

    private List<BaseContactItem> mContactItems;
    private Filter mFilter = null;

    @Nullable
    private OnItemCountChangedListener mOnItemCountChangedListener;

    public BetterContactsAdapter(XoActivity activity) {
        super(activity);
        initialize();
    }

    public BetterContactsAdapter(XoActivity activity, Filter filter) {
        super(activity);
        mFilter = filter;
        initialize();
    }

    private void initialize() {
        LOG.debug("initialize()");
        int oldItemCount = 0;
        if(mContactItems != null) {
            oldItemCount = mContactItems.size();
        }
        mContactItems = new ArrayList<BaseContactItem>();
        try {
            List<TalkClientContact> allClientContacts = mActivity.getXoDatabase().findAllClientContacts();
            List<TalkClientContact> filteredContacts = filter(allClientContacts);
            for (int i = 0; i < filteredContacts.size(); i++) {
                TalkClientContact contact = filteredContacts.get(i);
                mContactItems.add(new TalkClientContactItem(mActivity, contact));
            }

            List<TalkClientSmsToken> allSmsTokens = mActivity.getXoDatabase().findAllSmsTokens();
            for (int i = 0; i < allSmsTokens.size(); i++) {
                TalkClientSmsToken smsToken = allSmsTokens.get(i);
                mContactItems.add(new SmsContactItem(mActivity, smsToken));
            }
            if(mOnItemCountChangedListener != null && oldItemCount != getCount()) {
                mOnItemCountChangedListener.onItemCountChanged(getCount());
            }
            notifyDataSetChanged();
            reloadFinished();
        } catch (SQLException e) {
            LOG.error("sql error", e);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        getXoClient().registerContactListener(this);
        getXoClient().registerTokenListener(this);
        getXoClient().registerTransferListener(this);
        getXoClient().registerMessageListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getXoClient().unregisterContactListener(this);
        getXoClient().unregisterTokenListener(this);
        getXoClient().unregisterTransferListener(this);
        getXoClient().unregisterMessageListener(this);
    }

    @Override
    public void onReloadRequest() {
        initialize();
        super.onReloadRequest();
    }

    public Filter getFilter() {
        return mFilter;
    }

    public void setFilter(Filter filter) {
        this.mFilter = filter;
        initialize();
    }

    @Override
    public void notifyDataSetChanged() {
        LOG.debug("notifyDataSetChanged()");
        Collections.sort(mContactItems, LATEST_MESSAGE_COMPARATOR);
        super.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mContactItems.size();
    }

    @Override
    public Object getItem(int i) {
        return mContactItems.get(i).getContent();
    }

    @Override
    public long getItemId(int i) {
        return -1;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        View view = mContactItems.get(position).getView(convertView);
        return view;
    }


    @Override
    public void onContactAdded(TalkClientContact contact) {
        TalkClientContactItem item = new TalkClientContactItem(mActivity, contact);
        mContactItems.add(item);
        notifyDataSetChanged();
    }

    @Override
    public void onContactRemoved(TalkClientContact contact) {
        BaseContactItem item = findContactItemForContent(contact);
        mContactItems.remove(item);
        notifyDataSetChanged();
    }

    @Override
    public void onClientPresenceChanged(TalkClientContact contact) {
        LOG.debug("onClientPresenceChanged()");
        BaseContactItem item = findContactItemForContent(contact);
        if(item == null) {
            return;
        }
        item.update();
        notifyDataSetChanged();
    }

    private void updateAll() {
        for (int i = 0; i < mContactItems.size(); i++) {
            BaseContactItem item = mContactItems.get(i);
            item.update();
        }
        notifyDataSetChanged();
    }

    private BaseContactItem findContactItemForContent(Object content) {
        for (int i = 0; i < mContactItems.size(); i++) {
            BaseContactItem item = mContactItems.get(i);
            if(content.equals(item.getContent())) {
                return item;
            }
        }
        return null;
    }

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {
        updateAll();
    }

    @Override
    public void onGroupPresenceChanged(TalkClientContact contact) {
        updateAll();
    }

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) {
        updateAll();
    }

    @Override
    public void onMessageAdded(TalkClientMessage message) {
        LOG.debug("onMessageAdded()");
        try {
            if(message.isIncoming()) {
                TalkDelivery incomingDelivery = message.getIncomingDelivery();
                TalkClientContact contact = mDatabase.findContactByClientId(incomingDelivery.getSenderId(), false);
                if(contact == null) {
                    return;
                }
                TalkClientContactItem item = (TalkClientContactItem) findContactItemForContent(contact);
                if(item == null) { // the contact is not in our list so we won't update anything
                    return;
                }
                item.update();
                notifyDataSetChanged();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessageRemoved(TalkClientMessage message) {
        updateAll();
    }

    @Override
    public void onMessageStateChanged(TalkClientMessage message) {
        updateAll();
    }

    @Override
    public void onTokensChanged(List<TalkClientSmsToken> tokens, boolean newTokens) {
        for (int i = 0; i < tokens.size(); i++) {
            TalkClientSmsToken token = tokens.get(i);
            SmsContactItem item = (SmsContactItem) findContactItemForContent(token);
            int index = mContactItems.indexOf(item);
            item = null;
                item = new SmsContactItem(mActivity, token);
            if(index > -1) {
                mContactItems.set(index, item);
            } else {
                mContactItems.add(item);
            }

        }

        updateAll();
    }

    @Override
    public void onDownloadRegistered(TalkClientDownload download) {
        updateAll();
    }

    @Override
    public void onDownloadStarted(TalkClientDownload download) {
        updateAll();
    }

    @Override
    public void onDownloadProgress(TalkClientDownload download) {
    }

    @Override
    public void onDownloadFinished(TalkClientDownload download) {
        updateAll();
    }

    @Override
    public void onDownloadFailed(TalkClientDownload download) {
        updateAll();
    }

    @Override
    public void onDownloadStateChanged(TalkClientDownload download) {
        if(download.isAvatar() && download.getState() == TalkClientDownload.State.COMPLETE) {
            updateAll();
        }
    }

    @Override
    public void onUploadStarted(TalkClientUpload upload) {
        updateAll();
    }

    @Override
    public void onUploadProgress(TalkClientUpload upload) {
    }

    @Override
    public void onUploadFinished(TalkClientUpload upload) {
        updateAll();
    }

    @Override
    public void onUploadFailed(TalkClientUpload upload) {
        updateAll();
    }

    @Override
    public void onUploadStateChanged(TalkClientUpload upload) {
        updateAll();
    }

    private List<TalkClientContact> filter(List<TalkClientContact> in) {
        if(mFilter == null) {
            return in;
        }
        ArrayList<TalkClientContact> res = new ArrayList<TalkClientContact>();
        for(TalkClientContact contact: in) {
            if(mFilter.shouldShow(contact)) {
                res.add(contact);
            }
        }
        return res;
    }

    public void setOnItemCountChangedListener(OnItemCountChangedListener onItemCountChangedListener) {
        mOnItemCountChangedListener = onItemCountChangedListener;
    }

    public interface Filter {
        public boolean shouldShow(TalkClientContact contact);
    }
}