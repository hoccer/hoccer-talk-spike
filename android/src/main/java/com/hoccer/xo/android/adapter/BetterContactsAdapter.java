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
import com.hoccer.xo.android.view.model.ContactItem;

import android.view.View;
import android.view.ViewGroup;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


/**
 * TODO:
 * - add SMS Token Support
 * - add filtering
 * - add sort by unseen messages
 */
public class BetterContactsAdapter extends XoAdapter implements IXoContactListener, IXoMessageListener, IXoTokenListener, IXoTransferListenerOld {

    private List<ContactItem> mContactItems;


    public BetterContactsAdapter(XoActivity activity) {
        super(activity);
        initialize();
    }

    private void initialize() {
        mContactItems = new ArrayList<ContactItem>();
    }

    @Override
    public int getCount() {
        return mContactItems.size();
    }

    @Override
    public Object getItem(int i) {
        return mContactItems.get(i).getTalkClientContact();
    }

    @Override
    public long getItemId(int i) {
        return mContactItems.get(i).getTalkClientContact().getClientContactId();
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        View view = mContactItems.get(i).getView(convertView);
        return view;
    }


    @Override
    public void onContactAdded(TalkClientContact contact) {
        ContactItem item = new ContactItem(this, contact);
        mContactItems.add(item);
        notifyDataSetChanged();
    }

    @Override
    public void onContactRemoved(TalkClientContact contact) {
        ContactItem item = findContactItemForContact(contact);
        mContactItems.remove(item);
        notifyDataSetChanged();
    }

    @Override
    public void onClientPresenceChanged(TalkClientContact contact) {
        ContactItem contactItem = findContactItemForContact(contact);
        if(contactItem == null) {
            return;
        }

        contactItem.update();
        notifyDataSetChanged();
    }

    private void updateAll() {
        for (int i = 0; i < mContactItems.size(); i++) {
            ContactItem contactItem = mContactItems.get(i);
            contactItem.update();
        }

        notifyDataSetChanged();
    }

    private ContactItem findContactItemForContact(TalkClientContact contact) {
        for (int i = 0; i < mContactItems.size(); i++) {
            ContactItem contactItem = mContactItems.get(i);
            if(contact.equals(contactItem.getTalkClientContact())) {
                return contactItem;
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
        try {
            if(message.isIncoming()) {
                TalkDelivery incomingDelivery = message.getIncomingDelivery();
                TalkClientContact contact = mDatabase.findContactByClientId(incomingDelivery.getReceiverId(), false);
                if(contact == null) {
                    return;
                }
                ContactItem item = findContactItemForContact(contact);
                item.update();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        updateAll();
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

}
