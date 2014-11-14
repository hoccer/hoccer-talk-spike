package com.hoccer.xo.android.fragment;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.*;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.adapter.DeviceContactsAdapter;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.util.ContactOperations;
import com.hoccer.xo.android.util.DeviceContact;
import com.artcom.hoccer.R;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Shows device contacts via a DeviceContactsAdapter and manages search queries.
 */
public class DeviceContactsInvitationFragment extends SearchableListFragment {

    private final static Logger LOG = Logger.getLogger(DeviceContactsInvitationFragment.class);

    public static final String EXTRA_IS_SMS_INVITATION = "com.hoccer.xo.android.extra.IS_SMS_INVITATION";

    private boolean mIsSmsInvitation;
    private DeviceContactsAdapter mAdapter;
    private RelativeLayout mProgressOverlay;
    private boolean mIsInvitationCancelled;

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_device_contacts_selection, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mIsSmsInvitation = getActivity().getIntent().getBooleanExtra(EXTRA_IS_SMS_INVITATION, true);
        mProgressOverlay = (RelativeLayout) view.findViewById(R.id.rl_progress_overlay);

        final Button inviteButton = (Button) view.findViewById(R.id.bt_continue);
        inviteButton.setOnClickListener(new InviteButtonClickListener());

        mAdapter = createAdapter();
        setListAdapter(mAdapter);
    }

    private class InviteButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            showProgressOverlay(true);
            mIsInvitationCancelled = false;

            XoApplication.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        String token = XoApplication.getXoClient().generatePairingToken();

                        if (!mIsInvitationCancelled) {
                            if (token != null) {
                                composeInvitation(token);
                                getActivity().finish();
                            } else {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        showProgressOverlay(false);

                                        XoDialogs.showOkDialog(
                                                "MissingPairingToken",
                                                R.string.dialog_missing_pairing_token_title,
                                                R.string.dialog_missing_pairing_token_message,
                                                getActivity()
                                        );
                                    }
                                });
                            }
                        }
                    } catch (Throwable t) {
                        LOG.error("Error while inviting contacts", t);
                    }
                }
            });
        }
    }

    private DeviceContactsAdapter createAdapter() {
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

                String dataItem;
                if (mIsSmsInvitation) {
                    dataItem = cursor.getString(PHONE_NUMBER_FIELD);
                } else {
                    dataItem = cursor.getString(EMAIL_ADDRESS_FIELD);
                }
                currentContact.addDataItem(dataItem);
            } while (cursor.moveToNext());
        }

        return new DeviceContactsAdapter(contacts, getActivity());
    }

    private void showProgressOverlay(boolean visible) {
        mProgressOverlay.setVisibility(visible ? View.VISIBLE : View.GONE);
        enableOptionsMenu(!visible);
    }

    private void enableOptionsMenu(boolean enabled) {
        // toggle fragment options menu
        setMenuVisibility(enabled);

        // toggle activity options menu
        if (getActivity() instanceof XoActivity) {
            ((XoActivity)getActivity()).setOptionsMenuEnabled(enabled);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        showProgressOverlay(false);
    }

    @Override
    public void onPause() {
        super.onPause();
        mIsInvitationCancelled = true;
    }

    @Override
    protected ListAdapter searchInAdapter(String query) {
        mAdapter.setQuery(query);
        return mAdapter;
    }

    @Override
    protected void onSearchModeEnabled() {
        // do nothing
    }

    @Override
    protected void onSearchModeDisabled() {
        mAdapter.setQuery(null);
    }

    private void composeInvitation(String token) {
        String[] selectedContacts = mAdapter.getSelectedData();

        if (mIsSmsInvitation) {
            composeInviteSms(selectedContacts, token);
        } else {
            composeInviteEmail(selectedContacts, token);
        }
    }

    private void composeInviteSms(String[] phoneNumbers, String token) {
        String invitationServerUri = XoApplication.getConfiguration().getInvitationServerUri();
        String selfName = XoApplication.getXoClient().getSelfContact().getName();
        String message = String.format(getString(R.string.sms_invitation_text), invitationServerUri, token, selfName);
        ContactOperations.sendSMS(getActivity(), message, phoneNumbers);
    }

    private void composeInviteEmail(String[] eMailAddresses, String token) {
        String invitationServerUri = XoApplication.getConfiguration().getInvitationServerUri();
        String selfName = XoApplication.getXoClient().getSelfContact().getName();
        String subject = getString(R.string.email_invitation_subject);
        String message = String.format(getString(R.string.email_invitation_text), invitationServerUri, token, selfName);
        ContactOperations.sendEMail(getActivity(), subject, message, eMailAddresses);
    }
}
