package com.hoccer.xo.android.fragment;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.SparseBooleanArray;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientMediaCollection;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.activity.ContactSelectionActivity;
import com.hoccer.xo.android.activity.MediaCollectionSelectionActivity;
import com.hoccer.xo.android.adapter.AttachmentListAdapter;
import com.hoccer.xo.android.adapter.AttachmentSearchResultAdapter;
import com.hoccer.xo.android.adapter.ContactSearchResultAdapter;
import com.hoccer.xo.android.adapter.SearchResultsAdapter;
import com.hoccer.xo.android.base.XoListFragment;
import com.hoccer.xo.android.content.AudioAttachmentItem;
import com.hoccer.xo.android.content.audio.MediaPlaylist;
import com.hoccer.xo.android.service.MediaPlayerService;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AudioAttachmentListFragment extends XoListFragment {

    public static final String ARG_CLIENT_CONTACT_ID = "com.hoccer.xo.android.fragment.ARG_CLIENT_CONTACT_ID";
    public static final String ARG_MEDIA_COLLECTION_ID = "com.hoccer.xo.android.fragment.ARG_MEDIA_COLLECTION_ID";
    public static final String ARG_CONTENT_MEDIA_TYPE = "com.hoccer.xo.android.fragment.ARG_CONTENT_MEDIA_TYPE";
    public static final String AUDIO_ATTACHMENT_REMOVED_ACTION = "com.hoccer.xo.android.fragment.AUDIO_ATTACHMENT_REMOVED_ACTION";
    public static final String TALK_CLIENT_MESSAGE_ID_EXTRA = "com.hoccer.xo.android.fragment.TALK_CLIENT_MESSAGE_ID_EXTRA";

    public static final int ALL_CONTACTS_ID = -1;
    public static final int SELECT_COLLECTION_REQUEST = 1;
    public static final int SELECT_CONTACT_REQUEST = 2;
    private SparseBooleanArray mSelectedItems;


    private enum DisplayMode {ALL_ATTACHMENTS, COLLECTION_ATTACHMENTS, AUDIO_ATTACHMENTS;}

    private MediaPlayerService mMediaPlayerService;

    private final static Logger LOG = Logger.getLogger(AudioAttachmentListFragment.class);

    private ServiceConnection mConnection;

    private AttachmentListAdapter mAttachmentListAdapter;
    private SearchResultsAdapter mResultsAdapter;
    private ContactSearchResultAdapter mSearchContactsAdapter;
    private AttachmentSearchResultAdapter mSearchAttachmentAdapter;
    private int mContactIdFilter = ALL_CONTACTS_ID;
    private int mMediaCollectionId = 0;
    private MenuItem mSearchMenuItem;
    private DisplayMode mDisplayMode;
    private String mContentMediaTypeFilter;
    private TalkClientMediaCollection mCurrentCollection;
    private boolean mInSearchMode = false;
    private boolean mRemoveFromCollection = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        determineDisplayMode();
        setHasOptionsMenu(true);
        mAttachmentListAdapter = new AttachmentListAdapter(getActivity());
        mAttachmentListAdapter.setContactIdFilter(mContactIdFilter);
        mAttachmentListAdapter.setContentMediaTypeFilter(mContentMediaTypeFilter);
        loadAttachments();

        XoApplication.getXoClient().getDatabase().registerDownloadListener(mAttachmentListAdapter);

        if (mDisplayMode != DisplayMode.COLLECTION_ATTACHMENTS) {
            mSearchContactsAdapter = new ContactSearchResultAdapter(getXoActivity());
            mSearchContactsAdapter.onCreate();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        return inflater.inflate(R.layout.fragment_audio_attachment_list, container, false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == getActivity().RESULT_OK) {
            switch (requestCode) {
                case SELECT_COLLECTION_REQUEST:
                    Integer mediaCollectionId = data.getIntExtra(MediaCollectionSelectionListFragment.MEDIA_COLLECTION_ID_EXTRA, -1);
                    if (mediaCollectionId > -1) {
                        addSelectedAttachmentsToCollection(mediaCollectionId);
                    }
                    break;
                case SELECT_CONTACT_REQUEST:
                    List<Integer> contactSelections = data.getIntegerArrayListExtra(ContactSelectionActivity.SELECTED_CONTACT_IDS_EXTRA);
                    sendSelectedAttachmentsToContacts(contactSelections);
                    break;
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (mDisplayMode != DisplayMode.COLLECTION_ATTACHMENTS) {
            mSearchContactsAdapter.requestReload();
        }

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

        if (mDisplayMode != DisplayMode.COLLECTION_ATTACHMENTS) {
            if (mSearchContactsAdapter == null) {
                mSearchContactsAdapter = new ContactSearchResultAdapter(getXoActivity());
                mSearchContactsAdapter.onCreate();
                mSearchContactsAdapter.requestReload();
            }

            mSearchContactsAdapter.onResume();
        }

        if (mSearchMenuItem != null && mSearchMenuItem.isActionViewExpanded()) {
            toggleSearchMode(true);
            setListAdapter(mResultsAdapter);
        } else {
            setListAdapter(mAttachmentListAdapter);
        }

        switch (mDisplayMode) {
            case COLLECTION_ATTACHMENTS:
                if (mCurrentCollection != null) {
                    getActivity().getActionBar().setTitle(mCurrentCollection.getName());
                }
                mRemoveFromCollection = true;
                break;
            case AUDIO_ATTACHMENTS:
                getActivity().getActionBar().setTitle(R.string.menu_music_viewer);
                break;
            case ALL_ATTACHMENTS:
        }

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_attachment_list, menu);
        if (mDisplayMode == DisplayMode.AUDIO_ATTACHMENTS) {
            menu.findItem(R.id.menu_collections).setVisible(true);
        }
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
        XoApplication.getXoClient().unregisterTransferListener(mAttachmentListAdapter);
        getActivity().unbindService(mConnection);
    }

    @Override
    public void onDestroy() {
        XoApplication.getXoClient().getDatabase().unregisterDownloadListener(mAttachmentListAdapter);
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

        switch (mDisplayMode) {
            case COLLECTION_ATTACHMENTS:
                mAttachmentListAdapter.loadAttachmentsFromCollection(mCurrentCollection);
                break;
            case AUDIO_ATTACHMENTS:
            case ALL_ATTACHMENTS:
            default:
                mAttachmentListAdapter.setContactIdFilter(mContactIdFilter);
                mAttachmentListAdapter.setContentMediaTypeFilter(mContentMediaTypeFilter);
                mAttachmentListAdapter.loadAttachments();
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

    private void showRemoveOptionsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setSingleChoiceItems(R.array.delete_options, 0, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    mRemoveFromCollection = true;
                } else {
                    mRemoveFromCollection = false;
                }
            }
        });
        builder.setPositiveButton(R.string.common_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (mRemoveFromCollection) {
                    removeSelectedItemsFromCollection();
                } else {
                    showConfirmDeleteDialog();
                }
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showConfirmDeleteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.attachment_confirm_delete_dialog_message);
        builder.setPositiveButton(R.string.common_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (mRemoveFromCollection) {
                    removeSelectedItemsFromCollection();
                } else {
                    deleteSelectedAttachments(mSelectedItems);
                }
            }
        });
        builder.setNegativeButton(R.string.common_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void removeSelectedItemsFromCollection() {
        for (int index = 0; index < mSelectedItems.size(); ++index) {
            int pos = mSelectedItems.keyAt(index);
            if (mSelectedItems.get(pos)) {
                TalkClientDownload item = (TalkClientDownload) mAttachmentListAdapter.getItem(pos).getContentObject();
                mCurrentCollection.removeItem(item);
                mAttachmentListAdapter.removeItem(pos);
            }
        }
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
                TalkClientDownload download = (TalkClientDownload) item.getContentObject();

                XoApplication.getXoClient().getDatabase().deleteTalkClientDownload(download);

                int messageId = XoApplication.getXoClient().getDatabase().findMessageByDownloadId(download.getClientDownloadId()).getClientMessageId();
                XoApplication.getXoClient().getDatabase().deleteMessageById(messageId);

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

            if (mDisplayMode != DisplayMode.COLLECTION_ATTACHMENTS) {
                mSearchContactsAdapter.searchForContactsByName(query);
                if (mSearchContactsAdapter.getCount() > 0) {
                    mResultsAdapter.addSection(getString(R.string.search_section_caption_contacts),
                            mSearchContactsAdapter);
                }
            }

            mSearchAttachmentAdapter.searchForAttachments(query);
            if (mSearchAttachmentAdapter.getCount() > 0) {
                mResultsAdapter.addSection(getString(R.string.search_section_caption_audio_files),
                        mSearchAttachmentAdapter);
            }

            setListAdapter(mResultsAdapter);
        }
    }

    private void determineDisplayMode() {
        if (getArguments() != null) {
            mMediaCollectionId = getArguments().getInt(ARG_MEDIA_COLLECTION_ID);
            // TODO: change default value to something generic to support all media types
            mContentMediaTypeFilter = getArguments().getString(ARG_CONTENT_MEDIA_TYPE, ContentMediaType.AUDIO);
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
            if (mContentMediaTypeFilter.equals(ContentMediaType.AUDIO)) {
                mDisplayMode = DisplayMode.AUDIO_ATTACHMENTS;
            } else {
                mDisplayMode = DisplayMode.ALL_ATTACHMENTS;
            }
        }
    }

    private void filterAttachmentsByContactId(int selectedContactId) {
        if (mContactIdFilter != selectedContactId) {
            mContactIdFilter = selectedContactId;
            mAttachmentListAdapter.clear();
            mAttachmentListAdapter.setContactIdFilter(mContactIdFilter);
            mAttachmentListAdapter.setContentMediaTypeFilter(mContentMediaTypeFilter);
            mAttachmentListAdapter.loadAttachments();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mAttachmentListAdapter.notifyDataSetChanged();
                }
            });
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

            if (mSearchAttachmentAdapter == null) {
                mSearchAttachmentAdapter = new AttachmentSearchResultAdapter(getActivity());
            }

            mSearchAttachmentAdapter.setAttachmentItems(mAttachmentListAdapter.getAttachmentList());

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

    private void startContactSelectionActivity() {
        startActivityForResult(new Intent(getActivity(), ContactSelectionActivity.class), SELECT_CONTACT_REQUEST);
    }

    private void startMediaCollectionSelectionActivity() {
        startActivityForResult(new Intent(getActivity(), MediaCollectionSelectionActivity.class), SELECT_COLLECTION_REQUEST);
    }

    private void addSelectedAttachmentsToCollection(Integer mediaCollectionId) {
        try {
            retrieveCollectionAndAddSelectedAttachments(mediaCollectionId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void retrieveCollectionAndAddSelectedAttachments(Integer mediaCollectionId) throws SQLException {
        TalkClientMediaCollection mediaCollection = getXoDatabase().findMediaCollectionById(mediaCollectionId);
        List<String> addedFilenames = new ArrayList<String>();
        for (TalkClientDownload download : getSelectedAttachments()) {
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

    private void sendSelectedAttachmentsToContacts(List<Integer> contactIds) {
        for (Integer contactId : contactIds) {
            sendSelectedAttachmentsToContact(retrieveContactById(contactId));
        }
    }

    private void sendSelectedAttachmentsToContact(TalkClientContact contact) {
        for (TalkClientDownload download : getSelectedAttachments()) {
            TalkClientUpload upload = null;
            try {
                upload = createTalkClientUpload(download);
                createMessageAndSendUploadToContact(contact, upload);
            } catch (FileNotFoundException e) {
                Toast.makeText(getActivity(), String.format(getString(R.string.error_could_not_find_file_for_sending), download.getFileName()), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void createMessageAndSendUploadToContact(TalkClientContact contact, TalkClientUpload upload) {
        String messageTag = XoApplication.getXoClient().composeClientMessage(contact, "", upload).getMessageTag();
        XoApplication.getXoClient().sendMessage(messageTag);
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

    private TalkClientUpload createTalkClientUpload(TalkClientDownload download) throws FileNotFoundException {
        File fileToUpload = new File(download.getDataFile());
        if (!fileToUpload.exists()) {
            LOG.error("Error creating file from TalkClientDownloadObject.");
            throw new FileNotFoundException(fileToUpload.getAbsolutePath() + " could not be found on file system.");
        }

        String fileName = download.getFileName();
        String url = fileToUpload.getAbsolutePath();
        String contentType = download.getContentType();
        String mediaType = download.getMediaType();
        double aspectRatio = download.getAspectRatio();
        int contentLength = (int) fileToUpload.length();
        String contentHmac = download.getContentHmac();

        TalkClientUpload upload = new TalkClientUpload();
        upload.initializeAsAttachment(
                fileName,
                url,
                url,
                contentType,
                mediaType,
                aspectRatio,
                contentLength,
                contentHmac);
        return upload;
    }

    private List<TalkClientDownload> getSelectedAttachments() {
        List<TalkClientDownload> downloads = new ArrayList<TalkClientDownload>();
        for (int index = 0; index < mSelectedItems.size(); ++index) {
            int pos = mSelectedItems.keyAt(index);
            if (mSelectedItems.get(pos)) {
                TalkClientDownload download = (TalkClientDownload) mAttachmentListAdapter.getItem(pos).getContentObject();
                downloads.add(download);
            }
        }
        return downloads;
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
            } else if (selectedItem instanceof TalkClientContact) {
                toggleSearchMode(false);
                mSearchMenuItem.collapseActionView();
                filterAttachmentsByContactId(((TalkClientContact) selectedItem).getClientContactId());
                setListAdapter(mAttachmentListAdapter);
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

            setSelectedItems(getListView().getCheckedItemPositions());

            switch (item.getItemId()) {
                case R.id.menu_delete_attachment:
                    if (mDisplayMode == DisplayMode.COLLECTION_ATTACHMENTS) {
                        showRemoveOptionsDialog();
                    } else {
                        showConfirmDeleteDialog();
                    }
                    mode.finish();
                    return true;
                case R.id.menu_share:
                    mode.finish();
                    startContactSelectionActivity();
                    return false;
                case R.id.menu_add_to_collection:
                    mode.finish();
                    startMediaCollectionSelectionActivity();
                    return false;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
        }

        private void setMediaList() {
            List<AudioAttachmentItem> itemList = new ArrayList<AudioAttachmentItem>();
            for (AudioAttachmentItem audioAttachmentItem : mAttachmentListAdapter.getAttachmentList()) {
                itemList.add(audioAttachmentItem);
            }
            mMediaPlayerService.setMediaList(itemList);
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

}
