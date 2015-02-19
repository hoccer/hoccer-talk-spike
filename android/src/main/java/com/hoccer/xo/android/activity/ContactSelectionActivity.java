package com.hoccer.xo.android.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.content.SelectedContent;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.activity.component.ActivityComponent;
import com.hoccer.xo.android.activity.component.MediaPlayerActivityComponent;
import com.hoccer.xo.android.content.selector.AudioSelector;
import com.hoccer.xo.android.content.selector.IContentSelector;
import com.hoccer.xo.android.content.selector.ImageSelector;
import com.hoccer.xo.android.content.selector.VideoSelector;
import com.hoccer.xo.android.fragment.ContactSelectionFragment;
import com.hoccer.xo.android.util.ContactOperations;
import org.apache.log4j.Logger;

import java.util.ArrayList;

public class ContactSelectionActivity extends ComposableActivity implements ContactSelectionFragment.IContactSelectionListener {

    public static final String EXTRA_SELECTED_CONTACT_IDS = "com.hoccer.xo.android.extra.SELECTED_CONTACT_IDS";
    private static final Logger LOG = Logger.getLogger(ContactSelectionActivity.class);

    private ContactSelectionFragment mContactSelectionFragment;
    private Menu mMenu;

    @Override
    protected ActivityComponent[] createComponents() {
        return new ActivityComponent[]{new MediaPlayerActivityComponent(this)};
    }

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
        mContactSelectionFragment.addContactSelectionListener(this);
        android.support.v4.app.FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fl_fragment_container, mContactSelectionFragment);
        ft.commit();
    }

    @Override
    public void onContactSelectionChanged() {
        if (mContactSelectionFragment.getListView().getCheckedItemCount() == 0) {
            mMenu.findItem(R.id.menu_collections_ok).setVisible(false);
        } else {
            mMenu.findItem(R.id.menu_collections_ok).setVisible(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_collections_ok:
                createResultAndFinish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void createResultAndFinish() {
        Intent intent = new Intent();
        intent.putIntegerArrayListExtra(EXTRA_SELECTED_CONTACT_IDS,
                getSelectedContactIdsFromFragment());
        setResult(RESULT_OK, intent);

        finish();
    }

    private ArrayList<Integer> getSelectedContactIdsFromFragment() {
        ArrayList<Integer> selectedContactIds = new ArrayList<Integer>();
        SparseBooleanArray checkedItems = mContactSelectionFragment.getListView().getCheckedItemPositions();
        for (int i = 0; i < checkedItems.size(); i++) {
            int pos = checkedItems.keyAt(i);
            if (checkedItems.get(pos)) {
                TalkClientContact contact = (TalkClientContact) mContactSelectionFragment.getListView().getAdapter().getItem(pos);
                selectedContactIds.add(contact.getClientContactId());
            }
        }

        return selectedContactIds;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        mMenu = menu;
        menu.findItem(R.id.menu_my_profile).setVisible(false);
        menu.findItem(R.id.menu_settings).setVisible(false);

        return true;
    }
}
