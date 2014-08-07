package com.hoccer.xo.android.adapter;

import android.database.DataSetObserver;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.hoccer.talk.client.IXoDownloadListener;
import com.hoccer.talk.client.model.*;
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.view.AudioAttachmentView;
import com.mobeta.android.dslv.DragSortListView;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AttachmentListAdapter extends BaseAdapter implements DragSortListView.DropListener, IXoDownloadListener {

    protected Logger LOG = Logger.getLogger(AttachmentListAdapter.class);

    private List<TalkClientDownload> mItems = new ArrayList<TalkClientDownload>();

    private String mMediaType = null;
    private TalkClientContact mContact = null;

    private List<Integer> mSelectedItemIds = new ArrayList<Integer>();

    private boolean mShowDragHandle = false;

    /*
     * Constructor
     * If contact is null all downloads are considered.
     * If mediaType is null all types of downloads are considered.
     */
    public AttachmentListAdapter(TalkClientContact contact, String mediaType) {
        mMediaType = mediaType;
        mContact = contact;

        updateItems();
        XoApplication.getXoClient().getDatabase().registerDownloadListener(this);
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public TalkClientDownload getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getClientDownloadId();
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
            TalkClientDownload item = mItems.get(from);
            mItems.remove(from);
            mItems.add(to, item);

            refreshView();
        }
    }

    public void setMediaType(String mediaType) {
        // do nothing if already set
        if(areEqual(mMediaType, mediaType)) {
            return;
        }

        mMediaType = mediaType;
        updateItems();
    }

    public String getMediaType() {
        return mMediaType;
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

    public TalkClientDownload[] getItems() {
        return mItems.toArray(new TalkClientDownload[mItems.size()]);
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

    public List<TalkClientDownload> getSelectedItems() {
        List<TalkClientDownload> result = new ArrayList<TalkClientDownload>();
        for(int itemId : mSelectedItemIds) {
            result.add(getItemFromId(itemId));
        }

        return result;
    }

    // Returns the item with the given id or null if not found
    public TalkClientDownload getItemFromId(int itemId) {
        TalkClientDownload result = null;
        for(TalkClientDownload item : mItems) {
            if(item.getClientDownloadId() == itemId) {
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
    public void onDownloadCreated(TalkClientDownload download) {
        if(shouldItemBeAdded(download)) {
            updateItems();
        }
    }

    @Override
    public void onDownloadUpdated(TalkClientDownload download) {
        if(shouldItemBeAdded(download)) {
            updateItems();
        }
    }

    @Override
    public void onDownloadDeleted(TalkClientDownload download) {
        if(mItems.contains(download)) {
            mItems.remove(download);
            deselectItem(download.getClientDownloadId());
            refreshView();
        }
    }

    public void updateItems() {
        try {
            if (mMediaType != null) {
                if (mContact != null) {
                    mItems = XoApplication.getXoClient().getDatabase().findClientDownloadByMediaTypeAndContactId(ContentMediaType.AUDIO, mContact.getClientContactId());
                } else {
                    mItems = XoApplication.getXoClient().getDatabase().findClientDownloadByMediaType(mMediaType);
                }
            } else {
                if (mContact != null) {
                    mItems = XoApplication.getXoClient().getDatabase().findClientDownloadByContactId(mContact.getClientContactId());
                } else {
                    mItems = XoApplication.getXoClient().getDatabase().findAllClientDownloads();
                }
            }
        } catch (SQLException e) {
            LOG.error(e);
        }

        refreshView();
    }

    private boolean shouldItemBeAdded(TalkClientDownload download) {
        // do nothing if the download is incomplete or already contained
        if(download.getState() != TalkClientDownload.State.COMPLETE || mItems.contains(download)) {
            return false;
        }

        // check if mediaType matches
        if(mMediaType != null && !mMediaType.equals(download.getMediaType())) {
            return false;
        }

        // check if contact matches
        if(mContact != null) {
            try {
                TalkClientMessage message = XoApplication.getXoClient().getDatabase().findClientMessageByTalkClientDownloadId(download.getClientDownloadId());
                if(message == null || !mContact.equals(message.getConversationContact())) {
                    return false;
                }
            } catch (SQLException e) {
                LOG.error(e.getMessage(), e);
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
