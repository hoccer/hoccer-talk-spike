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
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.util.ColorSchemeManager;
import com.hoccer.xo.android.util.UriUtils;
import com.hoccer.xo.android.view.chat.ChatMessageItem;
import ezvcard.Ezvcard;
import ezvcard.VCard;

import java.io.IOException;
import java.io.InputStream;


public class ChatContactItem extends ChatMessageItem {

    private IContentObject mContent;
    private VCard mVCard;

    public ChatContactItem(Context context, TalkClientMessage message) {
        super(context, message);
    }

    @Override
    public ChatItemType getType() {
        return ChatItemType.ChatItemWithContact;
    }

    @Override
    protected void configureViewForMessage(View view) {
        super.configureViewForMessage(view);
        configureAttachmentViewForMessage(view);
    }

    @Override
    protected void displayAttachment(IContentObject contentObject) {
        super.displayAttachment(contentObject);
        mContent = contentObject;

        // add view lazily
        if (mContentWrapper.getChildCount() == 0) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            RelativeLayout contactLayout = (RelativeLayout) inflater.inflate(R.layout.content_vcard, null);
            mContentWrapper.addView(contactLayout);
        }

        TextView contactName = (TextView) mContentWrapper.findViewById(R.id.tv_vcard_name);
        TextView contactDescription = (TextView) mContentWrapper.findViewById(R.id.tv_vcard_description);
        ImageButton showButton = (ImageButton) mContentWrapper.findViewById(R.id.ib_vcard_show_button);

        int textColor = (mMessage.isIncoming()) ? mContext.getResources().getColor(R.color.xo_incoming_message_textColor) : mContext.getResources().getColor(R.color.xo_compose_message_textColor);

        contactName.setTextColor(textColor);
        contactDescription.setTextColor(textColor);
        showButton.setBackgroundDrawable(ColorSchemeManager.getRepaintedAttachmentDrawable(mContext, R.drawable.ic_light_contact, mMessage.isIncoming()));

        showButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LOG.debug("onClick(showButton)");
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(UriUtils.getAbsoluteFileUri(mContent.getFilePath()), mContent.getContentType());
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
        Uri fileUri = UriUtils.getAbsoluteFileUri(mContentObject.getFilePath());
        InputStream inputStream;
        try {
            inputStream = XoApplication.getXoClient().getHost().openInputStreamForUrl(fileUri.toString());
        } catch (IOException e) {
            LOG.error("Could not open VCard at " + mContent.getFilePath(), e);
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
