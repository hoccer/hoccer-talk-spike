package com.hoccer.xo.android.fragment;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CheckedTextView;
import com.artcom.hoccer.R;
import com.hoccer.xo.android.adapter.ContactSelectionAdapter;

import java.util.HashSet;
import java.util.Set;


public class ContactSelectionFragment extends ListFragment {

    private final Set<IContactSelectionListener> contactSelectionListeners = new HashSet<IContactSelectionListener>();

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
