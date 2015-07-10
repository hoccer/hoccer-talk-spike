package com.hoccer.xo.android.activity;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.adapter.ContactSelectionAdapter;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.fragment.ContactSelectionFragment;

import java.util.List;

public abstract class ContactSelectionActivity extends XoActivity implements ContactSelectionAdapter.IContactSelectionListener {

    private ContactSelectionFragment mContactSelectionFragment;
    protected Menu mMenu;

    @Override
    protected int getLayoutResource() {
        return R.layout.default_framelayout;
    }

    @Override
    protected int getMenuResource() {
        return R.menu.menu_activity_contact_selection;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        enableUpNavigation();
        showContactSelectionFragment();
    }

    private void showContactSelectionFragment() {
        mContactSelectionFragment = new ContactSelectionFragment();
        android.support.v4.app.FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fl_fragment_container, mContactSelectionFragment);
        ft.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.findItem(R.id.menu_my_profile).setVisible(false);
        menu.findItem(R.id.menu_settings).setVisible(false);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_contact_selection_ok:
                handleContactSelection(getSelectedContactsFromFragment());
        }
        return super.onOptionsItemSelected(item);
    }

    protected abstract void handleContactSelection(List<TalkClientContact> selectedContacts);

    private List<TalkClientContact> getSelectedContactsFromFragment() {
        return ((ContactSelectionAdapter)mContactSelectionFragment.getListAdapter()).getSelectedContacts();
    }

    @Override
    public void onContactSelectionChanged(int count) {
        if (count == 0) {
            mMenu.findItem(R.id.menu_contact_selection_ok).setVisible(false);
        } else {
            mMenu.findItem(R.id.menu_contact_selection_ok).setVisible(true);
        }
    }
}
