package com.hoccer.xo.android.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.xo.android.adapter.NearbyChatAdapter;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.content.Clipboard;
import com.hoccer.xo.android.util.ThumbnailManager;
import com.hoccer.xo.android.view.chat.ChatMessageItem;
import com.hoccer.xo.release.R;

import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import java.sql.SQLException;

public class NearbyHistoryMessagingActivity extends XoActivity {
    private IContentObject mClipboardAttachment;
    private ListView mListView;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_nearby_history_messaging;
    }

    @Override
    protected int getMenuResource() {
        return R.menu.common;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        enableUpNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mListView = (ListView) findViewById(R.id.lv_nearby_history_chat);
        NearbyChatAdapter adapter = new NearbyChatAdapter(mListView, this);
        adapter.onResume();
        mListView.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ThumbnailManager.getInstance(this).clearCache();
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
            case R.id.menu_copy_attachment:
                if (mClipboardAttachment != null) {
                    Clipboard clipboard = Clipboard.get(this);
                    clipboard.storeAttachment(mClipboardAttachment);
                    mClipboardAttachment = null;
                } else {
                    ClipboardManager clipboardText = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("simple text",text);
                    clipboardText.setPrimaryClip(clip);
                }
                break;
            case R.id.menu_delete_message:
                try {
                    getXoDatabase().deleteMessageById(messageId);
                    ((NearbyChatAdapter)mListView.getAdapter()).updateAdapter();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                break;
        }
    }
}

