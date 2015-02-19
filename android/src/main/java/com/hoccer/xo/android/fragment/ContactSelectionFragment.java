package com.hoccer.xo.android.fragment;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CheckedTextView;
import com.hoccer.xo.android.adapter.ContactSelectionAdapter;
import com.artcom.hoccer.R;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class ContactSelectionFragment extends ListFragment {

    public static final String EXTRA_SELECTED_CONTACT_IDS = "com.hoccer.xo.android.extra.SELECTED_CONTACT_IDS";

    private final static Logger LOG = Logger.getLogger(ContactSelectionFragment.class);

    private Set<IContactSelectionListener> contactSelectionListeners = new HashSet<IContactSelectionListener>();

    public interface IContactSelectionListener {
        public void onContactSelectionChanged();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_contact_selection_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupListView();
        setListAdapter(new ContactSelectionAdapter());
    }

    public void addContactSelectionListener(IContactSelectionListener l) {
        contactSelectionListeners.add(l);
    }

    public void removeContactSelectionListener(IContactSelectionListener l) {
        contactSelectionListeners.remove(l);
    }

    private void setupListView() {
        getListView().setItemsCanFocus(false);
        getListView().setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CheckedTextView checkedTextView = (CheckedTextView) view.findViewById(R.id.contact_name_checked);
                checkedTextView.setChecked(!checkedTextView.isChecked());
                for (IContactSelectionListener listener : contactSelectionListeners) {
                    listener.onContactSelectionChanged();
                }
            }
        });
    }
}
