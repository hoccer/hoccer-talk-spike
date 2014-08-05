package com.hoccer.xo.android.activity;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SearchView;
import com.hoccer.xo.android.adapter.AddressBookDialogAdapter;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.release.R;

public class AddressBookActivity extends XoActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private AddressBookDialogAdapter mAdapter;
    private LoaderManager mManager;
//    private String mAddressBookFilter;
    private String mToken;
    private Button mInviteButton;
//    private SearchView mSearchView;
    private ListView mListView;
    private boolean mIsSmsInvitation;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_multi_invitation;
    }

    @Override
    protected int getMenuResource() {
        return -1;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_invitation);
        mIsSmsInvitation = getIntent().getBooleanExtra("SMS", true);
        mToken = getIntent().getStringExtra("TOKEN");
        mAdapter = new AddressBookDialogAdapter(this, mIsSmsInvitation);
        mManager = getLoaderManager();
        if (mManager.getLoader(AddressBookDialogAdapter.ContactsQuery.QUERY_ID) != null) {
            mManager.restartLoader(AddressBookDialogAdapter.ContactsQuery.QUERY_ID, null, this);
        } else {
            mManager.initLoader(AddressBookDialogAdapter.ContactsQuery.QUERY_ID, null, this);
        }
        mListView = (ListView) findViewById(R.id.lv_address_book_dialog);
        mListView.setAdapter(mAdapter);
        mInviteButton = (Button) findViewById(R.id.bt_invite);
        mInviteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String recipients = mAdapter.getRecipients();
                if (!recipients.isEmpty()) {
                    if (mIsSmsInvitation) {
                        composeInviteSms(mToken, recipients);
                    } else {
                        composeInviteEmail(mToken, recipients);
                    }
                }
            }
        });
//        mSearchView = (SearchView) findViewById(R.id.tv_search);
//        mSearchView.setIconifiedByDefault(false);
//        initSearchView();
    }

//    private void initSearchView() {
//        final SearchManager searchManager = (SearchManager) mXoActivity.getSystemService(Context.SEARCH_SERVICE);
//        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(mXoActivity.getComponentName()));
//        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
//            @Override
//            public boolean onQueryTextSubmit(String queryText) {
//                return true;
//            }
//
//            @Override
//            public boolean onQueryTextChange(String newText) {
//                String newFilter = !TextUtils.isEmpty(newText) ? newText : null;
//                if (AddressBookDialogAdapter.mSearchTerm == null && newFilter == null) {
//                    return true;
//                }
//                if (AddressBookDialogAdapter.mSearchTerm != null && AddressBookDialogAdapter.mSearchTerm.equals(newFilter)) {
//                    return true;
//                }
//                AddressBookDialogAdapter.mSearchTerm = newFilter;
//                mManager.restartLoader(AddressBookDialogAdapter.ContactsQuery.QUERY_ID, null, AddressBookDialog.this);
//                return true;
//            }
//        });
//        if (AddressBookDialogAdapter.mSearchTerm != null) {
//            final String savedSearchTerm = AddressBookDialogAdapter.mSearchTerm;
//            mSearchView.setQuery(savedSearchTerm, false);
//        }
//    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        if (id == AddressBookDialogAdapter.ContactsQuery.QUERY_ID) {
            String[] params = null;
            Uri contentUri;
            String selection;
            contentUri = AddressBookDialogAdapter.ContactsQuery.CONTENT_URI;
            if (AddressBookDialogAdapter.mSearchTerm != null) {
                if (mIsSmsInvitation) {
                    selection = AddressBookDialogAdapter.ContactsQuery.SELECTION_WITH_PHONES_FILTERED;
                } else {
                    selection = AddressBookDialogAdapter.ContactsQuery.SELECTION_WITH_EMAILS_FILTERED;
                }
                params = new String[] {"%"+AddressBookDialogAdapter.mSearchTerm+"%"};
            } else {
                if (mIsSmsInvitation) {
                    selection = AddressBookDialogAdapter.ContactsQuery.SELECTION_WITH_PHONES;
                } else {
                    selection = AddressBookDialogAdapter.ContactsQuery.SELECTION_WITH_EMAILS;
                }
            }
            return new CursorLoader(this,
                    contentUri,
                    AddressBookDialogAdapter.ContactsQuery.PROJECTION,
                    selection,
                    params,
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
