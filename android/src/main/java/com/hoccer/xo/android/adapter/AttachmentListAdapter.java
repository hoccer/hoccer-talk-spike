package com.hoccer.xo.android.adapter;

import android.database.DataSetObserver;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.hoccer.talk.client.IXoDownloadListener;
import com.hoccer.talk.client.IXoUploadListener;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.XoTransfer;
import com.hoccer.talk.client.model.*;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.view.AudioAttachmentView;
import com.mobeta.android.dslv.DragSortListView;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class AttachmentListAdapter extends BaseAdapter implements DragSortListView.DropListener, IXoUploadListener, IXoDownloadListener {

    protected Logger LOG = Logger.getLogger(AttachmentListAdapter.class);

    private List<XoTransfer> mItems = new ArrayList<XoTransfer>();

    private final String mMediaType;
    private TalkClientContact mContact;

    private final List<Integer> mSelectedItemIds = new ArrayList<Integer>();

    private boolean mShowDragHandle;

    /*
     * Constructor
     * If contact is null all downloads are considered.
     * If mediaType is null all types of downloads are considered.
     */
    public AttachmentListAdapter(TalkClientContact contact, String mediaType) {
        mMediaType = mediaType;
        mContact = contact;
        updateItems();

        XoClientDatabase database = XoApplication.getXoClient().getDatabase();
        database.registerUploadListener(this);
        database.registerDownloadListener(this);
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public XoTransfer getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getTransferId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        AudioAttachmentView audioView = (AudioAttachmentView) convertView;
        if (audioView == null) {
            audioView = new AudioAttachmentView(parent.getContext());
        }

        audioView.setMediaItem(mItems.get(position));
        audioView.updatePlayPauseView();
        Integer itemId = (int)getItemId(position);
        audioView.getChildAt(0).setSelected(mSelectedItemIds.contains(itemId));
        audioView.showDragHandle(mShowDragHandle);

        return audioView;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    /*
    This needed to be overridden because 3rd party ListViews
    may override their setAdapter(); method and do not handle
    the IllegalStateException
     */
    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        try {
            super.registerDataSetObserver(observer);
        } catch (IllegalStateException e) {

        }
    }

    @Override
    public void drop(int from, int to) {
        if (from != to) {
            XoTransfer item = mItems.get(from);
            mItems.remove(from);
            mItems.add(to, item);

            refreshView();
        }
    }

    public void setContact(TalkClientContact contact) {
        // do nothing if already set
        if(areEqual(mContact, contact)) {
            return;
        }

        mContact = contact;
        updateItems();
    }

    public TalkClientContact getContact() {
        return mContact;
    }

    public List<XoTransfer> getItems() {
        return new ArrayList<XoTransfer>(mItems);
    }

    public void selectItem(int itemId) {
        if(!mSelectedItemIds.contains(itemId)) {
            mSelectedItemIds.add(itemId);
            refreshView();
        }
    }

    public void deselectItem(int itemId) {
        if(mSelectedItemIds.contains(itemId)) {
            mSelectedItemIds.remove((Integer)itemId);
            refreshView();
        }
    }

    public void selectAllItems() {
        deselectAllItems();
        for(int i = 0; i < getCount(); i++) {
            selectItem((int) getItemId(i));
        }
    }

    public void deselectAllItems() {
        mSelectedItemIds.clear();
    }

    public List<XoTransfer> getSelectedItems() {
        List<XoTransfer> result = new ArrayList<XoTransfer>();
        for(int itemId : mSelectedItemIds) {
            result.add(getItemFromId(itemId));
        }

        return result;
    }

    // Returns the item with the given id or null if not found
    public XoTransfer getItemFromId(int itemId) {
        XoTransfer result = null;
        for(XoTransfer item : mItems) {
            if(item.getTransferId() == itemId) {
                result = item;
                break;
            }
        }
        return result;
    }

    public void showDragHandle(boolean show) {
        mShowDragHandle = show;
    }

    @Override
    public void onUploadCreated(TalkClientUpload upload) {
        addItem(upload);
    }

    @Override
    public void onUploadUpdated(TalkClientUpload upload) {
        addItem(upload);
    }

    @Override
    public void onUploadDeleted(TalkClientUpload upload) {
        removeItem(upload);
    }

    @Override
    public void onDownloadCreated(TalkClientDownload download) {
        // do nothing if the download is incomplete
        if(download.getState() == TalkClientDownload.State.COMPLETE) {
            addItem(download);
        }
    }

    @Override
    public void onDownloadUpdated(TalkClientDownload download) {
        // do nothing if the download is incomplete
        if(download.getState() == TalkClientDownload.State.COMPLETE) {
            addItem(download);
        }
    }

    @Override
    public void onDownloadDeleted(TalkClientDownload download) {
        removeItem(download);
    }

    private void addItem(XoTransfer item) {
        if(shouldItemBeAdded(item)) {
            updateItems();
        }
    }

    private void removeItem(XoTransfer item) {
        if(mItems.contains(item)) {
            mItems.remove(item);
            deselectItem(item.getTransferId());
            refreshView();
        }
    }

    public void updateItems() {
        try {
            if (mContact != null) {
                mItems = new ArrayList<XoTransfer>(XoApplication.getXoClient().getDatabase().findClientDownloadsByMediaTypeAndContactId(mMediaType, mContact.getClientContactId()));
            } else {
                List<XoTransfer> items = XoApplication.getXoClient().getDatabase().findTransfersByMediaType(mMediaType);
                mItems = filterDuplicateFiles(items);
            }
        } catch (SQLException e) {
            LOG.error(e);
        }

        refreshView();
    }

    private static List<XoTransfer> filterDuplicateFiles(List<XoTransfer> transfers) {
        List<XoTransfer> filteredTransfers = new ArrayList<XoTransfer>();
        HashSet<String> filePathes = new HashSet<String>();

        for(int i = transfers.size()-1; i >= 0; i--) {
            XoTransfer transfer = transfers.get(i);
            if(!filePathes.contains(transfer.getFilePath())) {
                filteredTransfers.add(0, transfer);
                filePathes.add(transfer.getFilePath());
            }
        }

        return filteredTransfers;
    }

    private boolean shouldItemBeAdded(XoTransfer transfer) {
        // check if mediaType matches
        if(mMediaType != null && !mMediaType.equals(transfer.getContentMediaType())) {
            return false;
        }

        // check if item is already in the list
        if (mItems.contains(transfer)) {
            return false;
        }

        // check if contact matches
        if(mContact != null) {
            try {
                XoClientDatabase database = XoApplication.getXoClient().getDatabase();
                TalkClientMessage message = transfer.isUpload() ?
                        database.findClientMessageByTalkClientUploadId(transfer.getUploadOrDownloadId()) :
                        database.findClientMessageByTalkClientDownloadId(transfer.getUploadOrDownloadId());

                if(message == null || !mContact.equals(message.getSenderContact()) || !mContact.equals(message.getConversationContact())) {
                    return false;
                }
            } catch (SQLException e) {
                LOG.error(e.getMessage(), e);
            }
        }

        // check if it is a duplicate file
        for(XoTransfer item : mItems) {
            if(item.getFilePath().equals(transfer.getFilePath())) {
                return false;
            }
        }

        return true;
    }

    private boolean areEqual(Object obj1, Object obj2) {
        if(obj1 != null) {
            if (obj1.equals(obj2)) {
                return true;
            }
        } else {
            if(obj2 == null) {
                return true;
            }
        }

        return false;
    }

    private void refreshView() {
        Handler guiHandler = new Handler(Looper.getMainLooper());
        guiHandler.post(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }
}
