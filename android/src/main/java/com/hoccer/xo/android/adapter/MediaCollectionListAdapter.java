package com.hoccer.xo.android.adapter;

import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.hoccer.talk.client.IXoMediaCollectionListener;
import com.hoccer.talk.client.model.TalkClientMediaCollection;
import com.hoccer.xo.android.XoApplication;
import com.artcom.hoccer.R;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MediaCollectionListAdapter extends BaseAdapter implements IXoMediaCollectionListener {

    private final static Logger LOG = Logger.getLogger(MediaCollectionListAdapter.class);

    private List<TalkClientMediaCollection> mMediaCollections = new ArrayList<TalkClientMediaCollection>();
    private final SparseBooleanArray mSelectedItems = new SparseBooleanArray();

    public MediaCollectionListAdapter() {
        try {
            XoApplication.get().getXoClient().getDatabase().registerMediaCollectionListener(this);
            loadMediaCollections();
        } catch (SQLException e) {
            LOG.error("Loading media collections failed.", e);
        }
    }

    @Override
    public int getCount() {
        return mMediaCollections.size();
    }

    @Override
    public Object getItem(int position) {
        return mMediaCollections.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mMediaCollections.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_media_collection, null);

            viewHolder = new ViewHolder();
            viewHolder.titleName = (TextView) (convertView.findViewById(R.id.tv_title_name));
            viewHolder.goToImageView = (ImageView) (convertView.findViewById(R.id.iv_go_to));
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        TalkClientMediaCollection mediaCollection = mMediaCollections.get(position);
        viewHolder.titleName.setText(mediaCollection.getName());

        // TODO find out why the f**k this doesn't work but setting the BG colour manually does
//        convertView.setSelected(mSelectedItems.get(position));

        if (mSelectedItems.get(position)) {
            convertView.setBackgroundColor(parent.getResources().getColor(R.color.background_selected));
        } else {
            convertView.setBackgroundColor(parent.getResources().getColor(R.color.background_default));
        }

        if (mSelectedItems.size() > 0) {
            // if mSelectedItems.size() > 0 we can assume the contextual action mode is active
            viewHolder.goToImageView.setVisibility(View.INVISIBLE);
        } else {
            viewHolder.goToImageView.setVisibility(View.VISIBLE);
        }

        return convertView;
    }

    public void selectItem(int position, boolean selected) {
        if(selected) {
            mSelectedItems.put(position, true);
        } else {
            mSelectedItems.delete(position);
        }

        notifyDataSetChanged();
    }

    public List<TalkClientMediaCollection> getSelectedItems() {
        List<TalkClientMediaCollection> collections = new ArrayList<TalkClientMediaCollection>();
        for (int i = 0; i < mMediaCollections.size(); ++i) {
            if (mSelectedItems.get(i)) {
                collections.add(mMediaCollections.get(i));
            }
        }

        return collections;
    }

    public void clearSelection() {
        mSelectedItems.clear();
        notifyDataSetChanged();
    }

    private void loadMediaCollections() throws SQLException {
        mMediaCollections = XoApplication.get().getXoClient().getDatabase().findAllMediaCollections();
    }

    @Override
    public void onMediaCollectionCreated(TalkClientMediaCollection collectionCreated) {
        mMediaCollections.add(collectionCreated);
        notifyDataSetChanged();
    }

    @Override
    public void onMediaCollectionDeleted(TalkClientMediaCollection collectionDeleted) {
        TalkClientMediaCollection collectionToRemove = null;
        for (TalkClientMediaCollection collection : mMediaCollections) {
            if (collection.getId().equals(collectionDeleted.getId())) {
                collectionToRemove = collection;
                break;
            }
        }

        if (collectionToRemove != null) {
            mMediaCollections.remove(collectionToRemove);
        }

        notifyDataSetChanged();
    }

    private class ViewHolder {
        public TextView titleName;
        public ImageView goToImageView;
    }
}
