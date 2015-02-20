package com.hoccer.xo.android.activity;

import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.view.MenuItem;
import android.widget.Toast;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientMessage;
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
    protected void handleContactSelection(ArrayList<Integer> selectedContactIds) {
        if (getIntent().hasExtra(Intent.EXTRA_TEXT)) {
            sendMessageToContacts(getTextFromIntent(), selectedContactIds);
            showToast(getString(R.string.sending_message));
        } else if (getIntent().hasExtra(Intent.EXTRA_STREAM)) {
            List<Uri> contentUris = getContentUrisFromIntent();
            sendUploadsToContacts(createUploadsFromContentUris(contentUris), selectedContactIds);
            showToast(getResources().getQuantityString(R.plurals.sending_attachments, contentUris.size()));
        }

        startChatsActivity();
        finish();
    }

    private String getTextFromIntent() {
        String subject = getIntent().getStringExtra(Intent.EXTRA_SUBJECT) != null ? getIntent().getStringExtra(Intent.EXTRA_SUBJECT) + "\n" : "";
        String url = getIntent().getStringExtra(Intent.EXTRA_TEXT);

        return subject + url;
    }

    private void sendMessageToContacts(String textFromIntent, List<Integer> selectedContactIds) {
        for (Integer contactId : selectedContactIds) {
            try {
                TalkClientContact contact = XoApplication.get().getXoClient().getDatabase().findContactById(contactId);
                TalkClientMessage message = getXoClient().composeClientMessage(contact, textFromIntent);
                getXoClient().sendMessage(message.getMessageTag());
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
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

    private List<TalkClientUpload> createUploadsFromContentUris(List<Uri> contentUris) {
        List<TalkClientUpload> uploads = new ArrayList<TalkClientUpload>();
        for (Uri contentUri : contentUris) {
            TalkClientUpload upload = new TalkClientUpload();
            upload.initializeAsAttachment(getSelectedContent(contentUri, getIntent().getType()));
            uploads.add(upload);
        }

        return uploads;
    }

    private SelectedContent getSelectedContent(Uri contentUri, String mimeType) {
        Intent intent = new Intent();
        intent.setData(contentUri);

        return getContentSelector(mimeType).createObjectFromSelectionResult(this, intent);
    }

    private IContentSelector getContentSelector(String mimeType) {
        IContentSelector selector = null;
        if (mimeType.startsWith("image/")) {
            selector = new ImageSelector(this);
        } else if (mimeType.startsWith("video/")) {
            selector = new VideoSelector(this);
        } else if (mimeType.startsWith("audio/")) {
            selector = new AudioSelector(this);
        }

        return selector;
    }

    private void sendUploadsToContacts(List<TalkClientUpload> uploads, List<Integer> selectedContactIds) {
        for (Integer contactId : selectedContactIds) {
            try {
                TalkClientContact contact = XoApplication.get().getXoClient().getDatabase().findContactById(contactId);
                ContactOperations.sendTransfersToContact(uploads, contact);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
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
