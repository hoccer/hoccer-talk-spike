package com.hoccer.xo.android.adapter;

import android.database.DataSetObserver;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientMediaCollection;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.service.MediaPlayerService;
import com.hoccer.xo.android.view.AudioAttachmentView;
import com.mobeta.android.dslv.DragSortListView;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AttachmentListAdapter extends BaseAdapter implements DragSortListView.DropListener {

    private static final long INVALID_ID = -1;
    protected Logger LOG = Logger.getLogger(AttachmentListAdapter.class);

    private List<IContentObject> mAttachmentItems = new ArrayList<IContentObject>();

    private String mContentMediaType;
    private int mConversationContactId = MediaPlayerService.UNDEFINED_CONTACT_ID;

    private SparseBooleanArray mCheckedItemPositions = new SparseBooleanArray();

    private TalkClientMediaCollection mCollection = null;
    private boolean mShowDragHandle = false;

    public AttachmentListAdapter() {
        this(null, MediaPlayerService.UNDEFINED_CONTACT_ID);
    }

    public AttachmentListAdapter(String pContentMediaType) {
        this(pContentMediaType, MediaPlayerService.UNDEFINED_CONTACT_ID);
    }

    public AttachmentListAdapter(int pConversationContactId) {
        this(null, pConversationContactId);
    }

    public AttachmentListAdapter(String pContentMediaType, int pConversationContactId) {
        setContentMediaTypeFilter(pContentMediaType);
        setContactIdFilter(pConversationContactId);
    }

    public List<IContentObject> getAttachmentItems() {
        return mAttachmentItems;
    }

    public void setItems(IContentObject[] items) {
        mAttachmentItems.clear();
        for(int i = 0; i < items.length; i++) {
            mAttachmentItems.add(items[i]);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mAttachmentItems.size();
    }

    @Override
    public IContentObject getItem(int position) {
        return mAttachmentItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        long itemId = INVALID_ID;

        if (position >= 0 || position < mAttachmentItems.size()) {
            IContentObject contentObject = getItem(position);

            if (contentObject instanceof TalkClientDownload) {
                itemId = ((TalkClientDownload) contentObject).getClientDownloadId();
            } else if (contentObject instanceof TalkClientUpload) {
                itemId = ((TalkClientUpload) contentObject).getClientUploadId();
            }
        }

        return itemId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (mAttachmentItems.get(position) == null) {
            mAttachmentItems.remove(position);
            if (mAttachmentItems.size() <= position) {
                return null;
            }
            return getView(position, convertView, parent);
        }

        AudioAttachmentView audioRowView = (AudioAttachmentView) convertView;
        if (audioRowView == null) {
            audioRowView = new AudioAttachmentView(parent.getContext());
        }

        audioRowView.setMediaItem(mAttachmentItems.get(position));
        audioRowView.updatePlayPauseView();
        audioRowView.getChildAt(0).setSelected(mCheckedItemPositions.get(position));
        audioRowView.showDragHandle(mShowDragHandle);

        return audioRowView;
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

    public void setContentMediaTypeFilter(String pContentMediaType) {
        mContentMediaType = pContentMediaType;
    }

    public String getContentMediaType() {
        return mContentMediaType;
    }

    public void setContactIdFilter(int pConversationContactId) {
        mConversationContactId = pConversationContactId;
    }

    public int getConversationContactId() {
        return mConversationContactId;
    }

    @Override
    public void drop(int from, int to) {
        if (from != to) {
            IContentObject item = mAttachmentItems.get(from);
            mAttachmentItems.remove(from);
            mAttachmentItems.add(to, item);

            if (mCollection != null) {
                mCollection.reorderItemIndex(from, to);
            }

            notifyDataSetChanged();
        }
        // TODO: update mCheckedItemPositions
    }

    public boolean removeItem(IContentObject item) {
        return removeItem(item, false);
    }

    public boolean removeItem(IContentObject item, boolean removeFromCollection) {
        if (removeFromCollection) {
            removeItemFromCollection(item);
        }

        boolean isRemovable = mAttachmentItems.contains(item);
        if (isRemovable) {
            mAttachmentItems.remove(mAttachmentItems.indexOf(item));
            notifyDataSetChanged();
        }

        return isRemovable;
    }

    public void removeItemAt(int which) throws IndexOutOfBoundsException {
        removeItemAt(which, false);
    }

    public void removeItemAt(int which, boolean removeFromCollection) throws IndexOutOfBoundsException{
        if (removeFromCollection) {
            removeItemFromCollection(getItem(which));
        }

        mAttachmentItems.remove(which);
        updateCheckedItems();
    }

    public void addItemAt(IContentObject item, int pos) throws IndexOutOfBoundsException{
            mAttachmentItems.add(pos, item);
            updateCheckedItems();
            notifyDataSetChanged();
    }

    public void clear() {
        mAttachmentItems.clear();
        notifyDataSetChanged();
    }

    public void setCheckedItemsPositions(SparseBooleanArray itemPositions) {
        mCheckedItemPositions = itemPositions;
        notifyDataSetChanged();
    }

    public SparseBooleanArray getCheckedItemPositions() {
        return mCheckedItemPositions;
    }

    public void loadAttachments() {
        mCollection = null;
        try {
            List<TalkClientDownload> downloads;
            if (mContentMediaType != null) {
                if (mConversationContactId != MediaPlayerService.UNDEFINED_CONTACT_ID) {
                    downloads = XoApplication.getXoClient().getDatabase().findClientDownloadByMediaTypeAndConversationContactId(ContentMediaType.AUDIO, mConversationContactId);
                } else {
                    downloads = XoApplication.getXoClient().getDatabase().findClientDownloadByMediaType(mContentMediaType);
                }
            } else {
                downloads = XoApplication.getXoClient().getDatabase().findAllClientDownloads();
            }

            createAttachmentsFromTalkClientDownloads(downloads);
        } catch (SQLException e) {
            LOG.error(e);
        }
    }

    public void loadAttachmentsFromCollection(TalkClientMediaCollection collection) {
        mCollection = collection;
        List<TalkClientDownload> downloads = new ArrayList<TalkClientDownload>();
        for (int i = 0; i < collection.size(); ++i) {
            downloads.add(collection.getItem(i));
        }
        createAttachmentsFromTalkClientDownloads(downloads);
    }

    public void setSortEnabled(boolean shallShow) {
        mShowDragHandle = shallShow;
    }

    private void removeItemFromCollection(IContentObject item) {
        if (isItemPartOfCollection(item)) {
            mCollection.removeItem((TalkClientDownload) item);
        }
    }

    private void createAttachmentsFromTalkClientDownloads(Iterable<TalkClientDownload> downloads) {
        if (downloads != null) {
            for (TalkClientDownload download : downloads) {
                if (!isRecordedAudio(download.getFileName())) {
                    mAttachmentItems.add(download);
                }
            }
        }
    }

    private void updateCheckedItems() {
        if (mCheckedItemPositions.size() > 0) {
            SparseBooleanArray updatedSelection = new SparseBooleanArray(mCheckedItemPositions.size());

            for (int i = 0; i < mCheckedItemPositions.size(); ++i) {
                boolean b = mCheckedItemPositions.valueAt(i);
                int k = mCheckedItemPositions.keyAt(i);
                updatedSelection.put(k + 1, b);
            }

            mCheckedItemPositions.clear();

            //@Info Setting mSelection to updatedSelection won't work since the reference changes, which is not allowed
            for (int i = 0; i < updatedSelection.size(); ++i) {
                mCheckedItemPositions.append(updatedSelection.keyAt(i), updatedSelection.valueAt(i));
            }
        }
    }

    private boolean isRecordedAudio(String fileName) {
        if (fileName.startsWith("recording")) {
            return true;
        }
        return false;
    }

    private boolean isItemPartOfCollection(IContentObject item) {
        boolean isItemPartOfCollection = false;

        try {
            TalkClientDownload contentObject = (TalkClientDownload) item;
            if (mCollection != null && contentObject != null && mCollection.hasItem(contentObject)) {
                isItemPartOfCollection = true;
            }

        } catch (ClassCastException e) {

        }

        return isItemPartOfCollection;
    }
}
