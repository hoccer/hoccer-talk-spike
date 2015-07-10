package com.hoccer.xo.android.activity;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.model.TalkGroupMembership;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.adapter.ContactSelectionAdapter;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.fragment.ContactSelectionFragment;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.List;

public abstract class ContactSelectionActivity extends XoActivity implements ContactSelectionAdapter.IContactSelectionListener, ContactSelectionAdapter.Filter {

    private static final Logger LOG = Logger.getLogger(ContactSelectionActivity.class);

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
        return ((ContactSelectionAdapter) mContactSelectionFragment.getListAdapter()).getSelectedContacts();
    }

    @Override
    public void onContactSelectionChanged(int count) {
        if (count == 0) {
            mMenu.findItem(R.id.menu_contact_selection_ok).setVisible(false);
        } else {
            mMenu.findItem(R.id.menu_contact_selection_ok).setVisible(true);
        }
    }

    public boolean shouldShow(TalkClientContact contact) {
        boolean shouldShow = false;
        if (contact.isGroup()) {
            if (contact.isGroupInvolved() && contact.isGroupExisting() && groupHasOtherContacts(contact.getGroupId())) {
                shouldShow = true;
            }
        } else if (contact.isClient()) {
            if (contact.isClientFriend() || contact.isInEnvironment() || (contact.isClientRelated() && contact.getClientRelationship().isBlocked())) {
                shouldShow = true;
            }
        }

        return shouldShow;
    }

    private static boolean groupHasOtherContacts(String groupId) {
        try {
            return XoApplication.get().getXoClient().getDatabase().findMembershipsInGroupByState(groupId, TalkGroupMembership.STATE_JOINED).size() > 1;
        } catch (SQLException e) {
            LOG.error(e);
        }
        return false;
    }
}
