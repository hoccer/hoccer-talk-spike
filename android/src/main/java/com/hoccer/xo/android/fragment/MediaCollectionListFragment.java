package com.hoccer.xo.android.fragment;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.*;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import com.hoccer.talk.client.model.TalkClientMediaCollection;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.util.AttachmentOperationHelper;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

import java.sql.SQLException;

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
            bundle.putInt(AttachmentOperationHelper.ARG_MEDIA_COLLECTION_ID, mediaCollectionId);

            MediaCollectionItemListFragment fragment = new MediaCollectionItemListFragment();
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
            TalkClientMediaCollection selectedCollection = (TalkClientMediaCollection) getListView().getSelectedItem();
            if (selectedCollection != null) {
                switch (menuItem.getItemId()) {
                    case R.id.menu_delete_collection:
                        DeleteCollectionCallbackHandler deleteHandler = new DeleteCollectionCallbackHandler(selectedCollection);
                        XoDialogs.showYesNoDialog("delete_collection", R.string.dialog_delete_collection_title,
                                R.string.dialog_delete_collection_message, getActivity(), deleteHandler, new CancelDialogCallbackHandler());
                        eventHandled = true;
                        break;
                    default:
                }
            } else {
                XoDialogs.showOkDialog("select_collection", R.string.dialog_select_collection_title,
                        R.string.dialog_select_collection_message, getActivity(), null);
                eventHandled = true;
            }

            return eventHandled;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            mActionMode = null;
            mMediaCollectionListAdapter.clearSelection();
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode actionMode, int position, long id, boolean checked) {
            mMediaCollectionListAdapter.selectItem(position, checked);
        }

        private class DeleteCollectionCallbackHandler implements DialogInterface.OnClickListener {

            private TalkClientMediaCollection mCollection;

            DeleteCollectionCallbackHandler(TalkClientMediaCollection collection) {
                mCollection = collection;
            }

            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                try {
                    mCollection.clear();
                    XoApplication.getXoClient().getDatabase().deleteMediaCollection(mCollection);
                } catch (SQLException e) {
                    LOG.error(e.getMessage());
                }
            }
        }

        private class RenameCollectionCallbackHandler implements XoDialogs.OnTextSubmittedListener {

            private TalkClientMediaCollection mCollection;

            RenameCollectionCallbackHandler(TalkClientMediaCollection collection) {
                mCollection = collection;
            }

            @Override
            public void onClick(DialogInterface dialog, int id, String password) {
                if (password != null && !password.isEmpty()) {
                    mCollection.setName(password);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((BaseAdapter) getListAdapter()).notifyDataSetChanged();
                        }
                    });
                }
            }
        }

        private class CancelDialogCallbackHandler implements DialogInterface.OnClickListener {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        }
    }
}