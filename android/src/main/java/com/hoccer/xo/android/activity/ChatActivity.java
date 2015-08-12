package com.hoccer.xo.android.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import com.artcom.hoccer.R;
import com.hoccer.talk.model.TalkEnvironment;
import com.hoccer.xo.android.activity.component.ActivityComponent;
import com.hoccer.xo.android.activity.component.MediaPlayerActivityComponent;
import com.hoccer.xo.android.content.Clipboard;
import com.hoccer.xo.android.fragment.ChatFragment;
import com.hoccer.xo.android.fragment.HistoryFragment;
import com.hoccer.xo.android.util.IntentHelper;
import com.hoccer.xo.android.view.chat.MessageItem;
import org.apache.log4j.Logger;


public class ChatActivity extends ComposableActivity {

    private static final Logger LOG = Logger.getLogger(ChatActivity.class);

    public static final String EXTRA_ENVIRONMENT_GROUP_HISTORY = "com.hoccer.xo.android.intent.extra.ENVIRONMENT_GROUP_HISTORY";
    public static final String EXTRA_CLIENT_HISTORY = "com.hoccer.xo.android.intent.extra.NEARBY_CLIENT_HISTORY";

    public static final String SHARED_PREFERENCES = "chats";

    private ChatFragment mChatFragment;

    private boolean mIsKeyboardOpen = false;

    @Override
    protected ActivityComponent[] createComponents() {
        return new ActivityComponent[]{new MediaPlayerActivityComponent(this)};
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

        if (getIntent() == null) {
            return;
        }

        int contactId = getIntent().getIntExtra(IntentHelper.EXTRA_CONTACT_ID, -1);
        if (contactId != -1) {
            if (getIntent().hasExtra(EXTRA_CLIENT_HISTORY)) {
                showHistoryFragment(contactId);
            } else {
                showChatFragment(contactId);
            }
        } else {
            String type = getIntent().getStringExtra(EXTRA_ENVIRONMENT_GROUP_HISTORY);
            if (type != null) {
                if (TalkEnvironment.TYPE_NEARBY.equals(type)) {
                    showNearbyGroupHistoryFragment();
                } else if (TalkEnvironment.TYPE_WORLDWIDE.equals(type)) {
                    showWorldwideGroupHistoryFragment();
                }
            }
        }

        enableUpNavigation();
        initKeyboardCallback();
    }

    private void initKeyboardCallback() {
        final LinearLayout view = (LinearLayout) findViewById(R.id.content);
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if ((view.getRootView().getHeight() - view.getHeight()) >
                        view.getRootView().getHeight()/3) {
                    // keyboard is open
                    onKeyboardOpen(true);
                } else {
                    // keyboard is closed
                    onKeyboardOpen(false);
                }
            }
        });
    }

    private void onKeyboardOpen(boolean isOpen) {
        if((mIsKeyboardOpen == isOpen) || (mChatFragment == null)) {
            return;
        } else {
            mIsKeyboardOpen = isOpen;
        }
        if(mIsKeyboardOpen) {
            mChatFragment.onKeyboardOpen();
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

    private void showChatFragment(int contactId) {
        Bundle bundle = new Bundle();
        bundle.putInt(ChatFragment.ARG_CLIENT_CONTACT_ID, contactId);

        mChatFragment = new ChatFragment();
        mChatFragment.setArguments(bundle);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fl_messaging_fragment_container, mChatFragment);
        fragmentTransaction.commit();
    }

    private void showHistoryFragment(int contactId) {
        Bundle bundle = new Bundle();
        bundle.putInt(HistoryFragment.ARG_CLIENT_CONTACT_ID, contactId);

        replaceWithHistoryFragment(bundle);
    }

    private void showNearbyGroupHistoryFragment() {
        Bundle bundle = new Bundle();
        bundle.putString(HistoryFragment.ARG_GROUP_HISTORY, TalkEnvironment.TYPE_NEARBY);

        replaceWithHistoryFragment(bundle);
    }

    private void showWorldwideGroupHistoryFragment() {
        Bundle bundle = new Bundle();
        bundle.putString(HistoryFragment.ARG_GROUP_HISTORY, TalkEnvironment.TYPE_WORLDWIDE);

        replaceWithHistoryFragment(bundle);
    }

    private void replaceWithHistoryFragment(Bundle bundle) {
        HistoryFragment fragment = new HistoryFragment();
        fragment.setArguments(bundle);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fl_messaging_fragment_container, fragment);
        fragmentTransaction.commit();
    }
}
