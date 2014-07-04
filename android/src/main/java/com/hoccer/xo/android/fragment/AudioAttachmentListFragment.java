package com.hoccer.xo.android.fragment;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientMediaCollection;
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.adapter.AttachmentListAdapter;
import com.hoccer.xo.android.adapter.AttachmentListFilterAdapter;
import com.hoccer.xo.android.adapter.ContactsAdapter;
import com.hoccer.xo.android.adapter.SearchResultsAdapter;
import com.hoccer.xo.android.base.XoListFragment;
import com.hoccer.xo.android.content.AudioAttachmentItem;
import com.hoccer.xo.android.content.audio.MediaPlaylist;
import com.hoccer.xo.android.service.MediaPlayerService;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AudioAttachmentListFragment extends XoListFragment {

    public static final String ARG_CLIENT_CONTACT_ID = "com.hoccer.xo.android.fragment.ARG_CLIENT_CONTACT_ID";
    public static final String ARG_MEDIA_COLLECTION_ID = "com.hoccer.xo.android.fragment.ARG_MEDIA_COLLECTION_ID";
    public static final String ARG_CONTENT_MEDIA_TYPE = "com.hoccer.xo.android.fragment.ARG_CONTENT_MEDIA_TYPE";
    public static final String AUDIO_ATTACHMENT_REMOVED_ACTION = "com.hoccer.xo.android.fragment.AUDIO_ATTACHMENT_REMOVED_ACTION";
    public static final String TALK_CLIENT_MESSAGE_ID_EXTRA = "com.hoccer.xo.android.fragment.TALK_CLIENT_MESSAGE_ID_EXTRA";

    private enum DisplayMode { ALL_ATTACHMENTS, COLLECTION_ATTACHMENTS, AUDIO_ATTACHMENTS,}

    private MediaPlayerService mMediaPlayerService;

    public static final int ALL_CONTACTS_ID = -1;

    private final static Logger LOG = Logger.getLogger(AudioAttachmentListFragment.class);
    private ServiceConnection mConnection;
    private AttachmentListAdapter mAttachmentListAdapter;
    private SearchResultsAdapter mResultsAdapter;
    private ContactsAdapter mContactsAdapter;
    private int mFilteredContactId = 0;
    private int mMediaCollectionId = 0;
    private ActionMode mActionMode;
    private ActionMode.Callback mActionModeCallback;
    private DisplayMode mDisplayMode;
    private String mCurrentContentMediaType;
    private TalkClientMediaCollection mCurrentCollection;
    private boolean mInSearchMode = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        mFilteredContactId = ALL_CONTACTS_ID;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        return inflater.inflate(R.layout.fragment_audio_attachment_list, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        determineDisplayMode();

        mAttachmentListAdapter = new AttachmentListAdapter(getActivity());
        mAttachmentListAdapter.setContentMediaType(mCurrentContentMediaType);
        if (!mInSearchMode) {
            setListAdapter(mAttachmentListAdapter);
        }

        loadAttachments();

        XoApplication.getXoClient().registerTransferListener(mAttachmentListAdapter);

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

        if (mInSearchMode) {
            setListAdapter(mResultsAdapter);
        } else {
            setListAdapter(mAttachmentListAdapter);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        ActionBar ab = getActivity().getActionBar();
        inflater.inflate(R.menu.fragment_attachment_list, menu);

        switch (mDisplayMode) {
            case COLLECTION_ATTACHMENTS:
                if (mCurrentCollection != null) {
                    ab.setTitle(mCurrentCollection.getName());
                }
                ab.setDisplayShowTitleEnabled(true);
                break;
            case AUDIO_ATTACHMENTS:
            case ALL_ATTACHMENTS:
            default:
                ab.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
                ab.setDisplayShowTitleEnabled(false);
                AttachmentListFilterAdapter filterAdapter = new AttachmentListFilterAdapter(getXoActivity());
                ab.setListNavigationCallbacks(filterAdapter, new AttachmentListFilterHandler());
                ab.setSelectedNavigationItem(filterAdapter.getPosition(mFilteredContactId));
        }

        initSearchWidget(menu);
    }

    @Override
    public void onPause() {
        super.onPause();
        ActionBar ab = getActivity().getActionBar();
        ab.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        ab.setDisplayShowTitleEnabled(true);
    }

    @Override
    public void onStop() {
        super.onStop();
        XoApplication.getXoClient().unregisterTransferListener(mAttachmentListAdapter);
        getActivity().unbindService(mConnection);
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
        switch (mDisplayMode) {
            case COLLECTION_ATTACHMENTS:
                mAttachmentListAdapter.loadAttachmentsFromCollection(mMediaCollectionId);
                break;
            case AUDIO_ATTACHMENTS:
            case ALL_ATTACHMENTS:
                if (getArguments() != null) {
                    int clientContactId = getArguments().getInt(ARG_CLIENT_CONTACT_ID);
                    if (clientContactId > 0) {
                        mAttachmentListAdapter.loadAttachmentsFromContact(clientContactId, mCurrentContentMediaType);
                        break;
                    }
                }
            default:
                mAttachmentListAdapter.loadAttachmentList();
        }
    }

    private void deleteSelectedAttachments(SparseBooleanArray selectedItems) {
        int attachmentCount = getListView().getCount();

        // remove list items from back to front to avoid index invalidation
        for (int index = attachmentCount - 1; index >= 0; --index) {
            if (selectedItems.get(index)) {
                deleteAudioAttachment(index);
            }
        }
    }

    private void showConfirmDeleteDialog(final SparseBooleanArray selectedItems) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.attachment_confirm_delete_dialog_message);
        builder.setPositiveButton(R.string.common_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                deleteSelectedAttachments(selectedItems);
            }
        });
        builder.setNegativeButton(R.string.common_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void deleteAudioAttachment(int pos) {
        AudioAttachmentItem item = mAttachmentListAdapter.getItem(pos);

        if (isPlaying(item)) {
            if (mMediaPlayerService.getRepeatMode() == MediaPlaylist.RepeatMode.REPEAT_TITLE) {
                mMediaPlayerService.stop();
            } else {
                mMediaPlayerService.playNextByRepeatMode();
            }
        } else if (isPaused(item)) {
            mMediaPlayerService.stop();
        }

        if (deleteFile(item.getFilePath())) {
            try {
                int downloadId = ((TalkClientDownload) item.getContentObject()).getClientDownloadId();
                XoApplication.getXoClient().getDatabase().deleteTalkClientDownloadbyId(downloadId);

                int messageId = XoApplication.getXoClient().getDatabase().findMessageByDownloadId(downloadId).getClientMessageId();
                XoApplication.getXoClient().getDatabase().deleteMessageById(messageId);

                mAttachmentListAdapter.removeItem(pos);

                mMediaPlayerService.removeMedia(item);

                Intent intent = new Intent(AUDIO_ATTACHMENT_REMOVED_ACTION);
                intent.putExtra(TALK_CLIENT_MESSAGE_ID_EXTRA, messageId);
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
            } catch (SQLException e) {
                LOG.error("Error deleting message with client download id of " + ((TalkClientDownload) item.getContentObject()).getClientDownloadId());
                e.printStackTrace();
            }
        }
    }

    private boolean isPaused(AudioAttachmentItem item) {
        if (mMediaPlayerService != null && mMediaPlayerService.isPaused()) {
            if (item.equals(mMediaPlayerService.getCurrentMediaItem())) {
                return true;
            }
        }

        return false;
    }

    private boolean isPlaying(AudioAttachmentItem item) {
        if (mMediaPlayerService != null && !mMediaPlayerService.isStopped() && !mMediaPlayerService.isPaused()) {
            if (item.equals(mMediaPlayerService.getCurrentMediaItem())) {
                return true;
            }
        }

        return false;
    }

    private boolean deleteFile(String filePath) {
        String path = Uri.parse(filePath).getPath();
        File file = new File(path);

        return file.delete();
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

    private void initSearchWidget(Menu menu) {

        SearchActionHandler handler = new SearchActionHandler();

        MenuItem searchMenuItem = menu.findItem(R.id.menu_search);
        searchMenuItem.setOnActionExpandListener(handler);

        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) searchMenuItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
        searchView.setIconifiedByDefault(false);
        searchView.setOnQueryTextListener(handler);

    }

    private void searchAttachmentList(final String query) {
        if (mInSearchMode) {
            if (mResultsAdapter == null) {
                mResultsAdapter = new SearchResultsAdapter();
            }

            mResultsAdapter.clear();
            AttachmentListAdapter audioAttachments = new AttachmentListAdapter(getXoActivity());
            List<AudioAttachmentItem> items = mAttachmentListAdapter.getAttachmentItems();

            for (AudioAttachmentItem item : items) {
                String title = item.getMetaData().getTitle();
                String artist = item.getMetaData().getArtist();

                if ((title != null && title.toLowerCase().contains(query.toLowerCase())) ||
                        (artist != null && artist.toLowerCase().contains(query.toLowerCase()))) {
                    audioAttachments.addItem(item);
                }
            }

            if (mDisplayMode != DisplayMode.COLLECTION_ATTACHMENTS) {
                // TODO load contacts into a contacts adapter
            }

            if (audioAttachments.getCount() > 0) {
                mResultsAdapter.addSection("Audio Attachments", audioAttachments);
            }

            setListAdapter(mResultsAdapter);
        }
    }

    private void determineDisplayMode() {
        if (getArguments() != null) {
            mMediaCollectionId = getArguments().getInt(ARG_MEDIA_COLLECTION_ID);
            // TODO: change default value to something generic to support all media types
            mCurrentContentMediaType = getArguments().getString(ARG_CONTENT_MEDIA_TYPE, ContentMediaType.AUDIO);
        }

        if (mMediaCollectionId > 0) {
            mDisplayMode = DisplayMode.COLLECTION_ATTACHMENTS;
            try {
                mCurrentCollection = getXoDatabase().findMediaCollectionById(mMediaCollectionId);
            } catch (SQLException e) {
                // TODO display error message?
                LOG.error(e);
            }
        } else {
            if (mCurrentContentMediaType.equals(ContentMediaType.AUDIO)) {
                mDisplayMode = DisplayMode.AUDIO_ATTACHMENTS;
            } else {
                mDisplayMode = DisplayMode.ALL_ATTACHMENTS;
            }
        }
    }

    private class ListInteractionHandler implements AdapterView.OnItemClickListener, AbsListView.MultiChoiceModeListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Object selectedItem = getListAdapter().getItem(position);

            if (selectedItem instanceof AudioAttachmentItem) {
                AudioAttachmentItem selectedAudioItem = (AudioAttachmentItem) selectedItem;
                if (mInSearchMode) {
                    List<AudioAttachmentItem> itemList = new ArrayList<AudioAttachmentItem>();
                    itemList.add(selectedAudioItem);
                    position = 0;
                    mInSearchMode = false;
                    mMediaPlayerService.setMediaList(itemList);
                } else {
                    setMediaList();
                }

                if (isPlaying(selectedAudioItem)) {
                    mMediaPlayerService.updatePosition(position);
                } else if (isPaused(selectedAudioItem)) {
                    mMediaPlayerService.updatePosition(position);
                    mMediaPlayerService.play();
                } else {
                    mMediaPlayerService.play(position);
                }

                getXoActivity().showFullscreenPlayer();
            } else {

            }
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            mAttachmentListAdapter.setSelections(getListView().getCheckedItemPositions());
            mAttachmentListAdapter.notifyDataSetChanged();
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
            switch (item.getItemId()) {
                case R.id.menu_delete_attachment:
                        /*@Note Copy list in order to assure that the checked items are still available in the dialog.
                         *     The selected items might already have been reset by the list.*/
                    SparseBooleanArray selectedItems = getListView().getCheckedItemPositions();

                    SparseBooleanArray selectedItemsCopy = new SparseBooleanArray();
                    for (int i = 0; i < selectedItems.size(); ++i) {
                        selectedItemsCopy.append(selectedItems.keyAt(i), selectedItems.valueAt(i));
                    }
                    showConfirmDeleteDialog(selectedItemsCopy);

                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
        }

        private void setMediaList() {
            List<AudioAttachmentItem> itemList = new ArrayList<AudioAttachmentItem>();
            for (AudioAttachmentItem audioAttachmentItem : mAttachmentListAdapter.getAttachmentItems()) {
                itemList.add(audioAttachmentItem);
            }
            mMediaPlayerService.setMediaList(itemList);
        }
    }

    private class AttachmentListFilterHandler implements ActionBar.OnNavigationListener {

        @Override
        public boolean onNavigationItemSelected(int itemPosition, long itemId) {
            int selectedContactId = Integer.valueOf(new Long(itemId).intValue());
            if (mFilteredContactId != selectedContactId) {
                mFilteredContactId = selectedContactId;
                mAttachmentListAdapter.clear();
                mAttachmentListAdapter.loadAttachmentsFromContact(mFilteredContactId, mCurrentContentMediaType);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAttachmentListAdapter.notifyDataSetChanged();
                    }
                });
            }

            return true;
        }
    }

    private class SearchActionHandler implements SearchView.OnQueryTextListener, MenuItem.OnActionExpandListener{

        @Override
        public boolean onQueryTextSubmit(String query) {
            InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);

            return true;
        }

        @Override
        public boolean onQueryTextChange(final String query) {
            if(mInSearchMode) {
                searchAttachmentList(query);
            }

            return false;
        }

        @Override
        public boolean onMenuItemActionExpand(MenuItem item) {
            if (item.getItemId() == R.id.menu_search) {
                mInSearchMode = true;
                
                SearchView searchView = (SearchView) item.getActionView();
                if (searchView.getQuery().length() > 0) {
                    searchAttachmentList(searchView.getQuery().toString());
                }
            }

            return true;
        }

        @Override
        public boolean onMenuItemActionCollapse(MenuItem item) {
            if (item.getItemId() == R.id.menu_search) {
                mInSearchMode = false;
                mResultsAdapter.clear();
                Toast.makeText(getActivity(),"onMenuItemActionCollapse", Toast.LENGTH_LONG).show();
                setListAdapter(mAttachmentListAdapter);
            }

            return true;
        }
    }
}
