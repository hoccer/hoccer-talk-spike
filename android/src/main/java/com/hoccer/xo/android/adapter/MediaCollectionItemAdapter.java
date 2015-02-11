package com.hoccer.xo.android.adapter;

import android.database.DataSetObserver;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.hoccer.talk.client.XoTransfer;
import com.hoccer.talk.client.model.TalkClientMediaCollection;
import com.hoccer.xo.android.view.AudioAttachmentView;
import com.mobeta.android.dslv.DragSortListView;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class MediaCollectionItemAdapter extends BaseAdapter implements DragSortListView.DropListener, TalkClientMediaCollection.Listener {

    private static final Logger LOG = Logger.getLogger(MediaCollectionItemAdapter.class);

    private final List<Integer> mSelectedItemIds = new ArrayList<Integer>();

    private final TalkClientMediaCollection mCollection;
    private boolean mShowDragHandle;

    public MediaCollectionItemAdapter(TalkClientMediaCollection collection) {
        mCollection = collection;
        mCollection.registerListener(this);
    }

    @Override
    public int getCount() {
        return mCollection.size();
    }

    @Override
    public XoTransfer getItem(int position) {
        return mCollection.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        return mCollection.getItem(position).getTransferId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        AudioAttachmentView audioView = (AudioAttachmentView) convertView;
        if (audioView == null) {
            audioView = new AudioAttachmentView(parent.getContext());
        }

        XoTransfer item = mCollection.getItem(position);
        audioView.setMediaItem(item);
        audioView.updatePlayPauseView();
        Integer itemId = (int) getItemId(position);
        audioView.getChildAt(0).setSelected(mSelectedItemIds.contains(itemId));
        audioView.showDragHandle(mShowDragHandle);

        return audioView;
    }

    public void showDragHandle(boolean show) {
        mShowDragHandle = show;
    }

    public void selectItem(int itemId) {
        if (!mSelectedItemIds.contains(itemId)) {
            mSelectedItemIds.add(itemId);
            refreshView();
        }
    }

    public void deselectItem(int itemId) {
        if (mSelectedItemIds.contains(itemId)) {
            mSelectedItemIds.remove((Integer) itemId);
            refreshView();
        }
    }

    public void selectAllItems() {
        deselectAllItems();
        for (int i = 0; i < mCollection.size(); i++) {
            mSelectedItemIds.add((int) getItemId(i));
        }
    }

    public void deselectAllItems() {
        mSelectedItemIds.clear();
    }

    public List<XoTransfer> getSelectedItems() {
        List<XoTransfer> result = new ArrayList<XoTransfer>();

        for (int itemId : mSelectedItemIds) {
            result.add(mCollection.getItemFromId(itemId));
        }

        return result;
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
            LOG.error("Illegal state when registering DataSetObserver", e);
        }
    }

    @Override
    public void drop(int from, int to) {
        mCollection.reorderItemIndex(from, to);
        refreshView();
    }

    @Override
    public void onCollectionNameChanged(TalkClientMediaCollection collection) {
        // do nothing
    }

    @Override
    public void onItemOrderChanged(TalkClientMediaCollection collection, int fromIndex, int toIndex) {
        refreshView();
    }

    @Override
    public void onItemRemoved(TalkClientMediaCollection collection, int indexRemoved, XoTransfer itemRemoved) {
        deselectItem(itemRemoved.getTransferId());
        refreshView();
    }

    @Override
    public void onItemAdded(TalkClientMediaCollection collection, int indexAdded, XoTransfer itemAdded) {
        refreshView();
    }

    @Override
    public void onCollectionCleared(TalkClientMediaCollection collection) {
        refreshView();
    }
}
