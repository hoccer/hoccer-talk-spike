package com.hoccer.xo.android.fragment;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.*;
import android.widget.*;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientMediaCollection;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.activity.ContactSelectionActivity;
import com.hoccer.xo.android.activity.MediaCollectionSelectionActivity;
import com.hoccer.xo.android.adapter.AttachmentListAdapter;
import com.hoccer.xo.android.adapter.AttachmentSearchResultAdapter;
import com.hoccer.xo.android.database.AndroidTalkDatabase;
import com.hoccer.xo.android.util.AttachmentOperationHelper;
import com.hoccer.xo.android.util.DragSortController;
import com.hoccer.xo.release.R;
import com.mobeta.android.dslv.DragSortListView;
import org.apache.log4j.Logger;

import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MediaCollectionItemListFragment extends SearchableListFragment {

    private static final Logger LOG = Logger.getLogger(MediaCollectionItemListFragment.class);

    public static final int SELECT_COLLECTION_REQUEST = 1;
    public static final int SELECT_CONTACT_REQUEST = 2;

    private DragSortListView mListView;
    private DragSortController mController;
    private AttachmentListAdapter mAttachmentAdapter;
    private AttachmentSearchResultAdapter mSearchResultAdapter;
    private XoClientDatabase mDatabase;
    private TalkClientMediaCollection mCollection;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDatabase = new XoClientDatabase(
                AndroidTalkDatabase.getInstance(getActivity().getApplicationContext()));
        try {
            mDatabase.initialize();
        } catch (SQLException e) {
            LOG.error("sql error", e);
        }

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
    public void onStart() {
        super.onStart();

        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        ListInteractionHandler listHandler = new ListInteractionHandler();
        getListView().setMultiChoiceModeListener(listHandler);
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
    private List<IContentObject> getSelectedItems(SparseBooleanArray checkedItemPositions) {
        SparseBooleanArray selectedItemIds = new SparseBooleanArray();
        for (int i = 0; i < checkedItemPositions.size(); ++i) {
            selectedItemIds.append(checkedItemPositions.keyAt(i), checkedItemPositions.valueAt(i));
        }
        List<IContentObject> attachments = new ArrayList<IContentObject>();
        for (int index = 0; index < selectedItemIds.size(); ++index) {
            int pos = selectedItemIds.keyAt(index);
            if (selectedItemIds.get(pos)) {
                attachments.add(mAttachmentAdapter.getItem(pos));
            }
        }

        return attachments;
    }

    private void addSelectedItemsToCollection(Integer mediaCollectionId) {
        List<IContentObject> selectedItems = getSelectedItems(getListView().getCheckedItemPositions());
        if(selectedItems.size() > 0) {
            try {
                TalkClientMediaCollection mediaCollection = mDatabase.findMediaCollectionById(mediaCollectionId);
                List<String> addedFilenames = new ArrayList<String>();
                for (IContentObject item : selectedItems) {
                    mediaCollection.addItem((TalkClientDownload) item);
                    addedFilenames.add(item.getFileName());
                }
                Toast.makeText(getActivity(), String.format(getString(R.string.added_attachment_to_collection), addedFilenames, mediaCollection.getName()), Toast.LENGTH_LONG).show();
            } catch (SQLException e) {
                LOG.error("Could not find MediaCollection with id: " + String.valueOf(mediaCollectionId), e);
            }
        }
    }

    private TalkClientContact retrieveContactById(Integer contactId) {
        TalkClientContact contact = null;
        try {
            contact = mDatabase.findClientContactById(contactId);
        } catch (SQLException e) {
            LOG.error("Contact not found for id: " + contactId, e);
        }
        return contact;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case SELECT_COLLECTION_REQUEST:
                    Integer mediaCollectionId = data.getIntExtra(MediaCollectionSelectionListFragment.MEDIA_COLLECTION_ID_EXTRA, -1);
                    if (mediaCollectionId > -1) {
                        addSelectedItemsToCollection(mediaCollectionId);
                    }
                    break;
                case SELECT_CONTACT_REQUEST:
                    List<Integer> contactSelections = data.getIntegerArrayListExtra(ContactSelectionActivity.SELECTED_CONTACT_IDS_EXTRA);
                    // TODO better errorhandling!
                    for (Integer contactId : contactSelections) {
                        try {
                            TalkClientContact contact = retrieveContactById(contactId);
                            AttachmentOperationHelper.sendAttachmentsToContact(getSelectedItems(getListView().getCheckedItemPositions()), contact);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                        }
                    }

                    break;
            }
        }
    }

    private class ListInteractionHandler implements AbsListView.MultiChoiceModeListener, DragSortListView.DragListener {

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            mAttachmentAdapter.setCheckedItems(getListView().getCheckedItemPositions());
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mListView.setDragEnabled(true);
            mController.setSortEnabled(true);
            mAttachmentAdapter.setSortEnabled(true);
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.context_menu_fragment_messaging, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_delete_attachment:
                    XoDialogs.showSingleChoiceDialog("RemoveAttachment",
                            R.string.dialog_attachment_delete_title,
                            R.array.delete_options,
                            getActivity(),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    switch(id) {
                                        case 0:
                                            removeCheckedItemsFromCollection();
                                            break;
                                        case 1:
                                            deleteCheckedItems();
                                            break;
                                        default:
                                            throw new IllegalArgumentException("Invalid array index selected.");
                                    }
                                }
                            });
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

        private void removeCheckedItemsFromCollection() {
            SparseBooleanArray checkedItemPositions = getListView().getCheckedItemPositions();
            for (int i= 0; i < checkedItemPositions.size(); i++) {
                int listIndex = checkedItemPositions.keyAt(i);
                if(checkedItemPositions.valueAt(i)) {
                    mCollection.removeItem(listIndex);
                }
            }
        }

        private void deleteCheckedItems() {
            List<IContentObject> items = getSelectedItems(getListView().getCheckedItemPositions());
            for (IContentObject item : items) {
                try{
                    mDatabase.deleteClientDownloadAndMessage((TalkClientDownload)item);
                } catch(SQLException e) {
                    LOG.error("Could not delete download", e);
                }
            }
        }
    }
}
