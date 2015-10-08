package com.hoccer.xo.android.view.chat.attachments;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.xo.android.util.UriUtils;
import com.hoccer.xo.android.util.colorscheme.ColoredDrawable;
import com.hoccer.xo.android.view.chat.MessageItem;
import org.apache.log4j.Logger;


public class FileMessageItem extends MessageItem {

    private final static Logger LOG = Logger.getLogger(FileMessageItem.class);

    public FileMessageItem(Context context, TalkClientMessage message) {
        super(context, message);
    }

    @Override
    public ChatItemType getType() {
        return ChatItemType.ChatItemWithData;
    }

    @Override
    protected void displayAttachment() {
        super.displayAttachment();

        // add view lazily
        if (mAttachmentContentContainer.getChildCount() == 0) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            View v = inflater.inflate(R.layout.content_file, null);
            mAttachmentContentContainer.addView(v);
        }

        LinearLayout layout = (LinearLayout) mAttachmentContentContainer.getChildAt(0);
        ImageButton openFileButton = (ImageButton) layout.findViewById(R.id.ib_content_file_open);
        TextView filenameTextView = (TextView) layout.findViewById(R.id.tv_filename);
        TextView filetypeTextView = (TextView) layout.findViewById(R.id.tv_filetype);

        filenameTextView.setText(mAttachment.getFilename());
        filetypeTextView.setText(UriUtils.getFileExtension(Uri.parse(mAttachment.getFilePath())));
        filetypeTextView.setAllCaps(true);

        if (mMessage.isIncoming()) {
            filenameTextView.setTextColor(mContext.getResources().getColor(R.color.message_incoming_text));
            filetypeTextView.setTextColor(mContext.getResources().getColor(R.color.message_incoming_text));
            openFileButton.setBackgroundDrawable(ColoredDrawable.getFromCache(R.drawable.ic_light_data, R.color.attachment_incoming));
        } else {
            filenameTextView.setTextColor(mContext.getResources().getColor(R.color.message_outgoing_text));
            filetypeTextView.setTextColor(mContext.getResources().getColor(R.color.message_outgoing_text));
            openFileButton.setBackgroundDrawable(ColoredDrawable.getFromCache(R.drawable.ic_light_data, R.color.attachment_outgoing));
        }

        openFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setDataAndType(UriUtils.getAbsoluteFileUri(mAttachment.getFilePath()), mAttachment.getMimeType());
                try {
                    mContext.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    LOG.error(e.getMessage(), e);
                    Toast.makeText(mContext, R.string.error_compatible_app_unavailable, Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
