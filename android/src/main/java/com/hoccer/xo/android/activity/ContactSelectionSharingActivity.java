package com.hoccer.xo.android.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.view.MenuItem;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.content.SelectedContent;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.content.selector.AudioSelector;
import com.hoccer.xo.android.content.selector.IContentSelector;
import com.hoccer.xo.android.content.selector.ImageSelector;
import com.hoccer.xo.android.content.selector.VideoSelector;
import com.hoccer.xo.android.fragment.ContactSelectionFragment;
import com.hoccer.xo.android.util.ContactOperations;
import org.apache.log4j.Logger;

public class ContactSelectionSharingActivity extends ContactSelectionActivity implements ContactSelectionFragment.IContactSelectionListener {

    private static final Logger LOG = Logger.getLogger(ContactSelectionSharingActivity.class);

    @Override
    protected void handleContactSelection() {
        sendUploadToContacts(createUpload(getContentUriFromIntent()));
        startChatsActivity();
        finish();
    }

    private Uri getContentUriFromIntent() {
        return getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
    }

    private TalkClientUpload createUpload(Uri contentUri) {
        TalkClientUpload upload = new TalkClientUpload();
        upload.initializeAsAttachment(getSelectedContent(contentUri, getIntent().getType()));

        return upload;
    }

    private SelectedContent getSelectedContent(Uri contentUri, String type) {
        Intent intent = new Intent();
        intent.setData(contentUri);

        return getContentSelector(type).createObjectFromSelectionResult(this, intent);
    }

    private IContentSelector getContentSelector(String type) {
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

    private void sendUploadToContacts(TalkClientUpload upload) {
        for (Integer contactId : getSelectedContactIdsFromFragment()) {
            try {
                TalkClientContact contact = XoApplication.getXoClient().getDatabase().findContactById(contactId);
                ContactOperations.sendTransferToContact(upload, contact);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    private void startChatsActivity() {
        Intent intent = new Intent(this, ChatsActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                createNewTaskStackAndStartParent();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void createNewTaskStackAndStartParent() {
        Intent upIntent = NavUtils.getParentActivityIntent(this);
        TaskStackBuilder.create(this)
                .addNextIntentWithParentStack(upIntent)
                .startActivities();
    }
}
