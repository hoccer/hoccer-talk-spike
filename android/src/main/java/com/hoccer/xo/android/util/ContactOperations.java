package com.hoccer.xo.android.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Telephony;
import android.text.Html;
import com.hoccer.talk.client.XoTransfer;
import com.hoccer.talk.client.model.TalkClientContact;
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
public class ContactOperations {

    private static final Logger LOG = Logger.getLogger(ContactOperations.class);

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
                (int)file.length(),
                transfer.getContentHmac());

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

        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { // Android 4.4 and up
            String defaultSmsPackageName = Telephony.Sms.getDefaultSmsPackage(context);

            intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(recipientString)));
            intent.putExtra("sms_body", message);

            if (defaultSmsPackageName != null) { // Can be null in case that there is no default, then the user would be able to choose any app that supports this intent.
                intent.setPackage(defaultSmsPackageName);
            }
        }
        else {
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setType("vnd.android-dir/mms-sms");
            intent.putExtra("address", recipientString);
            intent.putExtra("sms_body", message);
        }
        context.startActivity(intent);
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
