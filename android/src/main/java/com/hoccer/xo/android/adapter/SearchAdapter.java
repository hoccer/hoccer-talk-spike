package com.hoccer.xo.android.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class SearchAdapter extends BaseAdapter {

    static final Logger LOG = Logger.getLogger(SearchAdapter.class);

    private final BaseAdapter mAdapter;
    private final LinkedHashMap<Searchable, Integer> mItemIndexMap = new LinkedHashMap<Searchable, Integer>();
    private final List<Object> mFoundItemList = new ArrayList<Object>();


    public SearchAdapter(BaseAdapter adapter) {
        mAdapter = adapter;
        for (int i = 0; i < adapter.getCount(); ++i) {
            try {
                mItemIndexMap.put((Searchable) mAdapter.getItem(i), i);
            } catch (ClassCastException e) {
                LOG.error("Item in adapter is not searchable", e);
            }
        }
    }

    @Override
    public int getCount() {
        return mFoundItemList.size();
    }

    @Override
    public Object getItem(int position) {
        return mFoundItemList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int adapterPosition = mItemIndexMap.get(mFoundItemList.get(position));
        return mAdapter.getView(adapterPosition, convertView, parent);
    }

    public SearchAdapter query(String query){
        mFoundItemList.clear();

        if (query != null && !query.isEmpty()) {
            for (Searchable item : mItemIndexMap.keySet()) {
                if (item.matches(query)) {
                    mFoundItemList.add(item);
                }
            }
        }

        notifyDataSetChanged();

        return this;
    }

    public interface Searchable {
        boolean matches(String query);
    }
}
