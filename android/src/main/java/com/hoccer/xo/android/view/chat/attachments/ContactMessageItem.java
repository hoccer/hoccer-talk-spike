package com.hoccer.xo.android.view.chat.attachments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.XoTransfer;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.util.colorscheme.ColoredDrawable;
import com.hoccer.xo.android.util.UriUtils;
import com.hoccer.xo.android.view.chat.MessageItem;
import ezvcard.Ezvcard;
import ezvcard.VCard;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;


public class ContactMessageItem extends MessageItem {

    private final static Logger LOG = Logger.getLogger(ContactMessageItem.class);

    private VCard mVCard;

    public ContactMessageItem(Context context, TalkClientMessage message) {
        super(context, message);
    }

    @Override
    public ChatItemType getType() {
        return ChatItemType.ChatItemWithContact;
    }

    @Override
    protected void displayAttachment() {
        super.displayAttachment();

        // add view lazily
        if (mAttachmentContentContainer.getChildCount() == 0) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            RelativeLayout contactLayout = (RelativeLayout) inflater.inflate(R.layout.content_vcard, null);
            mAttachmentContentContainer.addView(contactLayout);
        }

        TextView contactName = (TextView) mAttachmentContentContainer.findViewById(R.id.tv_vcard_name);
        TextView contactDescription = (TextView) mAttachmentContentContainer.findViewById(R.id.tv_vcard_description);
        ImageButton showButton = (ImageButton) mAttachmentContentContainer.findViewById(R.id.ib_vcard_show_button);

        int textColor = (mMessage.isIncoming()) ? mContext.getResources().getColor(R.color.message_incoming_text) : mContext.getResources().getColor(R.color.compose_message_text);

        contactName.setTextColor(textColor);
        contactDescription.setTextColor(textColor);

        if (mMessage.isIncoming()) {
            showButton.setBackgroundDrawable(ColoredDrawable.getFromCache(R.drawable.ic_light_contact, R.color.attachment_incoming));
        } else {
            showButton.setBackgroundDrawable(ColoredDrawable.getFromCache(R.drawable.ic_light_contact, R.color.attachment_outgoing));
        }

        showButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LOG.debug("onClick(showButton)");
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(UriUtils.getAbsoluteFileUri(mAttachment.getFilePath()), "text/x-vcard");
                XoActivity activity = (XoActivity) mContext;
                activity.startExternalActivity(intent);
            }
        });

        if (mVCard == null) {
            parseVCard();
        }

        contactName.setText(getContactName());
    }

    private String getContactName() {
        String name;
        if (mVCard != null && mVCard.getFormattedName() != null) {
            name = mVCard.getFormattedName().getValue();
        } else {
            name = mContext.getResources().getString(R.string.content_contact);
        }
        return name;
    }

    private void parseVCard() {
        Uri fileUri = UriUtils.getAbsoluteFileUri(mAttachment.getFilePath());
        InputStream inputStream;
        try {
            inputStream = XoApplication.get().getXoClient().getHost().openInputStreamForUrl(fileUri.toString());
        } catch (IOException e) {
            LOG.error("Could not open VCard at " + mAttachment.getFilePath(), e);
            return;
        }

        try {
            Ezvcard.ParserChainTextReader reader = Ezvcard.parse(inputStream);
            mVCard = reader.first();
        } catch (IOException e) {
            LOG.error("Could not parse VCard", e);
        }
    }
}
