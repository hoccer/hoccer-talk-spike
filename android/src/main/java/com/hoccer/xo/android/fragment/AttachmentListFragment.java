package com.hoccer.xo.android.fragment;

import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.view.*;
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
import com.hoccer.xo.android.util.IntentHelper;
import com.artcom.hoccer.R;
import org.apache.log4j.Logger;

import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AttachmentListFragment extends SearchableListFragment {

    public static final String ARG_CLIENT_CONTACT_ID = "com.hoccer.xo.android.fragment.ARG_CLIENT_CONTACT_ID";
    public static final String ARG_CONTENT_MEDIA_TYPE = "com.hoccer.xo.android.fragment.ARG_CONTENT_MEDIA_TYPE";

    public static final int SELECT_COLLECTION_REQUEST = 1;
    public static final int SELECT_CONTACT_REQUEST = 2;

    private MediaPlayerService mMediaPlayerService;

    private final static Logger LOG = Logger.getLogger(AttachmentListFragment.class);

    private ServiceConnection mConnection;

    private AttachmentListAdapter mAttachmentAdapter;
    private SectionedListAdapter mResultsAdapter;
    private ContactSearchResultAdapter mSearchContactsAdapter;
    private AttachmentSearchResultAdapter mSearchAttachmentAdapter;
    private TalkClientContact mFilterContact = null;
    private XoClientDatabase mDatabase;
    private ActionMode mCurrentActionMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDatabase = XoApplication.getXoClient().getDatabase();

        setHasOptionsMenu(true);

        if (getActivity().getIntent().hasExtra(IntentHelper.EXTRA_CONTACT_ID)) {
            int contactId = getActivity().getIntent().getIntExtra(IntentHelper.EXTRA_CONTACT_ID, -1);
            if (contactId >= 0) {
                try {
                    mFilterContact = mDatabase.findContactById(contactId);
                } catch (SQLException e) {
                    LOG.warn("Contact with ID " + contactId + " not found");
                }
            }
        }

        mAttachmentAdapter = new AttachmentListAdapter(mFilterContact, ContentMediaType.AUDIO);

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
        setListAdapter(mAttachmentAdapter);

        if (mFilterContact != null) {
            getActivity().getActionBar().setTitle(getResources().getString(R.string.content_audio_by_contact_caption,
                    mFilterContact.getNickname()));
        } else {
            getActivity().getActionBar().setTitle(R.string.content_audio_caption);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.findItem(R.id.menu_collections).setVisible(true);
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
                            TalkClientContact contact = mDatabase.findContactById(contactId);
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

        if(mCurrentActionMode != null) {
            mCurrentActionMode.finish();
        }
    }

    @Override
    protected ListAdapter searchInAdapter(String query) {
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

        return mResultsAdapter;
    }

    @Override
    protected void onSearchModeEnabled() {
        if (mResultsAdapter == null) {
            mResultsAdapter = new SectionedListAdapter();
        }

        mSearchAttachmentAdapter = new AttachmentSearchResultAdapter(mAttachmentAdapter.getItems());
    }

    @Override
    protected void onSearchModeDisabled() {
        // do nothing
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

    private class ListInteractionHandler implements AdapterView.OnItemClickListener, AbsListView.MultiChoiceModeListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Object selectedItem = getListAdapter().getItem(position);

            if (selectedItem instanceof XoTransfer) {
                XoTransfer transfer = (XoTransfer)selectedItem;

                MediaPlaylist playlist = isSearchModeEnabled() ?
                        new SingleItemPlaylist(mDatabase, transfer) :
                        new UserPlaylist(mDatabase, mAttachmentAdapter.getContact());

                mMediaPlayerService.playItemInPlaylist(transfer, playlist);

                getActivity().startActivity(new Intent(getActivity(), FullscreenPlayerActivity.class));
            } else if (selectedItem instanceof TalkClientContact) {
                leaveSearchMode();
                mFilterContact = (TalkClientContact) selectedItem;
                mAttachmentAdapter.setContact((TalkClientContact) selectedItem);
                final String newActionBarTitle = getResources().getString(R.string.content_audio_by_contact_caption,
                        mFilterContact.getNickname());
                getActivity().getActionBar().setTitle(newActionBarTitle);
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
            inflater.inflate(R.menu.context_menu_attachment_list, menu);

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
}
