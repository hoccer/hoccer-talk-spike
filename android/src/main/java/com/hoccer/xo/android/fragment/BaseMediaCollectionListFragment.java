package com.hoccer.xo.android.fragment;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.hoccer.talk.client.model.TalkClientMediaCollection;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.adapter.MediaCollectionListAdapter;
import com.artcom.hoccer.R;
import org.apache.log4j.Logger;

import java.sql.SQLException;

public abstract class BaseMediaCollectionListFragment extends ListFragment {

    private final static Logger LOG = Logger.getLogger(BaseMediaCollectionListFragment.class);

    protected MediaCollectionListAdapter mMediaCollectionListAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        initAndFillMediaCollectionListAdapter();
    }

    @Override
    public void onStart() {
        super.onStart();
        setListAdapter(mMediaCollectionListAdapter);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_media_collection_list, menu);
        getActivity().getActionBar().setTitle(getString(R.string.collections));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add_media_collection:
                showCreateMediaCollectionDialog();
        }
        return super.onOptionsItemSelected(item);
    }

    protected void initAndFillMediaCollectionListAdapter() {
        mMediaCollectionListAdapter = new MediaCollectionListAdapter();
    }

    protected void showCreateMediaCollectionDialog() {
        XoDialogs.showInputTextDialog("", R.string.new_media_collection, R.string.enter_new_collection_name, getActivity(),
                new XoDialogs.OnTextSubmittedListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, String input) {
                        addNewMediaCollection(input);
                    }
                },
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }
        );
    }

    private void addNewMediaCollection(String name) {
        try {
            XoApplication.getXoClient().getDatabase().createMediaCollection(name);
        } catch (SQLException e) {
            LOG.error("Creating new media collection failed.", e);
        }
    }
}
