package com.hoccer.xo.android.fragment;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientMediaCollection;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.activity.ContactSelectionActivity;
import com.hoccer.xo.android.activity.FullscreenPlayerActivity;
import com.hoccer.xo.android.activity.MediaCollectionSelectionActivity;
import com.hoccer.xo.android.adapter.AttachmentSearchResultAdapter;
import com.hoccer.xo.android.adapter.MediaCollectionItemAdapter;
import com.hoccer.xo.android.content.MediaCollectionPlaylist;
import com.hoccer.xo.android.content.SingleItemPlaylist;
import com.hoccer.xo.android.service.MediaPlayerServiceConnector;
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

public class MediaCollectionFragment extends SearchableListFragment {

    private static final Logger LOG = Logger.getLogger(MediaCollectionFragment.class);

    public static final int SELECT_COLLECTION_REQUEST = 1;
    public static final int SELECT_CONTACT_REQUEST = 2;

    private int mRenameMenuId = 0;

    private DragSortListView mListView;
    private DragSortController mController;
    private XoClientDatabase mDatabase;
    private TalkClientMediaCollection mCollection;
    private MediaCollectionItemAdapter mCollectionAdapter;
    private AttachmentSearchResultAdapter mSearchResultAdapter;

    private MediaPlayerServiceConnector mMediaPlayerServiceConnector = new MediaPlayerServiceConnector();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            try {
                mDatabase = XoApplication.getXoClient().getDatabase();
                int collectionId = getArguments().getInt(AttachmentOperationHelper.ARG_MEDIA_COLLECTION_ID);
                mCollection = mDatabase.findMediaCollectionById(collectionId);
                mCollectionAdapter = new MediaCollectionItemAdapter(mCollection);
                setListAdapter(mCollectionAdapter);
            } catch (SQLException e) {
                LOG.error(e);
            }

            mMediaPlayerServiceConnector.connect(getActivity());
        } else {
            LOG.error("No Media Collection ID transmitted");
        }
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
        getListView().setOnItemClickListener(listHandler);
        getListView().setMultiChoiceModeListener(listHandler);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        mRenameMenuId = R.id.menu_search + 1;
        MenuItem renameItem = menu.add(Menu.NONE, mRenameMenuId, Menu.NONE, R.string.rename_collection);
        renameItem.setIcon(R.drawable.ic_edit);
        renameItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        renameItem.setOnMenuItemClickListener(mRenameCollectionClickListener);
        updateActionBarTitle();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMediaPlayerServiceConnector.disconnect();
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
                            AttachmentOperationHelper.sendAttachmentsToContact(mCollectionAdapter.getSelectedItems(), contact);
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

    @Override
    protected ListAdapter searchInAdapter(String query) {
        mSearchResultAdapter.query(query);
        return mSearchResultAdapter;
    }

    @Override
    protected void onSearchModeEnabled() {
        mSearchResultAdapter = new AttachmentSearchResultAdapter(mCollection.toArray());
    }

    @Override
    protected void onSearchModeDisabled() {
        // do nothing
    }

    private MenuItem.OnMenuItemClickListener mRenameCollectionClickListener = new MenuItem.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            XoDialogs.showInputTextDialog("rename_collection", R.string.rename_collection, getActivity(),
                    mTextSubmittedListener);
            return false;
        }

        private XoDialogs.OnTextSubmittedListener mTextSubmittedListener = new XoDialogs.OnTextSubmittedListener() {
            @Override
            public void onClick(DialogInterface dialog, int id, String text) {
                if (text != null && !text.isEmpty()) {
                    mCollection.setName(text);
                    updateActionBarTitle();
                }
            }
        };
    };

    private void updateActionBarTitle() {
        getActivity().getActionBar().setTitle(mCollection.getName());
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

    private void addSelectedItemsToCollection(Integer mediaCollectionId) {
        List<TalkClientDownload> selectedItems = mCollectionAdapter.getAllSelectedItems();
        if(selectedItems.size() > 0) {
            try {
                TalkClientMediaCollection mediaCollection = mDatabase.findMediaCollectionById(mediaCollectionId);
                List<String> addedFilenames = new ArrayList<String>();
                for (TalkClientDownload item : selectedItems) {
                    mediaCollection.addItem((TalkClientDownload) item);
                    addedFilenames.add(item.getFileName());
                }
                Toast.makeText(getActivity(), String.format(getString(R.string.added_attachment_to_collection), addedFilenames, mediaCollection.getName()), Toast.LENGTH_LONG).show();
            } catch (SQLException e) {
                LOG.error("Could not find MediaCollection with id: " + String.valueOf(mediaCollectionId), e);
            }
        }
    }

    private class ListInteractionHandler implements AdapterView.OnItemClickListener, AbsListView.MultiChoiceModeListener, DragSortListView.DragListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            TalkClientDownload clickedItem = (TalkClientDownload)getListAdapter().getItem(position);
            if (mMediaPlayerServiceConnector.isConnected()) {
                if (isSearchModeEnabled()) {
                    mMediaPlayerServiceConnector.getService().setPlaylist(new SingleItemPlaylist(XoApplication.getXoClient().getDatabase(), clickedItem));
                } else {
                    MediaCollectionPlaylist playlist = new MediaCollectionPlaylist(mCollection);
                    mMediaPlayerServiceConnector.getService().setPlaylist(playlist);
                    mMediaPlayerServiceConnector.getService().play(position);
                }
                getActivity().startActivity(new Intent(getActivity(), FullscreenPlayerActivity.class));
            } else {
                LOG.error("MediaPlayerService is not connected");
            }
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            if(checked) {
                mCollectionAdapter.selectItem((int)id);
            } else {
                mCollectionAdapter.deselectItem((int) id);
            }
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mListView.setDragEnabled(true);
            mController.setSortEnabled(true);
            mCollectionAdapter.showDragHandle(true);
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.context_menu_fragment_messaging, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
            final List<TalkClientDownload> selectedItems = mCollectionAdapter.getAllSelectedItems();
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
                                            removeItemsFromCollection(selectedItems);
                                            break;
                                        case 1:
                                            deleteItems(selectedItems);
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
            mCollectionAdapter.showDragHandle(false);
            mCollectionAdapter.deselectAllItems();
        }

        @Override
        public void drag(int from, int to) {
            getListView().dispatchSetSelected(false);
        }

        private void removeItemsFromCollection(List<TalkClientDownload> items) {
            for (TalkClientDownload item : items) {
                mCollection.removeItem(item);
            }
        }

        private void deleteItems(List<TalkClientDownload> items) {
            for (TalkClientDownload item : items) {
                try{
                    mDatabase.deleteClientDownloadAndMessage(item);
                } catch(SQLException e) {
                    LOG.error("Could not delete download", e);
                }
            }
        }
    }
}
