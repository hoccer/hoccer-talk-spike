package com.hoccer.xo.android.fragment;

import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.ListAdapter;
import android.widget.SearchView;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

/**
 * Created by nico on 22/07/2014.
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
            if (mIsSearchModeEnabled) {
                searchInAdapter(query);
            }

            return false;
        }

        @Override
        public boolean onMenuItemActionExpand(MenuItem item) {
            if (item.getItemId() == R.id.menu_search) {
                mIsSearchModeEnabled = true;
                mCachedListAdapter = getListAdapter();
                onSearchModeEnabled();
                SearchView searchView = (SearchView) item.getActionView();
                SearchableListFragment.this.setListAdapter(searchInAdapter(searchView.getQuery().toString()));
            }

            return true;
        }

        @Override
        public boolean onMenuItemActionCollapse(MenuItem item) {
            if (item.getItemId() == R.id.menu_search) {
                mIsSearchModeEnabled = false;
                setListAdapter(mCachedListAdapter);
                onSearchModeDisabled();
            }

            return true;
        }
    }
}
