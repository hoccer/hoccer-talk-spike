package com.hoccer.xo.android.activity;

import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.view.MenuItem;
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

import java.util.ArrayList;
import java.util.List;

public class ContactSelectionSharingActivity extends ContactSelectionActivity implements ContactSelectionFragment.IContactSelectionListener {

    private static final Logger LOG = Logger.getLogger(ContactSelectionSharingActivity.class);

    @Override
    protected void handleContactSelection() {
        sendUploadsToContacts(createUploads(getContentUrisFromIntent()));
        startChatsActivity();
        finish();
    }

    private List<Uri> getContentUrisFromIntent() {
        List<Uri> result = new ArrayList<Uri>();
        Uri uri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
        if (uri != null) {
            result.add(uri);
        } else {
            result = getIntent().getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        }

        return result;
    }

    private List<TalkClientUpload> createUploads(List<Uri> contentUris) {
        List<TalkClientUpload> uploads = new ArrayList<TalkClientUpload>();
        for (Uri contentUri : contentUris) {
            TalkClientUpload upload = new TalkClientUpload();
            upload.initializeAsAttachment(getSelectedContent(contentUri, getIntent().getType()));
            uploads.add(upload);
        }

        return uploads;
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

    private void sendUploadsToContacts(List<TalkClientUpload> uploads) {
        for (Integer contactId : getSelectedContactIdsFromFragment()) {
            try {
                TalkClientContact contact = XoApplication.get().getXoClient().getDatabase().findContactById(contactId);
                ContactOperations.sendTransfersToContact(uploads, contact);
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
