package com.hoccer.xo.android.fragment;

import android.app.ListFragment;
import android.app.SearchManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.SearchView;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.adapter.DeviceContactsAdapter;
import com.hoccer.xo.android.util.ContactOperations;
import com.hoccer.xo.android.util.DeviceContact;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Shows device contacts via a DeviceContactsAdapter and manages search queries.
 */
public class DeviceContactsSelectionFragment extends ListFragment {

    private final static Logger LOG = Logger.getLogger(DeviceContactsSelectionFragment.class);

    public static final String EXTRA_IS_SMS_INVITATION = "com.hoccer.xo.android.extra.IS_SMS_INVITATION";
    public static final String EXTRA_TOKEN = "com.hoccer.xo.android.extra.TOKEN";

    private String mToken;
    private boolean mIsSmsInvitation;
    private DeviceContactsAdapter mAdapter;

    final static Uri CONTENT_URI = ContactsContract.Data.CONTENT_URI;

    final static String SELECTION_WITH_PHONES = ContactsContract.Data.MIMETYPE  + "='" + ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "'";
    final static String SELECTION_WITH_EMAILS = ContactsContract.Data.MIMETYPE  + "='" + ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE + "'";

    final static String[] PROJECTION = {
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Email.ADDRESS,
    };

    final static int LOOKUP_KEY_FIELD = 0;
    final static int DISPLAY_NAME_FIELD = 1;
    final static int THUMBNAIL_URI_FIELD = 2;
    final static int PHONE_NUMBER_FIELD = 3;
    final static int EMAIL_ADDRESS_FIELD = 4;

    private MenuItem mSearchMenuItem;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        mIsSmsInvitation = getActivity().getIntent().getBooleanExtra(EXTRA_IS_SMS_INVITATION, true);
        mToken = getActivity().getIntent().getStringExtra(EXTRA_TOKEN);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_device_contacts_selection, container, false);
        Button inviteButton = (Button) view.findViewById(R.id.bt_invite);
        inviteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String[] selectedContacts = mAdapter.getSelectedData();
                if (mIsSmsInvitation) {
                    composeInviteSms(selectedContacts);
                    getActivity().finish();
                } else {
                    composeInviteEmail(selectedContacts);
                    getActivity().finish();
                }
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();

        // query all phone numbers or email addresses and aggregate DeviceContacts
        List<DeviceContact> contacts = new ArrayList<DeviceContact>();
        Cursor cursor;
        if (mIsSmsInvitation) {
            cursor = getActivity().getContentResolver().query(CONTENT_URI, PROJECTION, SELECTION_WITH_PHONES, null, ContactsContract.Contacts.SORT_KEY_PRIMARY);
        } else {
            cursor = getActivity().getContentResolver().query(CONTENT_URI, PROJECTION, SELECTION_WITH_EMAILS, null, ContactsContract.Contacts.SORT_KEY_PRIMARY);
        }

        LOG.debug("Number of entries: " + cursor.getCount());

        // create a DeviceContact instance for every individual contact encountered keeping the order
        if(cursor.moveToFirst()) {
            DeviceContact currentContact = null;
            do {
                String lookupKey = cursor.getString(LOOKUP_KEY_FIELD);

                if (currentContact == null || !currentContact.getLookupKey().equals(lookupKey)) {
                    String displayName = cursor.getString(DISPLAY_NAME_FIELD);
                    String thumbnailUri = cursor.getString(THUMBNAIL_URI_FIELD);
                    currentContact = new DeviceContact(lookupKey, displayName);
                    currentContact.setThumbnailUri(thumbnailUri);
                    contacts.add(currentContact);
                }

                if (mIsSmsInvitation) {
                    String phoneNumber = cursor.getString(PHONE_NUMBER_FIELD);
                    currentContact.addPhoneNumber(phoneNumber);
                } else {
                    String eMailAddress = cursor.getString(EMAIL_ADDRESS_FIELD);
                    currentContact.addEMailAddress(eMailAddress);
                }
            } while (cursor.moveToNext());
        }

        DeviceContactsAdapter.DataType dataType = mIsSmsInvitation ? DeviceContactsAdapter.DataType.PhoneNumber : DeviceContactsAdapter.DataType.EMailAddress;
        mAdapter = new DeviceContactsAdapter(contacts, dataType, getActivity());
        setListAdapter(mAdapter);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_searchable_list, menu);
        setupSearchWidget(menu);
    }

    private void composeInviteSms(String[] phoneNumbers) {
        String urlScheme = XoApplication.getXoClient().getConfiguration().getUrlScheme();
        String selfName = XoApplication.getXoClient().getSelfContact().getName();
        String message = String.format(getString(R.string.sms_invitation_text), urlScheme, mToken, selfName);
        ContactOperations.sendSMS(getActivity(), message, phoneNumbers);
    }

    private void composeInviteEmail(String[] eMailAddresses) {
        String urlScheme = XoApplication.getXoClient().getConfiguration().getUrlScheme();
        String selfName = XoApplication.getXoClient().getSelfContact().getName();
        String subject = getString(R.string.email_invitation_subject);
        String message = String.format(getString(R.string.email_invitation_text), urlScheme, mToken, selfName);
        ContactOperations.sendEMail(getActivity(), subject, message, eMailAddresses);
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

    private void showSoftKeyboard() {
        InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }

    private class SearchActionHandler implements SearchView.OnQueryTextListener, MenuItem.OnActionExpandListener {

        @Override
        public boolean onQueryTextSubmit(String query) {
            showSoftKeyboard();

            return true;
        }

        @Override
        public boolean onQueryTextChange(final String query) {
            mAdapter.setQuery(query);
            return false;
        }

        @Override
        public boolean onMenuItemActionExpand(MenuItem item) {
            if (item.getItemId() == R.id.menu_search) {
            }
            return true;
        }

        @Override
        public boolean onMenuItemActionCollapse(MenuItem item) {
            if (item.getItemId() == R.id.menu_search) {
                mAdapter.setQuery("");
            }
            return true;
        }
    }
}
