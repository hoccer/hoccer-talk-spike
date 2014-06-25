package com.hoccer.xo.android.activity;

import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.content.Clipboard;
import com.hoccer.xo.android.fragment.CompositionFragment;
import com.hoccer.xo.android.fragment.MessagingFragment;
import com.hoccer.xo.android.view.chat.ChatMessageItem;
import com.hoccer.xo.release.R;

import java.sql.SQLException;

public class MessagingActivity extends XoActivity implements IXoContactListener {

    public static final String EXTRA_CLIENT_CONTACT_ID = "clientContactId";

    ActionBar mActionBar;

    MessagingFragment mMessagingFragment;
    CompositionFragment mCompositionFragment;

    TalkClientContact mContact;
    private IContentObject mClipboardAttachment;
    private getContactIdInConversation m_checkIdReceiver;

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
        LOG.debug("onCreate()");
        super.onCreate(savedInstanceState);

        // get action bar (for setting title)
        mActionBar = getActionBar();

        // enable up navigation
        enableUpNavigation();

        // get our primary fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        mMessagingFragment = (MessagingFragment) fragmentManager.findFragmentById(R.id.activity_messaging_fragment);
        mMessagingFragment.setRetainInstance(true);
        mCompositionFragment = (CompositionFragment) fragmentManager.findFragmentById(R.id.activity_messaging_composer);
        mCompositionFragment.setRetainInstance(true);

        // register receiver for notification check
        IntentFilter filter = new IntentFilter("com.hoccer.xo.android.activity.MessagingActivity$getContactIdInConversation");
        filter.addAction("CHECK_ID_IN_CONVERSATION");
        m_checkIdReceiver = new getContactIdInConversation();
        registerReceiver(m_checkIdReceiver, filter);
    }

    @Override
    protected void onResume() {
        LOG.debug("onResume()");
        super.onResume();

        Intent intent = getIntent();

        // handle converse intent
        if (intent != null && intent.hasExtra(EXTRA_CLIENT_CONTACT_ID)) {
            int contactId = intent.getIntExtra(EXTRA_CLIENT_CONTACT_ID, -1);
            m_checkIdReceiver.setId(contactId);
            if (contactId == -1) {
                LOG.error("invalid contact id");
            } else {
                try {
                    TalkClientContact contact = getXoDatabase().findClientContactById(contactId);
                    if (contact != null) {
                        converseWithContact(contact);
                    }
                } catch (SQLException e) {
                    LOG.error("sql error", e);
                }
            }
        }
        getXoClient().registerContactListener(this);
    }

    @Override
    protected void onPause() {
        LOG.debug("onPause()");
        super.onPause();

        getXoClient().unregisterContactListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        LOG.debug("onCreateOptionsMenu()");
        boolean result = super.onCreateOptionsMenu(menu);

        // select client/group profile entry for appropriate icon
        if (mContact != null) {
            MenuItem clientItem = menu.findItem(R.id.menu_profile_client);
            clientItem.setVisible(mContact.isClient());
            MenuItem groupItem = menu.findItem(R.id.menu_single_profile);
            groupItem.setVisible(mContact.isGroup());
        }

        return result;
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(m_checkIdReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        LOG.debug("onOptionsItemSelected(" + item.toString() + ")");
        switch (item.getItemId()) {
            case R.id.menu_profile_client:
            case R.id.menu_single_profile:
                if (mContact != null) {
                    showContactProfile(mContact);
                }
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void showPopupForMessageItem(ChatMessageItem messageItem, View messageItemView) {
        IContentObject contentObject = messageItem.getContent();

        if (contentObject.isContentAvailable()) {
            mClipboardAttachment = contentObject;

            PopupMenu popup = new PopupMenu(this, messageItemView);
            popup.getMenuInflater().inflate(R.menu.popup_menu_messaging, popup.getMenu());
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

                public boolean onMenuItemClick(MenuItem item) {
                    popupItemSelected(item);
                    return true;
                }
            });

            popup.show();
        }
    }

    public void popupItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_copy_attachment:
                Clipboard clipboard = Clipboard.get(this);
                clipboard.storeAttachment(mClipboardAttachment);
                mClipboardAttachment = null;
        }
    }

    @Override
    public void clipBoardItemSelected(IContentObject contentObject) {
        mCompositionFragment.onAttachmentSelected(contentObject);
    }

    public void converseWithContact(TalkClientContact contact) {
        LOG.debug("converseWithContact(" + contact.getClientContactId() + ")");
        mContact = contact;
        mActionBar.setTitle(contact.getName());
        mMessagingFragment.converseWithContact(contact);
        mCompositionFragment.converseWithContact(contact);
        if (mContact.isDeleted()) {
            finish();
        }
        // invalidate menu so that profile buttons get disabled/enabled
        invalidateOptionsMenu();
    }

    @Override
    protected void applicationWillEnterBackground() {
        super.applicationWillEnterBackground();
        if (mContact.isGroup() && mContact.getGroupPresence().isTypeNearby()) {
            finish();
        } else if (mContact.isClient() && mContact.isNearby()) {
            finish();
        }
    }

    @Override
    public void onContactAdded(TalkClientContact contact) {
        // we don't care
    }

    @Override
    public void onContactRemoved(TalkClientContact contact) {
        if (mContact != null && mContact.getClientContactId() == contact.getClientContactId()) {
            finish();
        }
    }

    @Override
    public void onClientPresenceChanged(TalkClientContact contact) {
        // we don't care
    }

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {
        // we don't care
    }

    @Override
    public void onGroupPresenceChanged(TalkClientContact contact) {
        // we don't care
    }

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) {
        // we don't care
    }

    private class getContactIdInConversation extends BroadcastReceiver {
        private int m_contactId;

        public void setId(int id) {
            m_contactId = id;
        }

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            Intent intent = new Intent();
            intent.setAction("CONTACT_ID_IN_CONVERSATION");
            intent.putExtra("id", m_contactId);
            sendBroadcast(intent);
        }

    }

}
