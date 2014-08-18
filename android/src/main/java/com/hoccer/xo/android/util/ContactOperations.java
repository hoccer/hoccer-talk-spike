package com.hoccer.xo.android.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Telephony;
import android.text.Html;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Static helper class.
 */
public class ContactOperations {

    private static final Logger LOG = Logger.getLogger(ContactOperations.class);

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

    public static void sendSMS(Context context, String message, String[] recipients) {
        LOG.debug("Sending SMS with message: " + message);

        String recipientString = "";
        for(int i = 0; i < recipients.length;i++) {
            recipientString += recipients[i] + ";";
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { // At least KitKat
            String defaultSmsPackageName = Telephony.Sms
                    .getDefaultSmsPackage(context); //Need to change the build to API 19

            Intent sendIntent = new Intent(Intent.ACTION_SENDTO);
            sendIntent.setData(Uri.parse("smsto:" + recipientString));
            sendIntent.setType("text/plain");
            sendIntent.putExtra(Intent.EXTRA_TEXT, message);

            if (defaultSmsPackageName != null) {
                sendIntent.setPackage(defaultSmsPackageName);
            }
            context.startActivity(sendIntent);
        } else {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("smsto:" + recipientString));
            intent.putExtra("sms_body", message);

            context.startActivity(intent);
        }
    }

    public static void sendEMail(Context context, String subject, String message, String[] recipients) {
        LOG.debug("Sending EMail with message: " + message);

        Intent email = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"));
        email.putExtra(Intent.EXTRA_SUBJECT, subject);
        email.putExtra(Intent.EXTRA_BCC, recipients);
        email.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(message));
        context.startActivity(Intent.createChooser(email, "Choose Email Client"));
    }
}
