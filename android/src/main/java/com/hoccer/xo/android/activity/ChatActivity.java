package com.hoccer.xo.android.activity;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.view.*;
import android.widget.LinearLayout;
import com.artcom.hoccer.R;
import com.hoccer.talk.model.TalkEnvironment;
import com.hoccer.xo.android.activity.component.ActivityComponent;
import com.hoccer.xo.android.activity.component.MediaPlayerActivityComponent;
import com.hoccer.xo.android.fragment.ChatFragment;
import com.hoccer.xo.android.fragment.HistoryFragment;
import com.hoccer.xo.android.util.IntentHelper;
import com.hoccer.xo.android.view.chat.attachments.TransferControlView;
import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.ExceptionHandler;
import org.apache.log4j.Logger;

import java.sql.SQLException;


public class ChatActivity extends ComposableActivity {

    private static final Logger LOG = Logger.getLogger(ChatActivity.class);

    public static final String EXTRA_ENVIRONMENT_GROUP_HISTORY = "com.hoccer.xo.android.intent.extra.ENVIRONMENT_GROUP_HISTORY";
    public static final String EXTRA_CLIENT_HISTORY = "com.hoccer.xo.android.intent.extra.NEARBY_CLIENT_HISTORY";

    public static final String SHARED_PREFERENCES = "chats";

    private ChatFragment mChatFragment;

    private boolean mIsKeyboardOpen = false;

    private TransferControlView mSpinner;
    private Handler mDialogDismisser;
    private Dialog mDialog;

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

        checkKeys();

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

    private void checkKeys() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplication());
        boolean needToRegenerateKey = preferences.getBoolean("NEED_TO_REGENERATE_KEYS", true);

        if (needToRegenerateKey) {
            createDialog();
            regenerateKeys();
        }
    }

    private void createDialog() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.waiting_dialog, null);
        mSpinner = (TransferControlView) view.findViewById(R.id.content_progress);

        mDialog = new Dialog(this);
        mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mDialog.setContentView(view);
        mDialog.getWindow()
                .setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.show();
        mDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                return true;
            }
        });

        Handler spinnerStarter = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                mSpinner.prepareToUpload();
                mSpinner.spin();
            }
        };
        mDialogDismisser = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                try {
                    mDialog.dismiss();
                    mSpinner.completeAndGone();
                } catch (IllegalArgumentException e) {
                    LOG.error("Dialog is not attached to current activity.");
                    e.printStackTrace();
                    //TODO: Once upon a time we will redesign all this stuff... Maybe.
                }
            }
        };
        spinnerStarter.sendEmptyMessageDelayed(0, 500);
    }

    private void regenerateKeys() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    getClient().regenerateKeyPair();

                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplication());
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean("NEED_TO_REGENERATE_KEYS", false);
                    editor.apply();
                } catch (SQLException e) {
                    e.printStackTrace();
                } finally {
                    mDialogDismisser.sendEmptyMessage(0);
                }
            }
        });
        t.start();
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    private void initKeyboardCallback() {
        final LinearLayout view = (LinearLayout) findViewById(R.id.content);
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if ((view.getRootView().getHeight() - view.getHeight()) >
                        view.getRootView().getHeight() / 3) {
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
        if ((mIsKeyboardOpen == isOpen) || (mChatFragment == null)) {
            return;
        } else {
            mIsKeyboardOpen = isOpen;
        }
        if (mIsKeyboardOpen) {
            mChatFragment.onKeyboardOpen();
        }
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
