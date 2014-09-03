package com.hoccer.xo.android.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.Formatter;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.talk.model.TalkRelationship;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.base.XoFragment;
import com.hoccer.xo.android.content.ContentRegistry;
import com.hoccer.xo.android.content.ContentSelection;
import com.hoccer.xo.android.content.SelectedContent;
import com.hoccer.xo.android.gesture.Gestures;
import com.hoccer.xo.android.gesture.MotionGestureListener;
import com.hoccer.xo.android.util.ColorSchemeManager;
import com.hoccer.xo.android.util.ImageContentHelper;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CompositionFragment extends XoFragment implements View.OnClickListener,
        View.OnLongClickListener, MotionGestureListener {

    private static final Logger LOG = Logger.getLogger(CompositionFragment.class);

    public static final String ARG_CLIENT_CONTACT_ID = "com.hoccer.xo.android.fragment.ARG_CLIENT_CONTACT_ID";
    public static final int REQUEST_SELECT_ATTACHMENT = 42;
    public static final int REQUEST_SELECT_IMAGE_ATTACHMENTS = 43;

    private static final int STRESS_TEST_MESSAGE_COUNT = 15;

    private EditText mTextEdit;
    private TextWatcher mTextWatcher;
    private ImageButton mSendButton;
    private TalkClientContact mContact;
    private String mLastMessage = null;
    private ImageButton mAddAttachmentButton;

    private ArrayList<IContentObject> mAttachments;

    private ContentSelection mAttachmentSelection = null;
    private Button mAddAttachmentsButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            int clientContactId = getArguments().getInt(ARG_CLIENT_CONTACT_ID);
            if (clientContactId == -1) {
                LOG.error("invalid contact id");
            } else {
                try {
                    mContact = XoApplication.getXoClient().getDatabase().findClientContactById(clientContactId);
                } catch (SQLException e) {
                    LOG.error("sql error", e);
                }
            }
        } else {
            LOG.error("MessagingFragment requires contactId as argument.");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        LOG.debug("onCreateView()");
        super.onCreateView(inflater, container, savedInstanceState);

        View v = inflater.inflate(R.layout.fragment_composition, container, false);

        setHasOptionsMenu(true);

        mTextEdit = (EditText) v.findViewById(R.id.messaging_composer_text);
        mTextEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if (isComposed()) {
                        processMessage();
                    }
                }
                return false;
            }
        });

        mSendButton = (ImageButton) v.findViewById(R.id.btn_messaging_composer_send);
        mSendButton.setEnabled(false);
        mSendButton.setOnClickListener(this);

        if (XoApplication.getConfiguration().isDevelopmentModeEnabled()) {
            mSendButton.setOnLongClickListener(this);
            mSendButton.setLongClickable(true);
        }

        AddAttachmentOnClickListener listener = new AddAttachmentOnClickListener();

        mAddAttachmentButton = (ImageButton) v.findViewById(R.id.ib_messaging_composer_add_attachment);
        mAddAttachmentButton.setOnClickListener(listener);

        mAddAttachmentsButton = (Button) v.findViewById(R.id.b_messaging_composer_add_attachments);
        mAddAttachmentsButton.setOnClickListener(listener);

        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_composition, menu);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_SELECT_ATTACHMENT) {
                IContentObject contentObject = ContentRegistry.get(getActivity()).createSelectedAttachment(mAttachmentSelection, intent);
                if (contentObject != null) {
                    onAttachmentSelected(contentObject);
                } else {
                    showAttachmentSelectionError();
                }
            } else if (requestCode == REQUEST_SELECT_IMAGE_ATTACHMENTS) {
                ArrayList<IContentObject> contentObjects = ContentRegistry.get(getActivity()).createSelectedImagesAttachment(mAttachmentSelection, intent);
                if (contentObjects != null && !contentObjects.isEmpty()) {
                    onAttachmentsSelected(contentObjects);
                } else {
                    showAttachmentSelectionError();
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        mTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateSendButton();
            }
        };
        mTextEdit.addTextChangedListener(mTextWatcher);
        updateSendButton();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mTextWatcher != null) {
            mTextEdit.removeTextChangedListener(mTextWatcher);
            mTextWatcher = null;
        }
    }

    @Override
    public void onClick(View v) {
        processMessage();
    }

    private void selectAttachment() {
        getXoActivity().setBackgroundActive();
        mAttachmentSelection = ContentRegistry.get(getActivity()).selectAttachment(this, REQUEST_SELECT_ATTACHMENT);
    }

    public void onAttachmentSelected(IContentObject contentObject) {
        if (contentObject.getContentMediaType().equals(ContentMediaType.IMAGE) && getXoClient().isEncodingNecessary()) {
            encodeImageAttachment(contentObject);
        } else {
            setAttachment(contentObject);
            mSendButton.setEnabled(isComposed());
        }
    }

    public void onAttachmentsSelected(ArrayList<IContentObject> contentObjects) {
        LOG.debug("onAttachmentsSelected(" + contentObjects.size() + ")");
        if (getXoClient().isEncodingNecessary()) {
            for (IContentObject attachment : contentObjects) {
                if (attachment.getContentMediaType().equals(ContentMediaType.IMAGE)) {
                    encodeImageAttachment(attachment);
                }
            }
        } else {
            setAttachments(contentObjects);
            mSendButton.setEnabled(isComposed());
        }
    }

    private void showAttachmentSelectionError() {
        Toast.makeText(getActivity(), R.string.error_attachment_selection, Toast.LENGTH_LONG).show();
    }

    private void setAttachment(IContentObject contentObject) {
        mAddAttachmentButton.setOnClickListener(new AttachmentOnClickListener());
        mAttachments = new ArrayList<IContentObject>();
        mAttachments.add(contentObject);
        updateAttachmentButton();
    }

    private void setAttachments(ArrayList<IContentObject> contentObjects) {
        mAddAttachmentsButton.setOnClickListener(new AttachmentOnClickListener());
        if (contentObjects != null) {
            mAttachments = contentObjects;
        }
        updateAttachmentButton();
    }

    private void updateAttachmentButton() {
        if (mAttachments != null && mAttachments.size() > 0) {
            if (mAttachments.size() > 1) {
                attachMultipleUploadsButton(mAttachments.size());
            } else {
                String mediaType = mAttachments.get(0).getContentMediaType();
                if (mediaType != null) {
                    attachAttachmentButtonByMediaType(mediaType);
                }
            }
        }
    }

    private void attachAttachmentButtonByMediaType(String mediaType) {
        int imageResource;
        if (mediaType.equals(ContentMediaType.IMAGE)) {
            imageResource = R.drawable.ic_light_image;
        } else if (mediaType.equals(ContentMediaType.VIDEO)) {
            imageResource = R.drawable.ic_light_video;
        } else if (mediaType.equals(ContentMediaType.VCARD)) {
            imageResource = R.drawable.ic_light_contact;
        } else if (mediaType.equals(ContentMediaType.LOCATION)) {
            imageResource = R.drawable.ic_light_location;
        } else if (mediaType.equals(ContentMediaType.DATA)) {
            imageResource = R.drawable.ic_light_data;
        } else if (mediaType.equals(ContentMediaType.AUDIO)) {
            imageResource = R.drawable.ic_light_video;
        } else {
            imageResource = android.R.drawable.stat_notify_error;
        }
        mAddAttachmentButton.setImageDrawable(ColorSchemeManager.getRepaintedAttachmentDrawable(getXoActivity(), imageResource, true));
    }

    private void attachMultipleUploadsButton(int count) {
        mAddAttachmentButton.setVisibility(View.GONE);
        mAddAttachmentsButton.setVisibility(View.VISIBLE);
        if (count > 99) {
            mAddAttachmentsButton.setTextSize(13);
        }
        if (count > 999) {
            mAddAttachmentsButton.setTextSize(10);
        }
        mAddAttachmentsButton.setText(String.valueOf(count));
    }

    public void updateContact(TalkClientContact contact) {
        mContact = contact;
    }

    private boolean isComposed() {
        return (mTextEdit.getText().length() > 0 || mAttachments != null);
    }

    private void clearComposedMessage() {
        mTextEdit.setText("");
        clearAttachment();
    }

    private void clearAttachment() {
        mAddAttachmentButton.setOnClickListener(new AddAttachmentOnClickListener());
        mAddAttachmentButton.setBackgroundDrawable(null);
        mAddAttachmentButton.setImageResource(R.drawable.ic_light_content_attachment);
        mAddAttachmentButton.setVisibility(View.VISIBLE);
        mAddAttachmentsButton.setOnClickListener(new AddAttachmentOnClickListener());
        mAddAttachmentsButton.setText("");
        mAddAttachmentsButton.setVisibility(View.GONE);
        mAttachments = null;
        updateSendButton();
    }

    private void updateSendButton() {
        boolean enabled = (isComposed() || (XoApplication.getConfiguration().isDevelopmentModeEnabled() && mLastMessage != null));
        mSendButton.setEnabled(enabled);
    }

    private boolean isBlocked() {
        if (!mContact.isGroup() && !mContact.isNearby()) {
            TalkRelationship clientRelationship = mContact.getClientRelationship();
            if (clientRelationship != null && clientRelationship.getState() != null && clientRelationship.getState().equals(TalkRelationship.STATE_BLOCKED)) {
                return true;
            }
        }
        return false;
    }

    private boolean isEmptyGroup() {
        return (mContact != null && mContact.isGroup() && mContact.isEmptyGroup());
    }

    private boolean uploadExceedsTransferLimit(List<IContentObject> uploads) {
        int transferLimit = getXoClient().getUploadLimit();

        if (transferLimit == -2) {
            return true;
        }
        if (transferLimit == -1) {
            return false;
        }

        long fileSize = calculateAttachmentSize(uploads);
        return (transferLimit >= 0 && fileSize >= transferLimit);
    }

    private long calculateAttachmentSize(List<IContentObject> attachments) {
        long fileSize = 0;
        for (IContentObject upload : attachments) {
            fileSize += upload.getContentLength();
        }
        return fileSize;
    }

    private void validateAndSendComposedMessage() {
        if (mContact == null) {
            return;
        }

        String messageText = mTextEdit.getText().toString();
        if (messageText != null && !messageText.equals("")) {
            mLastMessage = messageText;
        }

        TalkClientUpload upload = null;
        ArrayList<TalkClientUpload> uploads = new ArrayList<TalkClientUpload>();
        if (mAttachments != null) {
            for (IContentObject attachment : mAttachments) {
                uploads.add(SelectedContent.createAttachmentUpload(attachment));
            }
        }

        if (isBlocked()) {
            Toast.makeText(getXoActivity(), R.string.error_send_message_blocked, Toast.LENGTH_LONG).show();
            TalkClientMessage message = getXoClient().composeClientMessage(mContact, messageText, upload);
            getXoClient().markMessageAsAborted(message);
            clearComposedMessage();
            return;

        } else if (isEmptyGroup()) {
            showAlertSendMessageNotPossible();
            return;
        }
        sendComposedMessage(messageText, uploads);
    }

    private void processMessage() {
        if (mAttachments != null && !mAttachments.isEmpty()) {
            if (uploadExceedsTransferLimit(mAttachments)) {
                String alertTitle = getString(R.string.attachment_over_limit_title);
                String fileSize = Formatter.formatShortFileSize(getXoActivity(), calculateAttachmentSize(mAttachments));
                String alertMessage = getString(R.string.attachment_over_limit_upload_question, fileSize);
                showAlertTransferLimitExceeded(alertTitle, alertMessage);
                return;
            }
        }
        validateAndSendComposedMessage();
    }

    private void sendComposedMessage(String messageText, List<TalkClientUpload> uploads) {
        List<TalkClientMessage> messages = getXoClient().composeClientMessageWithMultipleAttachments(mContact, messageText, uploads);
        List<String> messageTags = new ArrayList<String>();

        for (TalkClientMessage message : messages) {
            messageTags.add(message.getMessageTag());
        }
        getXoClient().sendMessages(messageTags);

        clearComposedMessage();
    }

    private void showAlertSendMessageNotPossible() {
        XoDialogs.showOkDialog("EmptyGroupDialog",
                R.string.dialog_empty_group_title,
                R.string.dialog_empty_group_message,
                getXoActivity(),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int index) {
                    }
                }
        );
    }

    private void showAlertTransferLimitExceeded(String alertTitle, String alertMessage) {
        XoDialogs.showPositiveNegativeDialog("TransferLimitExceededDialog",
                alertTitle,
                alertMessage,
                getXoActivity(),
                R.string.attachment_over_limit_confirm,
                R.string.common_no,
                new SendOversizeAttachmentCallbackHandler(),
                null);
    }

    private void encodeImageAttachment(IContentObject contentObject) {
        String dataUri = contentObject.getContentDataUrl();
        if (dataUri.startsWith("file://")) {
            dataUri = dataUri.substring(7);
        }

        final File inBitmap = new File(dataUri);
        final File outBitmap = new File(XoApplication.getAttachmentDirectory(), "resized_"+inBitmap.getName());
        final Bitmap.CompressFormat format = getXoClient().getUploadLimit() != -1 ? Bitmap.CompressFormat.JPEG :
                Bitmap.CompressFormat.PNG;

        SelectedContent newContent = new SelectedContent(contentObject.getContentUrl(), outBitmap.toURI().getPath());
        newContent.setFileName(inBitmap.getName());
        newContent.setContentMediaType(contentObject.getContentMediaType());
        newContent.setContentType("image/"+format.name().toLowerCase());
        newContent.setContentLength(contentObject.getContentLength());
        newContent.setContentAspectRatio(contentObject.getContentAspectRatio());

        int rotation = ImageContentHelper.retrieveOrientation(getActivity(), null, outBitmap.getPath());

        ImageContentHelper.encodeBitmap(inBitmap, outBitmap, getXoClient().getImageUploadMaxPixelCount(),
                getXoClient().getImageUploadEncodingQuality(), format, rotation,
                new ImageEncodingCallback(newContent, outBitmap),
                new ImageEncodingErrorCallback());
    }

    @Override
    public boolean onLongClick(View v) {
        boolean longPressHandled = false;
        if (mLastMessage != null && !mLastMessage.equals("")) {
            for (int i = 0; i < STRESS_TEST_MESSAGE_COUNT; i++) {
                getXoClient().sendMessage(getXoClient().composeClientMessage(mContact, mLastMessage + " " + Integer.toString(i)).getMessageTag());
            }
            longPressHandled = true;
            clearComposedMessage();
        }
        return longPressHandled;
    }

    @Override
    public void onMotionGesture(int pType) {
        String gestureName = Gestures.GESTURE_NAMES.get(pType);
        LOG.debug("Received gesture of type: " + gestureName);

        if (isComposed()) {
            XoApplication.getXoSoundPool().playThrowSound();
            processMessage();
        }
    }

    private class AddAttachmentOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            selectAttachment();
        }
    }

    private class AttachmentOnClickListener implements View.OnClickListener, DialogInterface.OnClickListener {

        @Override
        public void onClick(View v) {
            // start dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(getXoActivity());
            builder.setTitle(R.string.dialog_attachment_title);
            builder.setItems(R.array.dialog_attachment_choose, this);
            builder.setCancelable(false);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case 0:
                    clearAttachment();
                    selectAttachment();
                    break;
                case 1:
                    clearAttachment();
                    break;
            }
        }
    }

    private class SendOversizeAttachmentCallbackHandler implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {

            validateAndSendComposedMessage();
        }
    }

    private class ImageEncodingCallback implements Runnable {

        private SelectedContent mEncodedContent;
        private File mEncodedFile;

        ImageEncodingCallback(SelectedContent contentObject, File encodedFile) {
            mEncodedContent = contentObject;
            mEncodedFile = encodedFile;
        }

        @Override
        public void run() {
            mEncodedContent.setContentLength((int) mEncodedFile.length());
            setAttachment(SelectedContent.createAttachmentUpload(mEncodedContent));
            mSendButton.setEnabled(isComposed());
        }
    }

    private class ImageEncodingErrorCallback implements Runnable {

        @Override
        public void run() {
            // TODO error handling - really important
            LOG.error("Error encoding bitmap for upload.");
            Toast.makeText(getActivity(), R.string.attachment_encoding_error, Toast.LENGTH_LONG).show();
        }
    }
}
