package com.hoccer.xo.android.fragment;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.*;
import android.widget.AbsListView;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.adapter.ContactSelectionAdapter;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class ContactSelectionFragment extends ListFragment{

    private static final Logger LOG = Logger.getLogger(ContactSelectionFragment.class);

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
        mContactSelectionAdapter = new ContactSelectionAdapter(getActivity());
        mContactSelectionAdapter.addContactSelectionListener((ContactSelectionAdapter.IContactSelectionListener) getActivity());
        mContactSelectionAdapter.registerListeners();
        updateSelectedContacts();

        setListAdapter(mContactSelectionAdapter);

        setHasOptionsMenu(true);
    }

    private void updateSelectedContacts() {
        List<TalkClientContact> contacts = new ArrayList<TalkClientContact>();
        List<Integer> contactIds = getActivity().getIntent().getIntegerArrayListExtra(EXTRA_SELECTED_CONTACT_IDS);
        if (contactIds == null) {
            return;
        }

        for (Integer contactId : contactIds) {
            try {
                TalkClientContact contact = XoApplication.get().getXoClient().getDatabase().findContactById(contactId);
                contacts.add(contact);
            } catch (SQLException e) {
                LOG.error("SQL error", e);
            }
        }
        mContactSelectionAdapter.setSelectedContacts(contacts);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mContactSelectionAdapter.unregisterListeners();
        mContactSelectionAdapter.removeContactSelectionListener((ContactSelectionAdapter.IContactSelectionListener) getActivity());
    }
}
