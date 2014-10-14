package com.hoccer.xo.android.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.content.Clipboard;
import com.hoccer.xo.android.content.ClipboardContent;
import com.hoccer.xo.android.fragment.ClipboardPreviewFragment;
import com.hoccer.xo.android.util.IntentHelper;
import com.hoccer.xo.release.R;

import java.sql.SQLException;


public class ClipboardPreviewActivity extends XoActivity {

    private ClipboardPreviewFragment mClipboardPreviewFragment;
    private ClipboardContent mContent;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_clipboard_preview;
    }

    @Override
    protected int getMenuResource() {
        return R.menu.fragment_clipboard_preview;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager fragmentManager = getSupportFragmentManager();
        mClipboardPreviewFragment = (ClipboardPreviewFragment) fragmentManager.findFragmentById(R.id.activity_clipboard_preview_fragment);

        Intent i = getIntent();
        if (i != null) {
            if (i.hasExtra(IntentHelper.EXTRA_CONTENT_OBJECT)) {
                mContent = i.getParcelableExtra(IntentHelper.EXTRA_CONTENT_OBJECT);
                mClipboardPreviewFragment.setContentObject(mContent);
            }
        }
    }

    public void sendSelectionIntent() {
        Intent intent = new Intent();
        intent.putExtra(IntentHelper.EXTRA_CONTENT_OBJECT, mContent);

        if (getParent() == null) {
            setResult(Activity.RESULT_OK, intent);
        } else {
            getParent().setResult(Activity.RESULT_OK, intent);
        }
        finish();
    }
}
