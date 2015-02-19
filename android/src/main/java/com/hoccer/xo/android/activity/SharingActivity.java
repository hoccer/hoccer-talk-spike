package com.hoccer.xo.android.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
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

public class SharingActivity extends ComposableActivity implements ContactSelectionFragment.IContactSelectionListener {

    public static final String EXTRA_SELECTED_CONTACT_IDS = "com.hoccer.xo.android.extra.SELECTED_CONTACT_IDS";
    private static final Logger LOG = Logger.getLogger(SharingActivity.class);

    private ContactSelectionFragment mContactSelectionFragment;
    private Menu mMenu;

    @Override
    protected ActivityComponent[] createComponents() {
        return new ActivityComponent[]{new MediaPlayerActivityComponent(this)}; //TODO
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

        TaskStackBuilder.create(this).addNextIntentWithParentStack(NavUtils.getParentActivityIntent(this));

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
        if (item.getItemId() == R.id.menu_collections_ok) {
            if (Intent.ACTION_SEND.equals(getIntent().getAction())) {
                handleShareIntent(getIntent());
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void handleShareIntent(Intent shareIntent) {
        Uri contentUri = shareIntent.getParcelableExtra(Intent.EXTRA_STREAM);
        SelectedContent content = getContent(contentUri, shareIntent.getType());
        final TalkClientUpload upload = new TalkClientUpload();
        upload.initializeAsAttachment(content);

        for (Integer contactId : getSelectedContactIdsFromFragment()) {
            try {
                TalkClientContact contact = XoApplication.getXoClient().getDatabase().findContactById(contactId);
                ContactOperations.sendTransferToContact(upload, contact);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }

        Intent intent = new Intent(this, ChatsActivity.class);
        startActivity(intent);
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

    private SelectedContent getContent(Uri contentUri, String type) {
        IContentSelector selector = determineContentSelectorForType(type);

        Intent intent = new Intent();
        intent.setData(contentUri);

        return selector.createObjectFromSelectionResult(this, intent);
    }

    private IContentSelector determineContentSelectorForType(String type) {
        IContentSelector selector = null;
        if (type.startsWith("image/")) {
            selector = new ImageSelector(this);
        } else if (type.startsWith("video/")) {
            selector = new VideoSelector(this);
        } else if (type.startsWith("audio/")) {
            selector = new AudioSelector(this);
        }

        return selector;
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
