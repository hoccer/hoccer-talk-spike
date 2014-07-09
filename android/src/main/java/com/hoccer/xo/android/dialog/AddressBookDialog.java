package com.hoccer.xo.android.dialog;

import android.app.Dialog;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
//import android.support.v4.app.LoaderManager;
//import android.support.v4.content.CursorLoader;
//import android.support.v4.content.Loader;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ListView;
import com.hoccer.xo.android.adapter.AddressBookDialogAdapter;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.release.R;

public class AddressBookDialog extends Dialog implements LoaderManager.LoaderCallbacks<Cursor> {

    private Context mContext;
    private AddressBookDialogAdapter mAdapter;
    private LoaderManager mManager;
    private String mAddressBookFilter;
    private String mToken;
    private Button mInviteButton;
    private Button mCancelButton;
    private ListView mListView;
    private XoActivity mXoActivity;

    public AddressBookDialog(Context context) {
        super(context);
        mContext = context;
    }

    public void setupDialog(XoActivity xoActivity, String token, String filter) {
        mXoActivity = xoActivity;
        mManager = mXoActivity.getLoaderManager();
        mAddressBookFilter = filter;
        mToken = token;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_multi_invitation);
        mAdapter = new AddressBookDialogAdapter(mContext);
        if (mManager.getLoader(AddressBookDialogAdapter.ContactsQuery.QUERY_ID) != null) {
            mManager.destroyLoader(AddressBookDialogAdapter.ContactsQuery.QUERY_ID);
        }
        mManager.initLoader(AddressBookDialogAdapter.ContactsQuery.QUERY_ID, null, this);
        mListView = (ListView) findViewById(R.id.lv_address_book_dialog);
        mListView.setAdapter(mAdapter);
        mCancelButton = (Button) findViewById(R.id.bt_cancel);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
        mInviteButton = (Button) findViewById(R.id.bt_invite);
        mInviteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String recipients = mAdapter.getRecipients();
                if (!recipients.isEmpty()) {
                    if (mAddressBookFilter.equals(ContactsContract.Contacts.HAS_PHONE_NUMBER)) {
                        mXoActivity.composeInviteSms(mToken, recipients);
                    } else {
                        mXoActivity.composeInviteEmail(mToken, recipients);
                    }
                    dismiss();
                }
            }
        });
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        if (id == AddressBookDialogAdapter.ContactsQuery.QUERY_ID) {
            Uri contentUri;
            contentUri = AddressBookDialogAdapter.ContactsQuery.CONTENT_URI;
            String selection = "";
            if (mAddressBookFilter.equals(ContactsContract.Contacts.HAS_PHONE_NUMBER)) {
                selection = AddressBookDialogAdapter.ContactsQuery.SELECTION_WITH_PHONES;
            } else {
                selection = AddressBookDialogAdapter.ContactsQuery.SELECTION_WITH_EMAILS;
            }
            return new CursorLoader(mContext,
                    contentUri,
                    AddressBookDialogAdapter.ContactsQuery.PROJECTION,
                    selection,
                    null,
                    AddressBookDialogAdapter.ContactsQuery.SORT_ORDER);
        }
        return  null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (cursorLoader.getId() == AddressBookDialogAdapter.ContactsQuery.QUERY_ID) {
            mAdapter.swapCursor(cursor);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        if (cursorLoader.getId() == AddressBookDialogAdapter.ContactsQuery.QUERY_ID) {
            mAdapter.swapCursor(null);
        }
    }
}
