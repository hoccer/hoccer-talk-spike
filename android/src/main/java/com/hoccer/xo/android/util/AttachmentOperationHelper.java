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
 * Helper class.
 */
public class AttachmentOperationHelper {

    private static final Logger LOG = Logger.getLogger(AttachmentOperationHelper.class);

    public static final String ARG_CLIENT_CONTACT_ID = "com.hoccer.xo.android.argument.CLIENT_CONTACT_ID";
    public static final String ARG_MEDIA_COLLECTION_ID = "com.hoccer.xo.android.argument.MEDIA_COLLECTION_ID";
    public static final String ARG_CONTENT_MEDIA_TYPE = "com.hoccer.xo.android.argument.CONTENT_MEDIA_TYPE";

    public static void sendAttachmentsToContact(List<TalkClientDownload> attachments, TalkClientContact contact) throws FileNotFoundException, URISyntaxException {
        for (TalkClientDownload attachment : attachments) {
            sendAttachmentToContact(attachment, contact);
        }
    }

    public static void sendAttachmentToContact(TalkClientDownload attachment, TalkClientContact contact) throws FileNotFoundException, URISyntaxException {
        TalkClientUpload upload = createAttachmentUpload(attachment);
        String messageTag = XoApplication.getXoClient().composeClientMessage(contact, "", upload).getMessageTag();
        LOG.debug("Sending Attachment " + attachment + " to contact " + contact);
        XoApplication.getXoClient().sendMessage(messageTag);
    }

    public static TalkClientUpload createAttachmentUpload(TalkClientDownload object) throws FileNotFoundException, URISyntaxException {
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
