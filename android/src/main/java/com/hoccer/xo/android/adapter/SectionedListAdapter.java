package com.hoccer.xo.android.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.artcom.hoccer.R;

import java.util.ArrayList;
import java.util.List;

public class SectionedListAdapter extends BaseAdapter {

    private final List<Section> mSections = new ArrayList<Section>();
    private static int sTypeSectionHeader;

    public void addSection(String caption, Adapter adapter) {
        mSections.add(new Section(caption, adapter));
        notifyDataSetChanged();
    }

    public void clear() {
        mSections.clear();
        notifyDataSetChanged();
    }

    @Override
    public Object getItem(int position) {
        for (Section section : this.mSections) {
            if (position == 0) {
                return section;
            }

            int size = section.getAdapter().getCount() + 1;

            if (position < size) {
                return section.getAdapter().getItem(position - 1);
            }

            position -= size;
        }

        return null;
    }

    @Override
    public int getCount() {
        int total = 0;

        for (Section section : mSections) {
            total += section.getAdapter().getCount() + 1;
        }

        return total;
    }

    @Override
    public int getViewTypeCount() {
        int total = 1;

        for (Section section : mSections) {
            total += section.getAdapter().getViewTypeCount();
        }

        return total;
    }

    @Override
    public int getItemViewType(int position) {
        int typeOffset = sTypeSectionHeader + 1;

        for (Section section : this.mSections) {
            if (position == 0) {
                return sTypeSectionHeader;
            }

            int size = section.getAdapter().getCount() + 1;

            if (position < size) {
                return typeOffset + section.getAdapter().getItemViewType(position - 1);
            }

            position -= size;
            typeOffset += section.getAdapter().getViewTypeCount();
        }

        return -1;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        return getItemViewType(position) != sTypeSectionHeader;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int sectionIndex = 0;
        int itemViewType = getItemViewType(position);
        View reusableConvertView = convertView != null && convertView.getTag().equals(itemViewType) ? convertView : null;
        View v = null;

        for (Section section : this.mSections) {
            if (position == 0) {
                v = getHeaderView(section.getCaption(), sectionIndex, reusableConvertView, parent);
                break;
            }

            // size includes the header
            int size = section.getAdapter().getCount() + 1;

            if (position < size) {
                v = section.getAdapter().getView(position - 1, reusableConvertView, parent);
                break;
            }

            position -= size;
            sectionIndex++;
        }

        v.setTag(itemViewType);
        return v;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private View getHeaderView(String caption, int index,
                               View convertView, ViewGroup parent) {
        View headerView = convertView;
        if (headerView == null) {
            headerView = View.inflate(parent.getContext(), R.layout.item_section_header, null);
            TextView captionView = (TextView) headerView.findViewById(R.id.tv_section_header);
            captionView.setText(caption);
        }

        return headerView;
    }

    class Section {
        private String mCaption;
        private Adapter mAdapter;

        Section(String caption, Adapter adapter) {
            mCaption = caption;
            mAdapter = adapter;
        }

        String getCaption() {
            return mCaption;
        }

        Adapter getAdapter() {
            return mAdapter;
        }
    }
}
