package com.hoccer.xo.android.fragment;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import com.hoccer.xo.android.adapter.NearbyChatAdapter;
import com.hoccer.xo.android.base.XoActivity;

/**
 * Created by nico on 08/08/2014.
 */
public class NearbyArchiveFragment extends ListFragment {

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setStackFromBottom(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        setListAdapter(new NearbyChatAdapter(getListView(), (XoActivity) getActivity()));
    }
}
