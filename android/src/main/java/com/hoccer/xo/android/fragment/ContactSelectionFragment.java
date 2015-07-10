package com.hoccer.xo.android.fragment;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.*;
import android.widget.AbsListView;
import com.artcom.hoccer.R;
import com.hoccer.xo.android.adapter.ContactSelectionAdapter;


public class ContactSelectionFragment extends ListFragment implements ContactSelectionAdapter.IContactSelectionListener{

    public static final String EXTRA_SELECTED_CONTACT_IDS = "com.hoccer.xo.android.extra.SELECTED_CONTACT_IDS";

    private ContactSelectionAdapter mContactSelectionAdapter;
    private Menu mMenu;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_contact_selection_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupListView();
    }

    private void setupListView() {
        getListView().setItemsCanFocus(false);
        getListView().setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContactSelectionAdapter = new ContactSelectionAdapter();
        mContactSelectionAdapter.addContactSelectionListener(this);
        mContactSelectionAdapter.registerListeners();
        setListAdapter(mContactSelectionAdapter);

        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mContactSelectionAdapter.unregisterListeners();
        mContactSelectionAdapter.removeContactSelectionListener(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        mMenu = menu;
    }

    @Override
    public void onContactSelectionChanged() {
        if (mContactSelectionAdapter.getSelectedContacts().size() == 0) {
            mMenu.findItem(R.id.menu_contact_selection_ok).setVisible(false);
        } else {
            mMenu.findItem(R.id.menu_contact_selection_ok).setVisible(true);
        }
    }
}
