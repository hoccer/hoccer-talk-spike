package com.hoccer.xo.android.fragment;

import android.app.Activity;
import android.app.SearchManager;
import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ListFragment;
import android.util.SparseBooleanArray;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientMediaCollection;
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.activity.ContactSelectionActivity;
import com.hoccer.xo.android.activity.FullscreenPlayerActivity;
import com.hoccer.xo.android.activity.MediaCollectionSelectionActivity;
import com.hoccer.xo.android.adapter.AttachmentListAdapter;
import com.hoccer.xo.android.adapter.AttachmentSearchResultAdapter;
import com.hoccer.xo.android.adapter.ContactSearchResultAdapter;
import com.hoccer.xo.android.adapter.SearchResultsAdapter;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.content.AttachmentAdapterDownloadHandler;
import com.hoccer.xo.android.database.AndroidTalkDatabase;
import com.hoccer.xo.android.content.SingleItemPlaylist;
import com.hoccer.xo.android.content.UserPlaylist;
import com.hoccer.xo.android.service.MediaPlayerService;
import com.hoccer.xo.android.util.AttachmentOperationHelper;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AudioAttachmentListFragment extends ListFragment {

    public static final String ARG_CLIENT_CONTACT_ID = "com.hoccer.xo.android.fragment.ARG_CLIENT_CONTACT_ID";
    public static final String ARG_CONTENT_MEDIA_TYPE = "com.hoccer.xo.android.fragment.ARG_CONTENT_MEDIA_TYPE";

    public static final int SELECT_COLLECTION_REQUEST = 1;
    public static final int SELECT_CONTACT_REQUEST = 2;

    public static final int ALL_CONTACTS_ID = -1;
    private SparseBooleanArray mSelectedItems;

    private MediaPlayerService mMediaPlayerService;

    private final static Logger LOG = Logger.getLogger(AudioAttachmentListFragment.class);

    private ServiceConnection mConnection;

    private AttachmentListAdapter mAttachmentListAdapter;
    private AttachmentAdapterDownloadHandler mTransferListener;
    private SearchResultsAdapter mResultsAdapter;
    private ContactSearchResultAdapter mSearchContactsAdapter;
    private AttachmentSearchResultAdapter mSearchAttachmentAdapter;
    private int mContactIdFilter = ALL_CONTACTS_ID;
    private MenuItem mSearchMenuItem;
    private String mContentMediaTypeFilter = ContentMediaType.AUDIO;
    private boolean mInSearchMode = false;
    private XoClientDatabase mDatabase;

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

        setHasOptionsMenu(true);
        mAttachmentListAdapter = new AttachmentListAdapter();
        mAttachmentListAdapter.setContactIdFilter(mContactIdFilter);
        mAttachmentListAdapter.setContentMediaTypeFilter(mContentMediaTypeFilter);
        loadAttachments();

        mTransferListener = new AttachmentAdapterDownloadHandler(getActivity(), mAttachmentListAdapter);
        XoApplication.getXoClient().getDatabase().registerDownloadListener(mTransferListener);

        mSearchContactsAdapter = new ContactSearchResultAdapter((XoActivity) getActivity());
        mSearchContactsAdapter.onCreate();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_audio_attachment_list, container, false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case SELECT_COLLECTION_REQUEST:
                    Integer mediaCollectionId = data.getIntExtra(MediaCollectionSelectionListFragment.MEDIA_COLLECTION_ID_EXTRA, -1);
                    if (mediaCollectionId > -1) {
                        addSelectedAttachmentsToCollection(mediaCollectionId);
                    }
                    break;
                case SELECT_CONTACT_REQUEST:
                    List<Integer> contactSelections = data.getIntegerArrayListExtra(ContactSelectionActivity.SELECTED_CONTACT_IDS_EXTRA);
                    // TODO better errorhandling!
                    for (Integer contactId : contactSelections) {
                        try {
                            TalkClientContact contact = retrieveContactById(contactId);
                            AttachmentOperationHelper.sendAttachmentsToContact(getSelectedAttachments(), contact);
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
    public void onStart() {
        super.onStart();

        mSearchContactsAdapter.requestReload();

        XoApplication.getXoClient().registerTransferListener(mTransferListener);

        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        ListInteractionHandler listHandler = new ListInteractionHandler();
        getListView().setOnItemClickListener(listHandler);
        getListView().setMultiChoiceModeListener(listHandler);

        Intent intent = new Intent(getActivity(), MediaPlayerService.class);
        getActivity().startService(intent);
        bindToMediaPlayerService(intent);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mSearchContactsAdapter == null) {
            mSearchContactsAdapter = new ContactSearchResultAdapter((XoActivity) getActivity());
            mSearchContactsAdapter.onCreate();
            mSearchContactsAdapter.requestReload();
        }

        mSearchContactsAdapter.onResume();

        if (mSearchMenuItem != null && mSearchMenuItem.isActionViewExpanded()) {
            toggleSearchMode(true);
            getListView().setAdapter(mResultsAdapter);
        } else {
            getListView().setAdapter(mAttachmentListAdapter);
        }

        getActivity().getActionBar().setTitle(R.string.menu_music_viewer);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_searchable_list, menu);
        menu.findItem(R.id.menu_collections).setVisible(true);
        setupSearchWidget(menu);
    }

    @Override
    public void onPause() {
        super.onPause();
        setListAdapter(null);
    }

    @Override
    public void onStop() {
        super.onStop();
        XoApplication.getXoClient().unregisterTransferListener(mTransferListener);
        getActivity().unbindService(mConnection);
    }

    @Override
    public void onDestroy() {
        XoApplication.getXoClient().getDatabase().unregisterDownloadListener(mTransferListener);
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_search:
                boolean success = getActivity().onSearchRequested();
                if (!success) {
                    LOG.warn("Failed to process search request.");
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadAttachments() {
        mAttachmentListAdapter.clear();
        mAttachmentListAdapter.setContactIdFilter(mContactIdFilter);
        mAttachmentListAdapter.setContentMediaTypeFilter(mContentMediaTypeFilter);
        mAttachmentListAdapter.loadAttachments();
    }

    private boolean isPaused(IContentObject item) {
        if (mMediaPlayerService != null && mMediaPlayerService.isPaused()) {
            if (item.equals(mMediaPlayerService.getCurrentMediaItem())) {
                return true;
            }
        }

        return false;
    }

    private boolean isPlaying(IContentObject item) {
        if (mMediaPlayerService != null && !mMediaPlayerService.isStopped() && !mMediaPlayerService.isPaused()) {
            if (item.equals(mMediaPlayerService.getCurrentMediaItem())) {
                return true;
            }
        }

        return false;
    }

    private void bindToMediaPlayerService(Intent intent) {
        mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                MediaPlayerService.MediaPlayerBinder binder = (MediaPlayerService.MediaPlayerBinder) service;
                mMediaPlayerService = binder.getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mMediaPlayerService = null;
            }
        };

        getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    private void setupSearchWidget(Menu menu) {

        SearchActionHandler handler = new SearchActionHandler();

        mSearchMenuItem = menu.findItem(R.id.menu_search);
        mSearchMenuItem.setOnActionExpandListener(handler);

        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) mSearchMenuItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
        searchView.setIconifiedByDefault(false);
        searchView.setOnQueryTextListener(handler);

    }

    private void searchAttachmentList(final String query) {
        if (mInSearchMode) {
            mResultsAdapter.clear();

                mSearchContactsAdapter.searchForContactsByName(query);
                if (mSearchContactsAdapter.getCount() > 0) {
                    mResultsAdapter.addSection(getString(R.string.search_section_caption_contacts),
                            mSearchContactsAdapter);
                }

            mSearchAttachmentAdapter.searchForAttachments(query);
            if (mSearchAttachmentAdapter.getCount() > 0) {
                mResultsAdapter.addSection(getString(R.string.search_section_caption_audio_files),
                        mSearchAttachmentAdapter);
            }

//            setListAdapter(mResultsAdapter);
        }
    }

    private void filterAttachmentsByContactId(int selectedContactId) {
        if (mContactIdFilter != selectedContactId) {
            mContactIdFilter = selectedContactId;
            mAttachmentListAdapter.clear();
            mAttachmentListAdapter.setContactIdFilter(mContactIdFilter);
            mAttachmentListAdapter.setContentMediaTypeFilter(mContentMediaTypeFilter);
            mAttachmentListAdapter.loadAttachments();
            updateListView(mAttachmentListAdapter);
        }
    }

    private void showSoftKeyboard() {
        InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }

    private void toggleSearchMode(boolean toggle) {
        if (toggle) {
            mInSearchMode = true;
            if (mResultsAdapter == null) {
                mResultsAdapter = new SearchResultsAdapter();
            }

            setListAdapter(mResultsAdapter);

            if (mSearchAttachmentAdapter == null) {
                mSearchAttachmentAdapter = new AttachmentSearchResultAdapter();
            }

            mSearchAttachmentAdapter.setAttachmentItems(mAttachmentListAdapter.getAttachmentItems());

        } else {
            mInSearchMode = false;
            mResultsAdapter = null;
            mSearchAttachmentAdapter = null;
        }
    }

    private void setSelectedItems(SparseBooleanArray checkedItemPositions) {
        mSelectedItems = new SparseBooleanArray();
        for (int i = 0; i < checkedItemPositions.size(); ++i) {
            mSelectedItems.append(checkedItemPositions.keyAt(i), checkedItemPositions.valueAt(i));
        }
    }

    private void addSelectedAttachmentsToCollection(Integer mediaCollectionId) {
        try {
            retrieveCollectionAndAddSelectedAttachments(mediaCollectionId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void retrieveCollectionAndAddSelectedAttachments(Integer mediaCollectionId) throws SQLException {
        TalkClientMediaCollection mediaCollection = mDatabase.findMediaCollectionById(mediaCollectionId);
        List<String> addedFilenames = new ArrayList<String>();
        for (TalkClientDownload download : getSelectedDownloads()) {
            if (addAttachmentToCollection(mediaCollection, download)) {
                addedFilenames.add(download.getFileName());
            }
        }
        if (!addedFilenames.isEmpty()) {
            Toast.makeText(getActivity(), String.format(getString(R.string.added_attachment_to_collection), addedFilenames, mediaCollection.getName()), Toast.LENGTH_LONG).show();
        }
    }

    private boolean addAttachmentToCollection(TalkClientMediaCollection mediaCollection, TalkClientDownload item) {
        if (!mediaCollection.hasItem(item)) {
            mediaCollection.addItem(item);
            return true;
        }
        return false;
    }

    private TalkClientContact retrieveContactById(Integer contactId) {
        TalkClientContact contact = null;
        try {
            contact = XoApplication.getXoClient().getDatabase().findClientContactById(contactId);
        } catch (SQLException e) {
            LOG.error("Contact not found for id: " + contactId, e);
        }
        return contact;
    }

    private List<IContentObject> getSelectedAttachments() {
        List<IContentObject> attachments = new ArrayList<IContentObject>();
        for (int index = 0; index < mSelectedItems.size(); ++index) {
            int pos = mSelectedItems.keyAt(index);
            if (mSelectedItems.get(pos)) {
                attachments.add(mAttachmentListAdapter.getItem(pos));
            }
        }
        return attachments;
    }

    private List<TalkClientDownload> getSelectedDownloads() {
        List<TalkClientDownload> downloads = new ArrayList<TalkClientDownload>();
        for (int index = 0; index < mSelectedItems.size(); ++index) {
            int pos = mSelectedItems.keyAt(index);
            if (mSelectedItems.get(pos)) {
                TalkClientDownload download = (TalkClientDownload) mAttachmentListAdapter.getItem(pos);
                downloads.add(download);
            }
        }
        return downloads;
    }

    private void updateListView(final BaseAdapter adapter) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });
    }

    private class ListInteractionHandler implements AdapterView.OnItemClickListener, AbsListView.MultiChoiceModeListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            IContentObject selectedItem = (IContentObject)getListAdapter().getItem(position);

            if (selectedItem != null) {
                if (mInSearchMode) {
                    position = 0;
                    mMediaPlayerService.setPlaylist(new SingleItemPlaylist(XoApplication.getXoClient().getDatabase(), selectedItem));
                } else {
                    setMediaList();
                }

                if (isPlaying(selectedItem)) {
                    mMediaPlayerService.setCurrentIndex(position);
                } else if (isPaused(selectedItem)) {
                    mMediaPlayerService.setCurrentIndex(position);
                    mMediaPlayerService.play();
                } else {
                    mMediaPlayerService.play(position);
                }

                getActivity().startActivity(new Intent(getActivity(), FullscreenPlayerActivity.class));
            } else if (selectedItem instanceof TalkClientContact) {
                toggleSearchMode(false);
                mSearchMenuItem.collapseActionView();
                filterAttachmentsByContactId(((TalkClientContact) selectedItem).getClientContactId());
                setListAdapter(mAttachmentListAdapter);
            }
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            mAttachmentListAdapter.setCheckedItemsPositions(getListView().getCheckedItemPositions());
            updateListView(mAttachmentListAdapter);
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
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

            setSelectedItems(getListView().getCheckedItemPositions());

            switch (item.getItemId()) {
                case R.id.menu_delete_attachment:
                    XoDialogs.showYesNoDialog("RemoveAttachment", R.string.dialog_attachment_delete_title, R.string.dialog_attachment_delete_message, getActivity(),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    deleteSelectedAttachments();
                                }
                            }, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
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
        }

        private void setMediaList() {
            int contactId = mAttachmentListAdapter.getConversationContactId();
            TalkClientContact contact;
            try {
                contact = mDatabase.findContactById(contactId);
            } catch (SQLException e) {
                LOG.error("Could not retrieve contact from database", e);
                return;
            }
            mMediaPlayerService.setPlaylist(new UserPlaylist(mDatabase, contact));
        }
    }

    private class SearchActionHandler implements SearchView.OnQueryTextListener, MenuItem.OnActionExpandListener {

        @Override
        public boolean onQueryTextSubmit(String query) {
            showSoftKeyboard();

            return true;
        }

        @Override
        public boolean onQueryTextChange(final String query) {
            if (mInSearchMode) {
                searchAttachmentList(query);
            }

            return false;
        }

        @Override
        public boolean onMenuItemActionExpand(MenuItem item) {
            if (item.getItemId() == R.id.menu_search) {
                toggleSearchMode(true);
                SearchView searchView = (SearchView) item.getActionView();
                searchAttachmentList(searchView.getQuery().toString());
            }

            return true;
        }

        @Override
        public boolean onMenuItemActionCollapse(MenuItem item) {
            if (item.getItemId() == R.id.menu_search) {
                toggleSearchMode(false);
                setListAdapter(mAttachmentListAdapter);
            }

            return true;
        }
    }

    private void deleteSelectedAttachments() {
        List<IContentObject> selectedObjects = getSelectedAttachments();
        for(IContentObject item : selectedObjects) {
            TalkClientDownload download = (TalkClientDownload)item;
            try {
                XoApplication.getXoClient().getDatabase().deleteClientDownloadAndMessage(download);
            } catch (SQLException e) {
                LOG.error(e);
            }
            mAttachmentListAdapter.removeItem(item);
        }
        updateListView(mAttachmentListAdapter);
    }
}
