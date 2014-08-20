package com.hoccer.xo.android.fragment;

import android.app.Activity;
import android.app.SearchManager;
import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ListFragment;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.XoTransfer;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientMediaCollection;
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.activity.ContactSelectionActivity;
import com.hoccer.xo.android.activity.FullscreenPlayerActivity;
import com.hoccer.xo.android.activity.MediaCollectionSelectionActivity;
import com.hoccer.xo.android.adapter.AttachmentListAdapter;
import com.hoccer.xo.android.adapter.AttachmentSearchResultAdapter;
import com.hoccer.xo.android.adapter.ContactSearchResultAdapter;
import com.hoccer.xo.android.adapter.SectionedListAdapter;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.content.MediaPlaylist;
import com.hoccer.xo.android.content.SingleItemPlaylist;
import com.hoccer.xo.android.content.UserPlaylist;
import com.hoccer.xo.android.service.MediaPlayerService;
import com.hoccer.xo.android.util.ContactOperations;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AttachmentListFragment extends ListFragment {

    public static final String ARG_CLIENT_CONTACT_ID = "com.hoccer.xo.android.fragment.ARG_CLIENT_CONTACT_ID";
    public static final String ARG_CONTENT_MEDIA_TYPE = "com.hoccer.xo.android.fragment.ARG_CONTENT_MEDIA_TYPE";

    public static final int SELECT_COLLECTION_REQUEST = 1;
    public static final int SELECT_CONTACT_REQUEST = 2;

    public static final int ALL_CONTACTS_ID = -1;

    private MediaPlayerService mMediaPlayerService;

    private final static Logger LOG = Logger.getLogger(AttachmentListFragment.class);

    private ServiceConnection mConnection;

    private AttachmentListAdapter mAttachmentAdapter;
    private SectionedListAdapter mResultsAdapter;
    private ContactSearchResultAdapter mSearchContactsAdapter;
    private AttachmentSearchResultAdapter mSearchAttachmentAdapter;
    private MenuItem mSearchMenuItem;
    private String mContentMediaTypeFilter = ContentMediaType.AUDIO;
    private boolean mInSearchMode = false;
    private XoClientDatabase mDatabase;
    private ActionMode mCurrentActionMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDatabase = XoApplication.getXoClient().getDatabase();

        setHasOptionsMenu(true);
        mAttachmentAdapter = new AttachmentListAdapter(null, mContentMediaTypeFilter);

        mSearchContactsAdapter = new ContactSearchResultAdapter((XoActivity) getActivity());
        mSearchContactsAdapter.onCreate();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_audio_attachment_list, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        mSearchContactsAdapter.requestReload();

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
            setListAdapter(mResultsAdapter);
        } else {
            setListAdapter(mAttachmentAdapter);
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
        getActivity().unbindService(mConnection);
    }

    @Override
    public void onDestroy() {
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case SELECT_COLLECTION_REQUEST:
                    Integer mediaCollectionId = data.getIntExtra(MediaCollectionSelectionListFragment.MEDIA_COLLECTION_ID_EXTRA, -1);
                    if (mediaCollectionId > -1) {
                        retrieveCollectionAndAddSelectedAttachments(mediaCollectionId);
                    }
                    break;
                case SELECT_CONTACT_REQUEST:
                    List<Integer> contactSelections = data.getIntegerArrayListExtra(ContactSelectionActivity.EXTRA_SELECTED_CONTACT_IDS);
                    // send attachment to all selected contacts
                    for (Integer contactId : contactSelections) {
                        try {
                            TalkClientContact contact = mDatabase.findClientContactById(contactId);
                            ContactOperations.sendTransfersToContact(mAttachmentAdapter.getSelectedItems(), contact);
                        } catch (SQLException e) {
                            LOG.error(e.getMessage(), e);
                        } catch (FileNotFoundException e) {
                            LOG.error(e.getMessage(), e);
                        } catch (URISyntaxException e) {
                            LOG.error(e.getMessage(), e);
                        }
                    }
                    break;
            }
        }
        mCurrentActionMode.finish();
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

            mSearchAttachmentAdapter.query(query);
            if (mSearchAttachmentAdapter.getCount() > 0) {
                mResultsAdapter.addSection(getString(R.string.search_section_caption_audio_files),
                        mSearchAttachmentAdapter);
            }
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
                mResultsAdapter = new SectionedListAdapter();
            }

            setListAdapter(mResultsAdapter);

            mSearchAttachmentAdapter = new AttachmentSearchResultAdapter(mAttachmentAdapter.getItems());

        } else {
            mInSearchMode = false;
            mResultsAdapter = null;
            mSearchAttachmentAdapter = null;
        }
    }

    private void retrieveCollectionAndAddSelectedAttachments(Integer mediaCollectionId) {
        List<XoTransfer> selectedItems = mAttachmentAdapter.getSelectedItems();
        if(selectedItems.size() > 0) {
            try {
                TalkClientMediaCollection mediaCollection = mDatabase.findMediaCollectionById(mediaCollectionId);
                List<String> addedFilenames = new ArrayList<String>();
                for (XoTransfer item : selectedItems) {
                    mediaCollection.addItem(item);
                    addedFilenames.add(item.getFileName());
                }
                Toast.makeText(getActivity(), String.format(getString(R.string.added_attachment_to_collection), addedFilenames, mediaCollection.getName()), Toast.LENGTH_LONG).show();
            } catch (SQLException e) {
                LOG.error("Could not find MediaCollection with id: " + String.valueOf(mediaCollectionId), e);
            }
        }
    }

    private class ListInteractionHandler implements AdapterView.OnItemClickListener, AbsListView.MultiChoiceModeListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Object selectedItem = getListAdapter().getItem(position);

            if (selectedItem instanceof XoTransfer) {
                XoTransfer transfer = (XoTransfer)selectedItem;

                MediaPlaylist playlist = mInSearchMode ?
                        new SingleItemPlaylist(mDatabase, transfer) :
                        new UserPlaylist(mDatabase, mAttachmentAdapter.getContact());

                mMediaPlayerService.playItemInPlaylist(transfer, playlist);

                getActivity().startActivity(new Intent(getActivity(), FullscreenPlayerActivity.class));
            } else if (selectedItem instanceof TalkClientContact) {
                toggleSearchMode(false);
                mSearchMenuItem.collapseActionView();
                mAttachmentAdapter.setContact((TalkClientContact) selectedItem);
                setListAdapter(mAttachmentAdapter);
            }
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            if(checked) {
                mAttachmentAdapter.selectItem((int) id);
            } else {
                mAttachmentAdapter.deselectItem((int) id);
            }
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
        public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_delete_attachment:
                    XoDialogs.showYesNoDialog("RemoveAttachment", R.string.dialog_attachment_delete_title, R.string.dialog_attachment_delete_message, getActivity(),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    deleteSelectedAttachments();
                                    mode.finish();
                                }
                            }, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            });
                    return true;
                case R.id.menu_share:
                    mCurrentActionMode = mode;
                    startActivityForResult(new Intent(getActivity(), ContactSelectionActivity.class), SELECT_CONTACT_REQUEST);
                    return true;
                case R.id.menu_add_to_collection:
                    mCurrentActionMode = mode;
                    startActivityForResult(new Intent(getActivity(), MediaCollectionSelectionActivity.class), SELECT_COLLECTION_REQUEST);
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mAttachmentAdapter.deselectAllItems();
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
                setListAdapter(mAttachmentAdapter);
            }

            return true;
        }
    }

    private void deleteSelectedAttachments() {
        List<XoTransfer> selectedObjects = mAttachmentAdapter.getSelectedItems();
        for(XoTransfer item : selectedObjects) {
            try {
                XoApplication.getXoClient().getDatabase().deleteTransferAndMessage(item);
            } catch (SQLException e) {
                LOG.error(e);
            }
        }
    }
}
