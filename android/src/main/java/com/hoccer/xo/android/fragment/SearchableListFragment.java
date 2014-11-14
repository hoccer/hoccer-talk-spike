package com.hoccer.xo.android.fragment;

import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ListFragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.SearchView;
import com.artcom.hoccer.R;
import org.apache.log4j.Logger;

/**
 * Base class providing list search support.
 */
public abstract class SearchableListFragment extends ListFragment {

    private static final Logger LOG = Logger.getLogger(SearchableListFragment.class);

    private boolean mIsSearchModeEnabled = false;
    private ListAdapter mCachedListAdapter;
    private MenuItem mSearchMenuItem;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mIsSearchModeEnabled && mCachedListAdapter != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setListAdapter(mCachedListAdapter);
                }
            });
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        leaveSearchMode();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_searchable_list, menu);

        SearchActionHandler handler = new SearchActionHandler();
        mSearchMenuItem = menu.findItem(R.id.menu_search);
        mSearchMenuItem.setOnActionExpandListener(handler);

        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) mSearchMenuItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
        searchView.setIconifiedByDefault(false);
        searchView.setOnQueryTextListener(handler);
    }

    protected boolean isSearchModeEnabled() {
        return mIsSearchModeEnabled;
    }

    protected ListAdapter getCachedListAdapter() {
        return mCachedListAdapter;
    }

    protected abstract ListAdapter searchInAdapter(String query);

    protected abstract void onSearchModeEnabled();

    protected abstract void onSearchModeDisabled();

    public void leaveSearchMode() {
        if(mSearchMenuItem != null) {
            mSearchMenuItem.collapseActionView();
        }
    }

    private class SearchActionHandler implements SearchView.OnQueryTextListener, MenuItem.OnActionExpandListener {

        private IBinder mWindowToken;

        @Override
        public boolean onQueryTextSubmit(String query) {
            if (mWindowToken != null) {
                InputMethodManager inputManager = (InputMethodManager) getActivity()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(mWindowToken, InputMethodManager.HIDE_IMPLICIT_ONLY);
            }

            return true;
        }

        @Override
        public boolean onQueryTextChange(final String query) {
            // check if expanded to determine legality for search-mode
            if (mSearchMenuItem.isActionViewExpanded()) {
                searchInAdapter(query);
            } else {
                leaveSearchMode();
            }

            return false;
        }

        @Override
        public boolean onMenuItemActionExpand(MenuItem item) {
            if (item.getItemId() == R.id.menu_search) {
                // set the correct icons since android changes them for some reason
                getActivity().getActionBar().setIcon(R.drawable.ic_launcher_plain);

                int searchImgId = getResources().getIdentifier("android:id/search_mag_icon", null, null);
                ImageView searchIcon = ((ImageView) mSearchMenuItem.getActionView().findViewById(searchImgId));
                if (searchIcon != null) {
                    searchIcon.setImageResource(R.drawable.ic_action_search);
                }

                mIsSearchModeEnabled = true;
                mCachedListAdapter = getListAdapter();
                onSearchModeEnabled();
                SearchView searchView = (SearchView) item.getActionView();
                mWindowToken = searchView.getWindowToken();
                SearchableListFragment.this.setListAdapter(searchInAdapter(searchView.getQuery().toString()));
            }

            return true;
        }

        @Override
        public boolean onMenuItemActionCollapse(MenuItem item) {
            if (item.getItemId() == R.id.menu_search) {
                mIsSearchModeEnabled = false;
                if (mCachedListAdapter != null) {
                    setListAdapter(mCachedListAdapter);
                }

                onSearchModeDisabled();
            }

            return true;
        }
    }
}
