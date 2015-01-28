package com.hoccer.xo.android.fragment;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.Formatter;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.client.predicates.TalkClientContactPredicates;
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.talk.content.SelectedAttachment;
import com.hoccer.talk.model.TalkGroupMembership;
import com.hoccer.talk.model.TalkRelationship;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.base.XoFragment;
import com.hoccer.xo.android.content.ContentRegistry;
import com.hoccer.xo.android.content.ContentSelection;
import com.hoccer.xo.android.content.MultiImageSelector;
import com.hoccer.xo.android.content.selector.IContentSelector;
import com.hoccer.xo.android.gesture.Gestures;
import com.hoccer.xo.android.gesture.MotionGestureListener;
import com.hoccer.xo.android.util.ImageUtils;
import com.hoccer.xo.android.util.UriUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public class CompositionFragment extends XoFragment implements MotionGestureListener {

    private static final Logger LOG = Logger.getLogger(CompositionFragment.class);

    public static final String ARG_CLIENT_CONTACT_ID = "com.hoccer.xo.android.fragment.ARG_CLIENT_CONTACT_ID";
    public static final int REQUEST_SELECT_ATTACHMENT = 42;
    private static final int STRESS_TEST_MESSAGE_COUNT = 15;

    private enum AttachmentSelectionType {
        NONE,
        IMAGE,
        VIDEO,
        AUDIO,
        CONTACT,
        LOCATION,
        DATA,
        MULTIPLE,
        ERROR
    }

    private TextFieldWatcher mTextFieldWatcher;
    private TextView mAttachmentButtonText;
    private EditText mTextField;
    private ImageButton mSendButton;

    private ContentSelection mAttachmentSelection;
    private EnumMap<AttachmentSelectionType, View> mAttachmentTypeViews;
    private List<SelectedAttachment> mAttachments;
    private TalkClientContact mContact;
    private String mLastMessage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            try {
                int clientContactId = getArguments().getInt(ARG_CLIENT_CONTACT_ID);
                mContact = XoApplication.getXoClient().getDatabase().findContactById(clientContactId);
            } catch (SQLException e) {
                LOG.error("sql error", e);
            }
        }

        if (mContact == null) {
            throw new IllegalArgumentException("MessagingFragment requires valid contact.");
        }

        mAttachments = new ArrayList<SelectedAttachment>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_composition, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeTextField(view);
        initializeSendButton(view);
        initializeAttachmentButton(view);
        initializeAttachmentTypeViews(view);
    }

    private void initializeTextField(View view) {
        mTextField = (EditText) view.findViewById(R.id.messaging_composer_text);
        mTextField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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

        mTextFieldWatcher = new TextFieldWatcher();
    }

    private void initializeSendButton(View view) {
        SendButtonListener sendButtonListener = new SendButtonListener();
        mSendButton = (ImageButton) view.findViewById(R.id.btn_messaging_composer_send);
        mSendButton.setOnClickListener(sendButtonListener);
        mSendButton.setEnabled(false);

        if (XoApplication.getConfiguration().isDevelopmentModeEnabled()) {
            mSendButton.setOnLongClickListener(sendButtonListener);
            mSendButton.setLongClickable(true);
        }
    }

    private void initializeAttachmentButton(View view) {
        RelativeLayout attachmentButton = (RelativeLayout) view.findViewById(R.id.rl_attachment_button);
        attachmentButton.setOnClickListener(new AttachmentButtonListener());
        mAttachmentButtonText = (TextView) view.findViewById(R.id.tv_attachment_count);
    }

    private void initializeAttachmentTypeViews(View view) {
        mAttachmentTypeViews = new EnumMap<AttachmentSelectionType, View>(AttachmentSelectionType.class);
        mAttachmentTypeViews.put(AttachmentSelectionType.NONE, view.findViewById(R.id.iv_attachment_none));
        mAttachmentTypeViews.put(AttachmentSelectionType.IMAGE, view.findViewById(R.id.iv_attachment_image));
        mAttachmentTypeViews.put(AttachmentSelectionType.VIDEO, view.findViewById(R.id.iv_attachment_video));
        mAttachmentTypeViews.put(AttachmentSelectionType.AUDIO, view.findViewById(R.id.iv_attachment_audio));
        mAttachmentTypeViews.put(AttachmentSelectionType.CONTACT, view.findViewById(R.id.iv_attachment_contact));
        mAttachmentTypeViews.put(AttachmentSelectionType.LOCATION, view.findViewById(R.id.iv_attachment_location));
        mAttachmentTypeViews.put(AttachmentSelectionType.DATA, view.findViewById(R.id.iv_attachment_data));
        mAttachmentTypeViews.put(AttachmentSelectionType.MULTIPLE, view.findViewById(R.id.iv_attachment_multiple));
        mAttachmentTypeViews.put(AttachmentSelectionType.ERROR, view.findViewById(R.id.iv_attachment_error));
    }

    @Override
    public void onResume() {
        super.onResume();
        mTextField.addTextChangedListener(mTextFieldWatcher);
        updateAttachmentButton();
        updateSendButton();
    }

    @Override
    public void onPause() {
        super.onPause();
        mTextField.removeTextChangedListener(mTextFieldWatcher);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == REQUEST_SELECT_ATTACHMENT) {
            mAttachments.clear();

            if (resultCode == Activity.RESULT_OK) {
                if (mAttachmentSelection.getSelector() instanceof MultiImageSelector) {
                    MultiImageSelector selector = (MultiImageSelector) mAttachmentSelection.getSelector();
                    mAttachments = selector.createObjectsFromSelectionResult(getActivity(), intent);
                } else {
                    IContentSelector selector = mAttachmentSelection.getSelector();
                    SelectedAttachment attachment = selector.createObjectFromSelectionResult(getActivity(), intent);
                    CollectionUtils.addIgnoreNull(mAttachments, attachment);
                }

                if (mAttachments.isEmpty()) {
                    Toast.makeText(getActivity(), R.string.error_attachment_selection, Toast.LENGTH_LONG).show();
                }
            }

            updateAttachmentButton();
            updateSendButton();
        }
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

    private void updateSendButton() {
        boolean enabled = (isComposed() || (XoApplication.getConfiguration().isDevelopmentModeEnabled() && mLastMessage != null));
        mSendButton.setEnabled(enabled);
    }

    private boolean isComposed() {
        return (mTextField.getText().length() > 0 || mAttachments != null);
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

    private void updateAttachmentButton() {
        if (mAttachments.isEmpty()) {
            updateAttachmentButtonImage(AttachmentSelectionType.NONE);
        } else if (mAttachments.size() == 1) {
            String contentType = mAttachments.get(0).getContentMediaType();

            if (ContentMediaType.IMAGE.equals(contentType)) {
                updateAttachmentButtonImage(AttachmentSelectionType.IMAGE);
            } else if (ContentMediaType.VIDEO.equals(contentType)) {
                updateAttachmentButtonImage(AttachmentSelectionType.VIDEO);
            } else if (ContentMediaType.VCARD.equals(contentType)) {
                updateAttachmentButtonImage(AttachmentSelectionType.CONTACT);
            } else if (ContentMediaType.LOCATION.equals(contentType)) {
                updateAttachmentButtonImage(AttachmentSelectionType.LOCATION);
            } else if (ContentMediaType.DATA.equals(contentType)) {
                updateAttachmentButtonImage(AttachmentSelectionType.DATA);
            } else if (ContentMediaType.AUDIO.equals(contentType)) {
                updateAttachmentButtonImage(AttachmentSelectionType.AUDIO);
            } else {
                updateAttachmentButtonImage(AttachmentSelectionType.ERROR);
            }
        } else {
            updateAttachmentButtonImage(AttachmentSelectionType.MULTIPLE);
        }
    }

    private void updateAttachmentButtonImage(AttachmentSelectionType type) {
        // hide all views first
        for (View attachmentTypeView : mAttachmentTypeViews.values()) {
            attachmentTypeView.setVisibility(View.GONE);
        }

        mAttachmentTypeViews.get(type).setVisibility(View.VISIBLE);

        if (type == AttachmentSelectionType.MULTIPLE) {
            mAttachmentButtonText.setText(String.valueOf(mAttachments.size()));
        } else {
            mAttachmentButtonText.setText("");
        }
    }

    private void clearComposedMessage() {
        mTextField.setText("");
        clearAttachment();
        updateSendButton();
    }

    private void clearAttachment() {
        mAttachments.clear();
        updateAttachmentButton();
    }

    private static boolean isGroupEmpty(TalkClientContact contact) {
        final List<TalkClientContact> otherContactsInGroup;
        try {
            otherContactsInGroup = XoApplication.getXoClient().getDatabase().findContactsInGroupByState(contact.getGroupId(), TalkGroupMembership.STATE_JOINED);
            CollectionUtils.filterInverse(otherContactsInGroup, TalkClientContactPredicates.IS_SELF_PREDICATE);
            return otherContactsInGroup.isEmpty();
        } catch (SQLException e) {
            LOG.error("Error retrieving contacts in group: " + contact.getGroupId(), e);
            return true;
        }
    }

    private void processMessage() {
        if (mContact.isGroup() && isGroupEmpty(mContact)) {
            showAlertGroupIsEmpty();
            return;
        }

        if (isBlocked()) {
            Toast.makeText(getXoActivity(), R.string.error_send_message_blocked, Toast.LENGTH_LONG).show();
            clearComposedMessage();
            return;
        }

        if (mAttachments.isEmpty()) {
            sendMessage();
        } else {
            sendMessageWithAttachments();
        }
    }

    private void sendMessage() {
        String messageText = mTextField.getText().toString();
        TalkClientMessage message = getXoClient().composeClientMessage(mContact, messageText);
        getXoClient().sendMessage(message.getMessageTag());
        clearComposedMessage();
    }

    private void sendMessageWithAttachments() {
        List<TalkClientUpload> uploads = new ArrayList<TalkClientUpload>(mAttachments.size());

        for (SelectedAttachment attachment : mAttachments) {
            TalkClientUpload upload = new TalkClientUpload();
            upload.initializeAsAttachment(attachment);
            uploads.add(upload);
        }

        AsyncTask<List<TalkClientUpload>, Void, List<TalkClientUpload>> asyncTask = new AsyncTask<List<TalkClientUpload>, Void, List<TalkClientUpload>>() {
            @Override
            protected List<TalkClientUpload> doInBackground(List<TalkClientUpload>... args) {
                List<TalkClientUpload> uploads = args[0];
                try {
                    if (getXoClient().isEncodingNecessary()) {
                        for (TalkClientUpload upload : uploads) {
                            if (upload.getContentMediaType().equals(ContentMediaType.IMAGE)) {
                                compressImageAttachment(upload);
                            }
                        }
                    }
                } catch (IOException e) {
                    cancel(false);
                }
                return uploads;
            }

            @Override
            protected void onPostExecute(List<TalkClientUpload> uploads) {
                long attachmentSize = calculateAttachmentSize(uploads);
                if (sizeExceedsUploadLimit(attachmentSize)) {
                    showTransferLimitExceededDialog(attachmentSize, uploads);
                } else {
                    sendMessageWithAttachments(uploads);
                }
            }

            @Override
            protected void onCancelled() {
                LOG.error("Error encoding bitmap(s) for upload.");
                Toast.makeText(getActivity(), R.string.attachment_encoding_error, Toast.LENGTH_LONG).show();
            }
        };
        asyncTask.execute(uploads);
    }

    private void showTransferLimitExceededDialog(long attachmentSize, final List<TalkClientUpload> uploads) {
        String fileSize = Formatter.formatShortFileSize(getXoActivity(), attachmentSize);
        XoDialogs.showPositiveNegativeDialog("TransferLimitExceededDialog",
                getString(R.string.attachment_over_limit_title),
                getString(R.string.attachment_over_limit_upload_question, fileSize),
                getXoActivity(),
                R.string.attachment_over_limit_confirm,
                R.string.common_no,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sendMessageWithAttachments(uploads);
                    }
                },
                null);
    }

    private void sendMessageWithAttachments(List<TalkClientUpload> uploads) {
        String messageText = mTextField.getText().toString();
        List<TalkClientMessage> messages = getXoClient().composeClientMessage(mContact, messageText, uploads);
        List<String> messageTags = new ArrayList<String>();

        for (TalkClientMessage message : messages) {
            messageTags.add(message.getMessageTag());

        }
        getXoClient().sendMessages(messageTags);
        clearComposedMessage();
    }

    private boolean sizeExceedsUploadLimit(long attachmentSize) {
        int transferLimit = getXoClient().getUploadLimit();
        if (transferLimit < 0) {
            if (transferLimit == -1) {
                return false;
            }
            if (transferLimit == -2) {
                return true;
            }
        }
        return attachmentSize >= transferLimit;
    }

    private static long calculateAttachmentSize(List<TalkClientUpload> uploads) {
        long fileSize = 0;
        for (TalkClientUpload upload : uploads) {
            File fileToUpload = new File(upload.getCachedFilePath() != null ? upload.getCachedFilePath() : upload.getFilePath());
            fileSize += fileToUpload.length();
        }
        return fileSize;
    }

    private void showAlertGroupIsEmpty() {
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

    private void compressImageAttachment(TalkClientUpload upload) throws IOException {
        Uri fileUri = UriUtils.getAbsoluteFileUri(upload.getFilePath());
        final File imageFile = new File(fileUri.getPath());
        final File compressedImageFile = new File(XoApplication.getCacheStorage(), imageFile.getName());

        Bitmap bitmap = ImageUtils.resizeImageToMaxPixelCount(imageFile, getXoClient().getImageUploadMaxPixelCount());
        if (bitmap == null) {
            throw new IOException("Could not resize image '" + imageFile.getAbsolutePath() + "' to bitmap");
        }

        boolean success = ImageUtils.compressBitmapToFile(bitmap, compressedImageFile, getXoClient().getImageUploadEncodingQuality(), Bitmap.CompressFormat.JPEG);
        if (!success) {
            throw new IOException("Could not compress bitmap to '" + compressedImageFile.getAbsolutePath() + "'");
        }

        ImageUtils.copyExifData(imageFile.getAbsolutePath(), compressedImageFile.getAbsolutePath());
        upload.setCachedFilePath(compressedImageFile.getAbsolutePath());
    }

    private void showSelectAttachmentDialog() {
        mAttachmentSelection = ContentRegistry.get(getActivity()).selectAttachment(this, REQUEST_SELECT_ATTACHMENT);
    }

    private void showEditAttachmentDialog() {
        XoDialogs.showSingleChoiceDialog(
                "AttachmentResetDialog",
                R.string.dialog_attachment_title,
                getResources().getStringArray(R.array.dialog_attachment_choose),
                getActivity(),
                new XoDialogs.OnSingleSelectionFinishedListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id, int selectedItem) {
                        switch (selectedItem) {
                            case 0:
                                showSelectAttachmentDialog();
                                break;
                            case 1:
                                clearAttachment();
                                break;
                            default:
                                break;
                        }
                    }
                });
    }

    private class AttachmentButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (mAttachments.isEmpty()) {
                showSelectAttachmentDialog();
            } else {
                showEditAttachmentDialog();
            }
        }
    }

    private class SendButtonListener implements View.OnClickListener, View.OnLongClickListener {
        @Override
        public void onClick(View v) {
            processMessage();
        }

        @Override
        public boolean onLongClick(View v) {
            boolean longPressHandled = false;
            if (mLastMessage != null && !mLastMessage.isEmpty()) {
                for (int i = 0; i < STRESS_TEST_MESSAGE_COUNT; i++) {
                    getXoClient().sendMessage(getXoClient().composeClientMessage(mContact, mLastMessage + " " + Integer.toString(i)).getMessageTag());
                }
                longPressHandled = true;
                clearComposedMessage();
            }
            return longPressHandled;
        }
    }

    private class TextFieldWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            updateSendButton();
        }
    }
}
