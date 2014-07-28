package com.hoccer.xo.android.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.activity.ContactSelectionActivity;
import com.hoccer.xo.android.activity.MediaCollectionSelectionActivity;
import com.hoccer.xo.android.content.AudioAttachmentItem;
import com.hoccer.xo.android.content.audio.MediaPlaylistController;
import com.hoccer.xo.android.service.MediaPlayerService;
import com.hoccer.xo.android.service.MediaPlayerServiceConnector;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by nico on 23/07/2014.
 */
public class AttachmentOperationHelper {

    private static final Logger LOG = Logger.getLogger(AttachmentOperationHelper.class);

    public static final String ARG_CLIENT_CONTACT_ID = "com.hoccer.xo.android.argument.CLIENT_CONTACT_ID";
    public static final String ARG_MEDIA_COLLECTION_ID = "com.hoccer.xo.android.argument.MEDIA_COLLECTION_ID";
    public static final String ARG_CONTENT_MEDIA_TYPE = "com.hoccer.xo.android.argument.CONTENT_MEDIA_TYPE";

    public static void deleteAttachments(final Context context, final List<AudioAttachmentItem> attachments) {
        final MediaPlayerServiceConnector connector = new MediaPlayerServiceConnector();
        connector.connect(context, IntentHelper.ACTION_PLAYER_TRACK_CHANGED, new MediaPlayerServiceConnector.Listener() {

            @Override
            public void onConnected(MediaPlayerService service) {
                deleteAttachments(context, attachments, service);
                connector.disconnect();
            }

            @Override
            public void onDisconnected() {
            }

            @Override
            public void onAction(String action, MediaPlayerService service) {
            }
        });
    }

    public static void deleteAttachments(Context context, List<AudioAttachmentItem> attachments, MediaPlayerService service) {
        for (AudioAttachmentItem attachment : attachments) {
            deleteAttachment(context, attachment, service);
        }
    }

    public static void deleteAttachment(final Context context, final AudioAttachmentItem attachment) {

        final MediaPlayerServiceConnector connector = new MediaPlayerServiceConnector();
        connector.connect(context, IntentHelper.ACTION_PLAYER_TRACK_CHANGED, new MediaPlayerServiceConnector.Listener() {

            @Override
            public void onConnected(MediaPlayerService service) {
                deleteAttachment(context,attachment,service);
                connector.disconnect();
            }

            @Override
            public void onDisconnected() {
            }

            @Override
            public void onAction(String action, MediaPlayerService service) {
            }
        });
    }

    public static void deleteAttachment(Context context, final AudioAttachmentItem attachment, MediaPlayerService service) {
        boolean isPlaying = false;
        boolean isPaused = false;
        if (service != null && !service.isStopped() && !service.isPaused()) {
            if (attachment.equals(service.getCurrentMediaItem())) {
                isPlaying = true;
            }
        }

        if (service != null && service.isPaused()) {
            if (attachment.equals(service.getCurrentMediaItem())) {
                isPaused = true;
            }
        }

        if (isPlaying) {
            if (service.getRepeatMode() == MediaPlaylistController.RepeatMode.REPEAT_TITLE) {
                service.stop();
            } else {
                service.playNextByRepeatMode();
            }
        } else if (isPaused) {
            service.stop();
        }

        String path = Uri.parse(attachment.getFilePath()).getPath();
        File file = new File(path);

        if (file.delete()) {
            try {
                TalkClientDownload download = (TalkClientDownload) attachment.getContentObject();

                XoApplication.getXoClient().getDatabase().deleteClientDownload(download);

                int messageId = XoApplication.getXoClient().getDatabase().findMessageByDownloadId(download.getClientDownloadId()).getClientMessageId();
                XoApplication.getXoClient().getDatabase().deleteMessageById(messageId);

                service.removeMedia(attachment);

                Intent intent = new Intent(IntentHelper.ACTION_AUDIO_ATTACHMENT_REMOVED);
                intent.putExtra(IntentHelper.EXTRA_TALK_CLIENT_MESSAGE_ID, messageId);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            } catch (SQLException e) {
                LOG.error("Error deleting message with client download id of " + ((TalkClientDownload) attachment.getContentObject()).getClientDownloadId());
                e.printStackTrace();
            }
        }
    }

    public static void sendAttachmentsToContacts(List<AudioAttachmentItem> attachments, List<TalkClientContact> contacts) throws FileNotFoundException, URISyntaxException {
        for (TalkClientContact contact : contacts) {
            sendAttachmentsToContact(attachments, contact);
        }
    }

    public static void sendAttachmentsToContact(List<AudioAttachmentItem> attachments, TalkClientContact contact) throws FileNotFoundException, URISyntaxException {
        for (AudioAttachmentItem attachment : attachments) {
            sendAttachmentToContact(attachment, contact);
        }
    }

    public static void sendAttachmentToContact(AudioAttachmentItem attachment, TalkClientContact contact) throws FileNotFoundException, URISyntaxException {
        TalkClientUpload upload = createAttachmentUpload(attachment.getContentObject());
        String messageTag = XoApplication.getXoClient().composeClientMessage(contact, "", upload).getMessageTag();
        LOG.debug("Sending Attachment " + attachment + " to contact " + contact);
        XoApplication.getXoClient().sendMessage(messageTag);
    }

    public static TalkClientUpload createAttachmentUpload(IContentObject object) throws FileNotFoundException, URISyntaxException {
        URI fileUri = new URI(object.getContentDataUrl());
        File fileToUpload = new File(fileUri);

        if (!fileToUpload.exists()) {
            LOG.error("Error creating file from TalkClientDownloadObject.");
            throw new FileNotFoundException(fileToUpload.getAbsolutePath() + " could not be found on file system.");
        }

        TalkClientUpload upload = new TalkClientUpload();
        upload.initializeAsAttachment(
                object.getFileName(),
                object.getContentUrl(),
                object.getContentDataUrl(),
                object.getContentType(),
                object.getContentMediaType(),
                object.getContentAspectRatio(),
                (int) fileToUpload.length(),
                object.getContentHmac());

        return upload;
    }

}
