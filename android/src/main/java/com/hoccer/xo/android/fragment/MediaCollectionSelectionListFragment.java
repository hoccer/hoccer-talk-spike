package com.hoccer.xo.android.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import com.hoccer.talk.client.model.TalkClientMediaCollection;
import com.artcom.hoccer.R;
import org.apache.log4j.Logger;

public class MediaCollectionSelectionListFragment extends BaseMediaCollectionListFragment {

    public static final String MEDIA_COLLECTION_ID_EXTRA = "com.hoccer.xo.android.fragment.MEDIA_COLLECTION_ID_EXTRA";

    private final static Logger LOG = Logger.getLogger(MediaCollectionSelectionListFragment.class);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_media_collection_list, container, false);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        getListView().setOnItemClickListener(new ListInteractionHandler());
    }

    private class ListInteractionHandler implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Integer mediaCollectionId = ((TalkClientMediaCollection) mMediaCollectionListAdapter.getItem(position)).getId();
            Intent resultIntent = new Intent();
            resultIntent.putExtra(MEDIA_COLLECTION_ID_EXTRA, mediaCollectionId);
            getActivity().setResult(getActivity().RESULT_OK, resultIntent);
            getActivity().finish();
        }
    }

}
