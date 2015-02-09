package com.hoccer.xo.android.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import com.artcom.hoccer.R;
import com.hoccer.talk.content.SelectedContent;
import com.hoccer.xo.android.activity.component.ActivityComponent;
import com.hoccer.xo.android.activity.component.MediaPlayerActivityComponent;
import com.hoccer.xo.android.activity.component.ViewPagerActivityComponent;
import com.hoccer.xo.android.content.Clipboard;
import com.hoccer.xo.android.content.selector.IContentSelector;
import com.hoccer.xo.android.content.selector.ImageSelector;
import com.hoccer.xo.android.content.selector.VideoSelector;
import com.hoccer.xo.android.fragment.ClientContactListFragment;
import com.hoccer.xo.android.fragment.GroupContactListFragment;

public class ContactsActivity extends ComposableActivity {

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_contacts;
    }

    @Override
    protected int getMenuResource() {
        return R.menu.menu_activity_contacts;
    }

    @Override
    protected ActivityComponent[] createComponents() {
        return new ActivityComponent[]{
                new MediaPlayerActivityComponent(this),
                new ViewPagerActivityComponent(this,
                        R.id.pager,
                        new ClientContactListFragment(),
                        new GroupContactListFragment())
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Intent.ACTION_SEND.equals(getIntent().getAction())) {
            handleShareIntent(getIntent());
        }

        enableUpNavigation();
    }
    private void handleShareIntent(Intent intent) {
        Uri contentUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        SelectedContent content = getContent(contentUri, intent.getType());
        addSharedContentToClipboard(content);
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
        }

        return selector;
    }

    private void addSharedContentToClipboard(SelectedContent content) {
        if (content != null) {
            Clipboard.getInstance().setContent(content);
            Toast.makeText(this, getString(R.string.toast_stored_file_in_clipboard), Toast.LENGTH_LONG).show();
        } else {
            Clipboard.getInstance().clearContent();
            Toast.makeText(this, R.string.toast_failed_to_store_file_in_clipboard, Toast.LENGTH_LONG).show();
        }
    }
}