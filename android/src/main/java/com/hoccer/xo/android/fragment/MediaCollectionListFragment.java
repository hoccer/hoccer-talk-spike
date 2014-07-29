package com.hoccer.xo.android.fragment;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.*;
import android.widget.AdapterView;
import com.hoccer.talk.client.model.TalkClientMediaCollection;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

public class MediaCollectionListFragment extends BaseMediaCollectionListFragment {

    private final static Logger LOG = Logger.getLogger(AudioAttachmentListFragment.class);

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
            showAudioAttachmentListFragment(mediaCollectionId);
        }
    }

    private void showAudioAttachmentListFragment(Integer mediaCollectionId) {
        Bundle bundle = new Bundle();
        bundle.putInt(AudioAttachmentListFragment.ARG_MEDIA_COLLECTION_ID, mediaCollectionId);

        AudioAttachmentListFragment audioAttachmentListFragment = new AudioAttachmentListFragment();
        audioAttachmentListFragment.setArguments(bundle);

        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fl_fragment_container, audioAttachmentListFragment);
        ft.addToBackStack(null);
        ft.commit();
    }
}