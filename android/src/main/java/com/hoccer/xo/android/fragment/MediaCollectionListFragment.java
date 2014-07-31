package com.hoccer.xo.android.fragment;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.*;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import com.hoccer.talk.client.model.TalkClientMediaCollection;
import com.hoccer.xo.android.util.AttachmentOperationHelper;
import com.hoccer.xo.release.R;
import com.mobeta.android.dslv.DragSortListView;
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
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        ListInteractionHandler handler = new ListInteractionHandler();
        getListView().setMultiChoiceModeListener(handler);
        getListView().setOnItemClickListener(handler);
    }

    private class ListInteractionHandler implements AdapterView.OnItemClickListener, AbsListView.MultiChoiceModeListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Integer mediaCollectionId = ((TalkClientMediaCollection) mMediaCollectionListAdapter.getItem(position)).getId();
            showCollectionListFragment(mediaCollectionId);
        }

        private void showCollectionListFragment(Integer mediaCollectionId) {
            Bundle bundle = new Bundle();
            bundle.putInt(AttachmentOperationHelper.ARG_MEDIA_COLLECTION_ID, mediaCollectionId);

            CollectionListFragment fragment = new CollectionListFragment();
            fragment.setArguments(bundle);

            FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.fl_fragment_container, fragment);
            ft.addToBackStack(null);
            ft.commit();
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode actionMode, int i, long l, boolean b) {

        }

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
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
            switch (menuItem.getItemId()) {
                case R.id.menu_delete_collection:

                    break;
                case R.id.menu_rename_collection:

                    break;
                default:

            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {

        }
    }
}