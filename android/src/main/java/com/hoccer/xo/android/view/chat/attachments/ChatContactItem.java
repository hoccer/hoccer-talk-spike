package com.hoccer.xo.android.view.chat.attachments;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.content.ContentDisposition;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.xo.android.base.XoActivity;
import com.hoccer.xo.android.util.ColorSchemeManager;
import com.hoccer.xo.android.util.UriUtils;
import com.hoccer.xo.android.view.chat.ChatMessageItem;
import com.artcom.hoccer.R;
import ezvcard.Ezvcard;
import ezvcard.VCard;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
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
        ImageButton importButton = (ImageButton) mContentWrapper.findViewById(R.id.ib_vcard_import_button);

        int textColor = (mMessage.isIncoming()) ? mContext.getResources().getColor(R.color.xo_incoming_message_textColor) : mContext.getResources().getColor(R.color.xo_compose_message_textColor);

        contactName.setTextColor(textColor);
        contactDescription.setTextColor(textColor);
        showButton.setBackgroundDrawable(ColorSchemeManager.getRepaintedAttachmentDrawable(mContext, R.drawable.ic_light_contact, mMessage.isIncoming()));
        importButton.setBackgroundDrawable(showButton.getBackground());

        showButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LOG.debug("onClick(showButton)");
                if (isContentShowable()) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mContent.getContentUrl()));
                    XoActivity activity = (XoActivity) mContext;
                    activity.startExternalActivity(intent);
                }
            }
        });


        importButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LOG.debug("onClick(importButton)");
                if (isContentImportable()) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(UriUtils.getAbsoluteFileUri(mContent.getContentDataUrl()), mContent.getContentType());
                    XoActivity activity = (XoActivity) mContext;
                    activity.startExternalActivity(intent);
                }
            }
        });


        // apply data from VCard
        parseVCard();
        contactName.setText(getContactName());

        if (isContentShowable()) {
            showButton.setVisibility(View.VISIBLE);
        } else {
            showButton.setVisibility(View.GONE);
        }
        if (isContentImportable()) {
            importButton.setVisibility(View.VISIBLE);
        } else {
            importButton.setVisibility(View.GONE);
        }

    }

    private boolean isContentImported() {
        return mContent != null
                && mContent.getContentUrl() != null
                && !mContent.getContentUrl().startsWith("file://")
                && !mContent.getContentUrl().startsWith("content://media/external/file");
    }

    private boolean isContentImportable() {
        return mContent != null
                && mContent.getContentDisposition() == ContentDisposition.DOWNLOAD
                && !isContentImported();
    }

    private boolean isContentShowable() {
        return mContent != null
                && mContent.getContentDisposition() != ContentDisposition.SELECTED
                && isContentImported();
    }

    private boolean isContentChanged(IContentObject newContent) {
        return mContent == null
                || (newContent.getContentDataUrl() != null
                && !newContent.getContentDataUrl().equals(mContent.getContentDataUrl()));
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
        if (mVCard != null) {
            return;
        }

        Uri fileUri = UriUtils.getAbsoluteFileUri(mContentObject.getContentDataUrl());

        InputStream inputStream = openStreamForContentUri(fileUri);
        if (inputStream == null) {
            LOG.error("Could not open VCard at " + mContent.getContentDataUrl());
            return;
        }

        try {
            Ezvcard.ParserChainTextReader reader = Ezvcard.parse(inputStream);
            if (reader != null) {
                mVCard = reader.first();
            }
        } catch (IOException e) {
            LOG.error("Could not parse VCard", e);
        }
    }

    private @Nullable InputStream openStreamForContentUri(Uri contentUri) {
        if (contentUri == null) {
            return null;
        }
        InputStream inputStream = null;
        ContentResolver resolver = mContext.getContentResolver();
        try {
            inputStream = resolver.openInputStream(contentUri);
        } catch (FileNotFoundException e) {
            LOG.error("Could not find VCard at " + contentUri, e);
        }
        if (inputStream == null) {
            LOG.error("Do not know how to open " + contentUri);
        }
        return inputStream;
    }
}
