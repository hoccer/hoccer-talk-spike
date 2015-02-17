package com.hoccer.xo.android.fragment;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.*;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.model.TalkClientMediaCollection;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.XoDialogs;
import org.apache.log4j.Logger;

import java.sql.SQLException;

public class MediaCollectionListFragment extends BaseMediaCollectionListFragment {

    private final static Logger LOG = Logger.getLogger(MediaCollectionListFragment.class);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        return inflater.inflate(R.layout.fragment_media_collection_list, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        ListInteractionHandler handler = new ListInteractionHandler();
        getListView().setMultiChoiceModeListener(handler);
        getListView().setOnItemClickListener(handler);
    }

    private class ListInteractionHandler implements AdapterView.OnItemClickListener, AbsListView.MultiChoiceModeListener {

        private ActionMode mActionMode;

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (mActionMode == null) {
                Integer mediaCollectionId = ((TalkClientMediaCollection) mMediaCollectionListAdapter.getItem(position)).getId();
                showCollectionListFragment(mediaCollectionId);
            }
        }

        private void showCollectionListFragment(Integer mediaCollectionId) {
            Bundle bundle = new Bundle();
            bundle.putInt(MediaCollectionFragment.ARG_MEDIA_COLLECTION_ID, mediaCollectionId);

            MediaCollectionFragment fragment = new MediaCollectionFragment();
            fragment.setArguments(bundle);

            FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.fl_fragment_container, fragment);
            ft.addToBackStack(null);
            ft.commit();
        }

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            mActionMode = actionMode;
            MenuInflater inflater = actionMode.getMenuInflater();
            inflater.inflate(R.menu.context_menu_collection_list, menu);

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            boolean eventHandled = false;

            switch (menuItem.getItemId()) {
                case R.id.menu_delete_collection:
                    XoDialogs.showYesNoDialog("delete_collection", R.string.dialog_delete_collection_title,
                            R.string.dialog_delete_collection_message, getActivity(),
                            new DeleteCollectionCallbackHandler());
                    eventHandled = true;
                    break;
                default:

            }

            return eventHandled;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            mActionMode = null;
            mMediaCollectionListAdapter.clearSelection();
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode actionMode, final int position, long id, final boolean checked) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mMediaCollectionListAdapter.selectItem(position, checked);
                }
            });
        }

        private class DeleteCollectionCallbackHandler implements DialogInterface.OnClickListener {

            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                try {
                    for (TalkClientMediaCollection collection : mMediaCollectionListAdapter.getSelectedItems()) {
                        XoApplication.get().getXoClient().getDatabase().deleteMediaCollection(collection);
                    }
                } catch (SQLException e) {
                    LOG.error(e.getMessage());
                } finally {
                    mActionMode.finish();
                }
            }
        }
    }
}