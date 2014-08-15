package com.hoccer.xo.android.util;

import com.hoccer.talk.client.XoTransfer;
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

    public static void sendTransfersToContact(List<XoTransfer> transfers, TalkClientContact contact) throws FileNotFoundException, URISyntaxException {
        for (XoTransfer transfer : transfers) {
            sendTransferToContact(transfer, contact);
        }
    }

    public static void sendTransferToContact(XoTransfer transfer, TalkClientContact contact) throws FileNotFoundException, URISyntaxException {
        File file = new File(transfer.getDataFile());

        TalkClientUpload upload = new TalkClientUpload();
        upload.initializeAsAttachment(
                transfer.getFileName(),
                transfer.getContentUrl(),
                transfer.getContentDataUrl(),
                transfer.getContentType(),
                transfer.getContentMediaType(),
                transfer.getContentAspectRatio(),
                (int) file.length(),
                transfer.getContentHmac());

        String messageTag = XoApplication.getXoClient().composeClientMessage(contact, "", upload).getMessageTag();
        LOG.debug("Sending Attachment " + upload + " to contact " + contact);
        XoApplication.getXoClient().sendMessage(messageTag);
    }
}
