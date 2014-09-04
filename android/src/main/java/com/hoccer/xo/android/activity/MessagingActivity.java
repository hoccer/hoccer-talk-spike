package com.hoccer.xo.android.activity;

import android.app.ActionBar;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.xo.android.base.IMessagingFragmentManager;
import com.hoccer.xo.android.base.IProfileFragmentManager;
import com.hoccer.xo.android.base.XoActionbarActivity;
import com.hoccer.xo.android.content.Clipboard;
import com.hoccer.xo.android.fragment.*;
import com.hoccer.xo.android.util.IntentHelper;
import com.hoccer.xo.android.view.chat.ChatMessageItem;
import com.hoccer.xo.release.R;


public class MessagingActivity extends XoActionbarActivity implements IMessagingFragmentManager, IProfileFragmentManager {

    public static final String EXTRA_NEARBY_ARCHIVE = "com.hoccer.xo.android.intent.extra.NEARBY_ARCHIVE";

    ActionBar mActionBar;

    MessagingFragment mMessagingFragment;

    private IContentObject mClipboardAttachment;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_messaging;
    }

    @Override
    protected int getMenuResource() {
        return R.menu.fragment_messaging;
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
    public void showPopupForMessageItem(ChatMessageItem messageItem, View messageItemView) {
        IContentObject contentObject = messageItem.getContent();
        final int messageId = messageItem.getMessage().getClientMessageId();
        final String messageText = messageItem.getText();
        PopupMenu popup = new PopupMenu(this, messageItemView);
        if (contentObject != null) {
            if (contentObject.isContentAvailable()) {
                mClipboardAttachment = contentObject;
            }
        }
        popup.getMenuInflater().inflate(R.menu.popup_menu_messaging, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                popupItemSelected(item, messageId, messageText);
                return true;
            }
        });
        popup.show();
    }

    public void popupItemSelected(MenuItem item, int messageId, String text) {
        switch (item.getItemId()) {
            case R.id.menu_copy_message:
                if (mClipboardAttachment != null) {
                    Clipboard clipboard = Clipboard.get(this);
                    clipboard.storeAttachment(mClipboardAttachment);
                    mClipboardAttachment = null;
                } else {
                    ClipboardManager clipboardText = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("simple text", text);
                    clipboardText.setPrimaryClip(clip);
                }
                break;
            case R.id.menu_delete_message:
                    getXoClient().deleteMessage(messageId);
                break;
            }
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
        if (mMessagingFragment != null) {
            mMessagingFragment.onAttachmentSelected(contentObject);
        }
    }

    @Override
    protected void applicationWillEnterBackground() {
        super.applicationWillEnterBackground();
        if (mMessagingFragment != null) {
            mMessagingFragment.applicationWillEnterBackground();
        }
    }

    @Override
    public void showMessageFragment(int contactId) {
        Bundle bundle = new Bundle();
        bundle.putInt(MessagingFragment.ARG_CLIENT_CONTACT_ID, contactId);

        mMessagingFragment = new MessagingFragment();
        mMessagingFragment.setArguments(bundle);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fl_messaging_fragment_container, mMessagingFragment);
        fragmentTransaction.commit();
    }

    @Override
    public void showNearbyArchiveFragment() {
        NearbyArchiveFragment fragment = new NearbyArchiveFragment();

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fl_messaging_fragment_container, fragment);
        fragmentTransaction.commit();
    }

    @Override
    public void showSingleProfileFragment(int clientContactId) {
        Bundle bundle = new Bundle();
        bundle.putInt(SingleProfileFragment.ARG_CLIENT_CONTACT_ID, clientContactId);

        SingleProfileFragment singleProfileFragment = new SingleProfileFragment();
        singleProfileFragment.setArguments(bundle);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fl_messaging_fragment_container, singleProfileFragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    @Override
    public void showGroupProfileFragment(int groupContactId, boolean isFollowUp) {
        Bundle bundle = new Bundle();
        bundle.putInt(GroupProfileFragment.ARG_CLIENT_CONTACT_ID, groupContactId);

        GroupProfileFragment groupProfileFragment = new GroupProfileFragment();
        groupProfileFragment.setArguments(bundle);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fl_messaging_fragment_container, groupProfileFragment);

        if (!isFollowUp) {
            fragmentTransaction.addToBackStack(null);
        }
        fragmentTransaction.commit();
    }

    @Override
    public void showGroupProfileCreationFragment(int groupContactId, boolean cloneProfile) {
        Bundle bundle = new Bundle();
        bundle.putInt(GroupProfileCreationFragment.ARG_CLIENT_CONTACT_ID, groupContactId);

        if (cloneProfile) {
            bundle.putBoolean(GroupProfileCreationFragment.ARG_CLONE_CURRENT_GROUP, true);
        }

        GroupProfileCreationFragment groupProfileCreationFragment = new GroupProfileCreationFragment();
        groupProfileCreationFragment.setArguments(bundle);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fl_messaging_fragment_container, groupProfileCreationFragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }
}
