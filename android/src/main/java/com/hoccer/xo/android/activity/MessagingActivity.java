package com.hoccer.xo.android.activity;

import android.app.ActionBar;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.xo.android.activity.component.ActivityComponent;
import com.hoccer.xo.android.activity.component.MediaPlayerActivityComponent;
import com.hoccer.xo.android.base.IMessagingFragmentManager;
import com.hoccer.xo.android.base.IProfileFragmentManager;
import com.hoccer.xo.android.content.Clipboard;
import com.hoccer.xo.android.fragment.*;
import com.hoccer.xo.android.util.IntentHelper;
import com.hoccer.xo.android.view.chat.ChatMessageItem;
import com.hoccer.xo.release.R;


public class MessagingActivity extends ComposableActivity implements IMessagingFragmentManager, IProfileFragmentManager {

    public static final String EXTRA_NEARBY_ARCHIVE = "com.hoccer.xo.android.intent.extra.NEARBY_ARCHIVE";

    ActionBar mActionBar;

    Fragment mCurrentFragment;

    @Override
    protected ActivityComponent[] createComponents() {
        return new ActivityComponent[] { new MediaPlayerActivityComponent(this) };
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_messaging;
    }

    @Override
    protected int getMenuResource() {
        return -1;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get action bar (for setting title)
        mActionBar = getActionBar();

        // enable up navigation
        enableUpNavigation();

        // handle converse intent
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(IntentHelper.EXTRA_CONTACT_ID)) {
            int contactId = intent.getIntExtra(IntentHelper.EXTRA_CONTACT_ID, -1);
            if (contactId == -1) {
                LOG.error("invalid contact id");
            } else {
                showMessageFragment(contactId);
            }
        } else if (intent != null && intent.hasExtra(EXTRA_NEARBY_ARCHIVE)) {
            showNearbyArchiveFragment();
        } else {
            LOG.error("Neither contact ID nor nearby-archive specified");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void showPopupForMessageItem(final ChatMessageItem messageItem, View messageItemView) {
        PopupMenu popup = new PopupMenu(this, messageItemView);
        popup.getMenuInflater().inflate(R.menu.popup_menu_messaging, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                popupItemSelected(item, messageItem);
                return true;
            }
        });
        popup.show();
    }

    private void popupItemSelected(MenuItem item, ChatMessageItem messageItem) {
        switch (item.getItemId()) {
            case R.id.menu_copy_message:
                if (messageItem.getContent() != null) {
                    Clipboard.getInstance(this).storeAttachment(messageItem.getContent());
                } else {
                    putMessageTextInSystemClipboard(messageItem);
                }
                break;
            case R.id.menu_delete_message:
                getXoClient().deleteMessage(messageItem.getMessage());
                break;
        }
    }

    private void putMessageTextInSystemClipboard(ChatMessageItem messageItem) {
        ClipboardManager clipboardText = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("simple text", messageItem.getText());
        clipboardText.setPrimaryClip(clip);
    }

    public void setActionBarText(TalkClientContact contact) {
        String title;
        if (contact.isGroup() && contact.getGroupPresence().isTypeNearby()) {
            title = getResources().getString(R.string.nearby_text);
        } else {
            title = contact.getNickname();
        }
        mActionBar.setTitle(title);
    }

    @Override
    public void clipBoardItemSelected(IContentObject contentObject) {
        if (mCurrentFragment instanceof MessagingFragment) {
            ((MessagingFragment) mCurrentFragment).onAttachmentSelected(contentObject);
        }
    }

    @Override
    protected void applicationWillEnterBackground() {
        super.applicationWillEnterBackground();
        if (mCurrentFragment instanceof MessagingFragment) {
            ((MessagingFragment) mCurrentFragment).applicationWillEnterBackground();
        }
    }

    @Override
    public void showMessageFragment(int contactId) {
        mCurrentFragment = new MessagingFragment();

        Bundle bundle = new Bundle();
        bundle.putInt(MessagingFragment.ARG_CLIENT_CONTACT_ID, contactId);
        mCurrentFragment.setArguments(bundle);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fl_messaging_fragment_container, mCurrentFragment);
        fragmentTransaction.commit();
    }

    @Override
    public void showNearbyArchiveFragment() {
        mCurrentFragment = new NearbyArchiveFragment();

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fl_messaging_fragment_container, mCurrentFragment);
        fragmentTransaction.commit();
    }

    @Override
    public void showSingleProfileFragment(int clientContactId) {
        mCurrentFragment = new SingleProfileFragment();

        Bundle bundle = new Bundle();
        bundle.putInt(SingleProfileFragment.ARG_CLIENT_CONTACT_ID, clientContactId);
        mCurrentFragment.setArguments(bundle);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fl_messaging_fragment_container, mCurrentFragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    @Override
    public void showGroupProfileFragment(int groupContactId, boolean startInActionMode, boolean addToBackStack) {
        mCurrentFragment = new GroupProfileFragment();

        Bundle bundle = new Bundle();
        bundle.putInt(GroupProfileFragment.ARG_CLIENT_CONTACT_ID, groupContactId);
        if (startInActionMode) {
            bundle.putBoolean(GroupProfileFragment.ARG_START_IN_ACTION_MODE, true);
        } else {
            bundle.putBoolean(GroupProfileFragment.ARG_START_IN_ACTION_MODE, false);
        }
        mCurrentFragment.setArguments(bundle);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fl_messaging_fragment_container, mCurrentFragment);

        if (addToBackStack) {
            fragmentTransaction.addToBackStack(null);
        }
        fragmentTransaction.commit();
    }

    @Override
    public void showGroupProfileCreationFragment(int groupContactId, boolean cloneProfile) {
        mCurrentFragment = new GroupProfileCreationFragment();

        Bundle bundle = new Bundle();
        bundle.putInt(GroupProfileCreationFragment.ARG_CLIENT_CONTACT_ID, groupContactId);
        if (cloneProfile) {
            bundle.putBoolean(GroupProfileCreationFragment.ARG_CLONE_CURRENT_GROUP, true);
        }
        mCurrentFragment.setArguments(bundle);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fl_messaging_fragment_container, mCurrentFragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }
}
