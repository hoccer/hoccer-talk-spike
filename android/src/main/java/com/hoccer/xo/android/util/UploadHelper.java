package com.hoccer.xo.android.util;

import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.xo.android.XoApplication;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Static helper class.
 */
public class UploadHelper {

    private static final Logger LOG = Logger.getLogger(UploadHelper.class);

    public static void sendDownloadsToContact(List<TalkClientDownload> attachments, TalkClientContact contact) throws FileNotFoundException, URISyntaxException {
        for (TalkClientDownload attachment : attachments) {
            sendDownloadToContact(attachment, contact);
        }
    }

    public static void sendDownloadToContact(TalkClientDownload download, TalkClientContact contact) throws FileNotFoundException, URISyntaxException {
        File file = new File(download.getDataFile());

        TalkClientUpload upload = new TalkClientUpload();
        upload.initializeAsAttachment(
                download.getFileName(),
                download.getContentUrl(),
                download.getContentDataUrl(),
                download.getContentType(),
                download.getContentMediaType(),
                download.getContentAspectRatio(),
                (int)file.length(),
                download.getContentHmac());

        String messageTag = XoApplication.getXoClient().composeClientMessage(contact, "", upload).getMessageTag();
        LOG.debug("Sending Attachment " + upload + " to contact " + contact);
        XoApplication.getXoClient().sendMessage(messageTag);
    }
}
