package com.hoccer.xo.android.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.*;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.model.TalkClientMediaCollection;
import com.hoccer.xo.android.activity.ContactSelectionActivity;
import com.hoccer.xo.android.activity.MediaCollectionSelectionActivity;
import com.hoccer.xo.android.adapter.AttachmentListAdapter;
import com.hoccer.xo.android.adapter.AttachmentSearchResultAdapter;
import com.hoccer.xo.android.content.AudioAttachmentItem;
import com.hoccer.xo.android.database.AndroidTalkDatabase;
import com.hoccer.xo.android.dialog.AttachmentRemovalDialogBuilder;
import com.hoccer.xo.android.util.AttachmentOperationHelper;
import com.hoccer.xo.android.util.DragSortController;
import com.hoccer.xo.release.R;
import com.mobeta.android.dslv.DragSortListView;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by nico on 22/07/2014.
 */
public class CollectionListFragment extends SearchableListFragment {

    private static final Logger LOG = Logger.getLogger(CollectionListFragment.class);

    public static final int SELECT_COLLECTION_REQUEST = 1;
    public static final int SELECT_CONTACT_REQUEST = 2;

    private DragSortListView mListView;
    private DragSortController mController;
    private AttachmentListAdapter mAttachmentAdapter;
    private AttachmentSearchResultAdapter mSearchResultAdapter;
    private TalkClientMediaCollection mCollection;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAttachmentAdapter = new AttachmentListAdapter();

        int collectionId = -1;
        if (getArguments() != null) {
            collectionId= getArguments().getInt(AttachmentOperationHelper.ARG_MEDIA_COLLECTION_ID);
        }

        if (collectionId > 0) {
            try {
                XoClientDatabase database = new XoClientDatabase(AndroidTalkDatabase.getInstance(getActivity().getApplicationContext()));
                database.initialize();
                mCollection = database.findMediaCollectionById(collectionId);
                mAttachmentAdapter.loadAttachmentsFromCollection(mCollection);
            } catch (SQLException e) {
                // TODO display error message?
                LOG.error(e);
            }
        } else {
            LOG.warn("No Media Collection ID transmitted");
        }

        setListAdapter(mAttachmentAdapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mListView = (DragSortListView) inflater.inflate(R.layout.fragment_collection_list, null);

        mController = new DragSortController(mListView);
        mController.setDragHandleId(R.id.list_drag_handle);
        mController.setSortEnabled(false);
        mController.setRemoveEnabled(false);

        mListView.setDragEnabled(false);
        mListView.setOnTouchListener(mController);
        mListView.setFloatViewManager(mController);
        mListView.setChoiceMode(DragSortListView.CHOICE_MODE_MULTIPLE_MODAL);

        ListInteractionHandler handler = new ListInteractionHandler();
        mListView.setMultiChoiceModeListener(handler);

        return mListView;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected ListAdapter searchInAdapter(String query) {
        mSearchResultAdapter.searchForAttachments(query);

        return mSearchResultAdapter;
    }

    @Override
    protected void onSearchModeEnabled() {
        if (mSearchResultAdapter == null) {
            mSearchResultAdapter = new AttachmentSearchResultAdapter();
        } else {
            mSearchResultAdapter.clear();
        }

        mSearchResultAdapter.setAttachmentItems(mAttachmentAdapter.getAttachmentItems());
    }

    @Override
    protected void onSearchModeDisabled() {

    }

    // XXX: duplicate to the one in AudioAttachmentFragment
    private List<AudioAttachmentItem> getSelectedItems(SparseBooleanArray checkedItemPositions) {
        List<AudioAttachmentItem> selectedItems = new ArrayList<AudioAttachmentItem>();
        for (int i = 0; i < checkedItemPositions.size(); ++i) {
            if (checkedItemPositions.get(i)) {
                selectedItems.add(((AudioAttachmentItem)getListAdapter().getItem(i)));
            }
        }

        return selectedItems;
    }

    private void removeItemsFromAdapter(List<AudioAttachmentItem> items) {
        for (AudioAttachmentItem item : items) {
            mAttachmentAdapter.removeItem(item, true);
            mAttachmentAdapter.notifyDataSetChanged();
        }
    }

    private class ListInteractionHandler implements AdapterView.OnItemClickListener, AbsListView.MultiChoiceModeListener, DragSortListView.DragListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            mAttachmentAdapter.setSelections(getListView().getCheckedItemPositions());
            mAttachmentAdapter.notifyDataSetChanged();
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mListView.setDragEnabled(true);
            mController.setSortEnabled(true);
            mAttachmentAdapter.setSortEnabled(true);

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            List<AudioAttachmentItem> selectedItems = getSelectedItems(getListView().getCheckedItemPositions());
            switch (item.getItemId()) {
                case R.id.menu_delete_attachment:
                    AttachmentRemovalDialogBuilder builder = new AttachmentRemovalDialogBuilder(getActivity(), selectedItems);
                    DialogCallbackHandler handler = new DialogCallbackHandler();
                    builder.setRemoveFromCollectionCallbackHandler(handler);
                    builder.setDeleteCallbackHandler(handler);
                    AlertDialog removeDialog = builder.create();
                    removeDialog.show();
                    mode.finish();
                    return true;
                case R.id.menu_share:
                    mode.finish();
                    startActivityForResult(new Intent(getActivity(), ContactSelectionActivity.class), SELECT_CONTACT_REQUEST);
                    return false;
                case R.id.menu_add_to_collection:
                    mode.finish();
                    startActivityForResult(new Intent(getActivity(), MediaCollectionSelectionActivity.class), SELECT_COLLECTION_REQUEST);
                    return false;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mListView.setDragEnabled(false);
            mController.setSortEnabled(false);
            mAttachmentAdapter.setSortEnabled(false);

        }

        @Override
        public void drag(int from, int to) {
            getListView().dispatchSetSelected(false);
        }
    }

    private class DialogCallbackHandler implements AttachmentRemovalDialogBuilder.DeleteCallback,
            AttachmentRemovalDialogBuilder.RemoveFromCollectionCallback {

        @Override
        public void deleteAttachments(List<AudioAttachmentItem> attachments) {
            AttachmentOperationHelper.deleteAttachments(getActivity(), attachments);
            removeItemsFromAdapter(attachments);
        }

        @Override
        public void removeAttachmentsFromCollection(List<AudioAttachmentItem> attachments, int collectionId) {
            removeItemsFromAdapter(attachments);
        }
    }
}
