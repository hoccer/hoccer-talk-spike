package com.hoccer.xo.android.adapter;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientMediaCollection;
import com.hoccer.xo.android.view.AudioAttachmentView;
import com.mobeta.android.dslv.DragSortListView;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class MediaCollectionItemAdapter extends BaseAdapter implements DragSortListView.DropListener, TalkClientMediaCollection.Listener {

    protected Logger LOG = Logger.getLogger(AttachmentListAdapter.class);

    private List<Integer> mSelectedItemIds = new ArrayList<Integer>();

    private TalkClientMediaCollection mCollection = null;
    private boolean mShowDragHandle = false;

    public MediaCollectionItemAdapter(TalkClientMediaCollection collection) {
        mCollection = collection;
        mCollection.registerListener(this);
    }

    @Override
    public int getCount() {
        return mCollection.size();
    }

    @Override
    public TalkClientDownload getItem(int position) {
        return mCollection.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        return mCollection.getItem(position).getClientDownloadId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        AudioAttachmentView audioRowView = (AudioAttachmentView) convertView;
        if (audioRowView == null) {
            audioRowView = new AudioAttachmentView(parent.getContext());
        }

        TalkClientDownload item = mCollection.getItem(position);
        audioRowView.setMediaItem(item);
        audioRowView.updatePlayPauseView();
        Integer itemId = (int)getItemId(position);
        audioRowView.getChildAt(0).setSelected(mSelectedItemIds.contains(itemId));
        audioRowView.showDragHandle(mShowDragHandle);

        return audioRowView;
    }

    public void showDragHandle(boolean show) {
        mShowDragHandle = show;
    }

    public TalkClientDownload[] getItems() {
        return mCollection.toArray();
    }

    public void selectItem(int itemId) {
        if(!mSelectedItemIds.contains(itemId)) {
            mSelectedItemIds.add(itemId);
            notifyDataSetChanged();
        }
    }

    public void deselectItem(int itemId) {
        if(mSelectedItemIds.contains(itemId)) {
            mSelectedItemIds.remove((Integer)itemId);
            notifyDataSetChanged();
        }
    }

    public void selectAllItems() {
        for(int i = 0; i < mCollection.size(); i++) {
            mSelectedItemIds.add((int)getItemId(i));
        }
    }

    public void deselectAllItems() {
        mSelectedItemIds.clear();
    }

    public List<TalkClientDownload> getAllSelectedItems() {
        List<TalkClientDownload> result = new ArrayList<TalkClientDownload>();
        for(int itemId : mSelectedItemIds) {
            result.add(mCollection.getItemFromId(itemId));
        }

        return result;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    /*
     * This needed to be overridden because 3rd party ListViews may override their setAdapter(); method and do not handle
     * the IllegalStateException.
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
        mCollection.reorderItemIndex(from, to);
        notifyDataSetChanged();
    }

    @Override
    public void onCollectionNameChanged(TalkClientMediaCollection collection) {
        // do nothing
    }

    @Override
    public void onItemOrderChanged(TalkClientMediaCollection collection, int fromIndex, int toIndex) {
        notifyDataSetChanged();

    }

    @Override
    public void onItemRemoved(TalkClientMediaCollection collection, int indexRemoved, TalkClientDownload itemRemoved) {
        notifyDataSetChanged();

    }

    @Override
    public void onItemAdded(TalkClientMediaCollection collection, int indexAdded, TalkClientDownload itemAdded) {
        notifyDataSetChanged();

    }

    @Override
    public void onCollectionCleared(TalkClientMediaCollection collection) {
        notifyDataSetChanged();
    }
}
