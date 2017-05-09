package com.hoccer.xo.android.fragment;

import android.content.*;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.Formatter;
import android.util.Base64;
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
import com.hoccer.talk.content.SelectedContent;
import com.hoccer.talk.model.TalkGroupMembership;
import com.hoccer.xo.android.BackgroundManager;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.content.selector.MultiImageSelector;
import com.hoccer.xo.android.content.selector.ClipboardSelector;
import com.hoccer.xo.android.content.selector.IContentSelector;
import com.hoccer.xo.android.dialog.ActivityNotFoundDialog;
import com.hoccer.xo.android.dialog.ContentSelectionDialogFragment;
import com.hoccer.xo.android.gesture.Gestures;
import com.hoccer.xo.android.gesture.MotionGestureListener;
import com.hoccer.xo.android.util.ImageUtils;
import com.hoccer.xo.android.util.UriUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import static android.app.Activity.RESULT_OK;
import static com.hoccer.xo.android.dialog.ContentSelectionDialogFragment.ACTIVITY_NOT_FOUND_DIALOG_TAG;
import static com.hoccer.xo.android.dialog.ContentSelectionDialogFragment.ATTACHMENT_SELECTION_DIALOG_TAG;

public class CompositionFragment extends Fragment implements MotionGestureListener, ContentSelectionDialogFragment.OnAttachmentSelectedListener {

    private static final Logger LOG = Logger.getLogger(CompositionFragment.class);

    public static final String ARG_CLIENT_CONTACT_ID = "com.hoccer.xo.android.fragment.ARG_CLIENT_CONTACT_ID";
    public static final int REQUEST_SELECT_ATTACHMENT = 42;
    private static final int STRESS_TEST_MESSAGE_COUNT = 15;

    private static final String KEY_COMPOSITION_TEXT = "composition_text:";

    private static final String KEY_ATTACHMENTS = "composition_attachment:";

    private static final String SHARED_PREFERENCES = "chats";
    private IContentSelector mContentSelector;

    private enum AttachmentSelectionType {
        NONE,
        IMAGE,
        VIDEO,
        AUDIO,
        CONTACT,
        LOCATION,
        FILE,
        MULTIPLE,
        ERROR
    }

    private TextFieldWatcher mTextFieldWatcher;
    private TextView mAttachmentButtonText;
    private EditText mTextField;
    private ImageButton mSendButton;

    private EnumMap<AttachmentSelectionType, View> mAttachmentTypeViews;
    private ArrayList<SelectedContent> mSelectedContent = new ArrayList<SelectedContent>();
    private TalkClientContact mContact;
    private String mLastMessage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            try {
                int clientContactId = getArguments().getInt(ARG_CLIENT_CONTACT_ID);
                mContact = XoApplication.get().getClient().getDatabase().findContactById(clientContactId);
            } catch (SQLException e) {
                LOG.error("sql error", e);
            }
        }

        if (mContact == null) {
            throw new IllegalArgumentException("MessagingFragment requires valid contact.");
        }
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

        applyCompositionIfAvailable();
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
        mAttachmentTypeViews.put(AttachmentSelectionType.FILE, view.findViewById(R.id.iv_attachment_data));
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

    private void applyCompositionIfAvailable() {
        String contactId = mContact.isClient() ? mContact.getClientId() : mContact.getGroupId();

        SharedPreferences preferences = getActivity().getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        String text = preferences.getString(KEY_COMPOSITION_TEXT + contactId, "");
        mTextField.setText(text);

        try {
            String string = preferences.getString(KEY_ATTACHMENTS + contactId, null);
            if (string != null) {
                JSONArray attachmentJsonArray = new JSONArray(string);
                int length = attachmentJsonArray.length();
                for (int i = 0; i < length; i++) {
                    try {
                        SelectedContent selectedContent = stringToSelectedContent(attachmentJsonArray.getString(i));
                        mSelectedContent.add(selectedContent);
                    } catch (IOException e) {
                        LOG.error(e.getMessage());
                    } catch (ClassNotFoundException e) {
                        LOG.error(e.getMessage());
                    }
                }
                updateAttachmentButton();
                updateSendButton();
            }
        } catch (JSONException e) {
            LOG.error(e.getMessage());
        }
    }

    private static SelectedContent stringToSelectedContent(String selectedContentString) throws IOException, ClassNotFoundException {
        byte[] data = Base64.decode(selectedContentString, Base64.DEFAULT);
        ObjectInputStream ois = new ObjectInputStream(
                new ByteArrayInputStream(data));
        SelectedContent selectedContent = (SelectedContent) ois.readObject();
        ois.close();
        return selectedContent;
    }

    private static String selectedContentToString(SelectedContent selectedContent) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(selectedContent);
        oos.close();
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
    }

    @Override
    public void onPause() {
        super.onPause();
        mTextField.removeTextChangedListener(mTextFieldWatcher);
        saveComposition();
    }

    private void saveComposition() {
        SharedPreferences preferences = getActivity().getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = preferences.edit();

        String contactId = mContact.isClient() ? mContact.getClientId() : mContact.getGroupId();
        edit.putString(KEY_COMPOSITION_TEXT + contactId, mTextField.getText().toString());
        try {
            JSONArray selectedAttachmentJson = new JSONArray();
            for (SelectedContent content : mSelectedContent) {
                selectedAttachmentJson.put(selectedContentToString(content));
            }
            edit.putString(KEY_ATTACHMENTS + contactId, selectedAttachmentJson.toString());
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
        edit.apply();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == REQUEST_SELECT_ATTACHMENT) {
            mSelectedContent.clear();
            if (resultCode == RESULT_OK) {
                setSelectedContent(intent);
            }
        }
    }

    private void setSelectedContent(Intent intent) {
        if (mContentSelector instanceof MultiImageSelector) {
            try {
                mSelectedContent = MultiImageSelector.createObjectsFromSelectionResult(getActivity(), intent);
            } catch (Exception e) {
                LOG.error("Could not create object from selection result.", e);
            }
        } else {
            SelectedContent content = null;
            try {
                content = mContentSelector.createObjectFromSelectionResult(getActivity(), intent);
            } catch (Exception e) {
                LOG.error("Could not create object from selection result.", e);
            }
            CollectionUtils.addIgnoreNull(mSelectedContent, content);
        }
    }

    @Override
    public void onSelected(IContentSelector contentSelector) {
        mContentSelector = contentSelector;
        if (mContentSelector instanceof ClipboardSelector) {
            setSelectedContent(null);
            updateAttachmentButton();
            updateSendButton();
        } else {
            Intent intent = contentSelector.createSelectionIntent(getActivity());
            startExternalActivityForResult(intent);
        }
    }

    private void startExternalActivityForResult(Intent intent) {
        BackgroundManager.get().ignoreNextBackgroundPhase();
        try {
            startActivityForResult(intent, REQUEST_SELECT_ATTACHMENT);
        } catch (ActivityNotFoundException e) {
            if (Intent.ACTION_GET_CONTENT.equals(intent.getAction())) {
                showActivityNotFoundDialog(intent.getAction());
            } else {
                Toast.makeText(getActivity(), R.string.error_compatible_app_unavailable, Toast.LENGTH_LONG).show();
                LOG.error(e.getMessage());
            }
        }
    }

    private void showActivityNotFoundDialog(String action) {
        Bundle bundle = new Bundle();
        bundle.putString(ActivityNotFoundDialog.ACTION_KEY, action);

        DialogFragment dialog = new ActivityNotFoundDialog();
        dialog.setArguments(bundle);
        dialog.show(getActivity().getSupportFragmentManager(), ACTIVITY_NOT_FOUND_DIALOG_TAG);
    }

    @Override
    public void onMotionGesture(int pType) {
        String gestureName = Gestures.GESTURE_NAMES.get(pType);
        LOG.debug("Received gesture of type: " + gestureName);

        if (isComposed()) {
            XoApplication.getSoundPool().playThrowSound();
            processMessage();
        }
    }

    private void updateSendButton() {
        boolean enabled = (isComposed() || (XoApplication.getConfiguration().isDevelopmentModeEnabled() && mLastMessage != null));
        mSendButton.setEnabled(enabled);
    }

    private boolean isComposed() {
        return !(mSelectedContent.isEmpty() && mTextField.getText().toString().trim().isEmpty());
    }

    private void updateAttachmentButton() {
        if (mSelectedContent.isEmpty()) {
            updateAttachmentButtonImage(AttachmentSelectionType.NONE);
        } else if (mSelectedContent.size() == 1) {
            String mediaType = mSelectedContent.get(0).getMediaType();

            if (ContentMediaType.IMAGE.equals(mediaType)) {
                updateAttachmentButtonImage(AttachmentSelectionType.IMAGE);
            } else if (ContentMediaType.VIDEO.equals(mediaType)) {
                updateAttachmentButtonImage(AttachmentSelectionType.VIDEO);
            } else if (ContentMediaType.VCARD.equals(mediaType)) {
                updateAttachmentButtonImage(AttachmentSelectionType.CONTACT);
            } else if (ContentMediaType.LOCATION.equals(mediaType)) {
                updateAttachmentButtonImage(AttachmentSelectionType.LOCATION);
            } else if (ContentMediaType.FILE.equals(mediaType)) {
                updateAttachmentButtonImage(AttachmentSelectionType.FILE);
            } else if (ContentMediaType.AUDIO.equals(mediaType)) {
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
            mAttachmentButtonText.setText(String.valueOf(mSelectedContent.size()));
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
        mSelectedContent.clear();
        updateAttachmentButton();
    }

    private static boolean isGroupEmpty(TalkClientContact contact) {
        final List<TalkClientContact> otherContactsInGroup;
        try {
            otherContactsInGroup = XoApplication.get().getClient().getDatabase().findContactsInGroupByState(contact.getGroupId(),
                    TalkGroupMembership.STATE_JOINED);
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

        if (mContact.isClientBlocked()) {
            Toast.makeText(getActivity(), R.string.error_send_message_blocked, Toast.LENGTH_LONG).show();
            clearComposedMessage();
            return;
        }

        if (!(mContact.isInEnvironment() || mContact.isClientFriend() || mContact.isGroup())){
            Toast.makeText(getActivity(), R.string.error_client_not_nearby_or_ww, Toast.LENGTH_LONG).show();
            return;
        }

        if (mSelectedContent.isEmpty()) {
            sendMessage();
        } else {
            sendMessageWithAttachments();
        }
    }

    private void sendMessage() {
        String messageText = mTextField.getText().toString();
        TalkClientMessage message = XoApplication.get().getClient().composeClientMessage(mContact, messageText);
        XoApplication.get().getClient().sendMessage(message.getMessageTag());
        clearComposedMessage();
        mLastMessage = messageText;
    }

    private void sendMessageWithAttachments() {
        List<TalkClientUpload> uploads = new ArrayList<TalkClientUpload>(mSelectedContent.size());

        for (SelectedContent content : mSelectedContent) {
            TalkClientUpload upload = new TalkClientUpload();
            upload.initializeAsAttachment(content);
            uploads.add(upload);
        }

        AsyncTask<List<TalkClientUpload>, Void, List<TalkClientUpload>> asyncTask = new AsyncTask<List<TalkClientUpload>, Void, List<TalkClientUpload>>() {
            @Override
            protected List<TalkClientUpload> doInBackground(List<TalkClientUpload>... args) {
                List<TalkClientUpload> uploads = args[0];
                try {
                    if (XoApplication.get().getClient().isEncodingNecessary()) {
                        for (TalkClientUpload upload : uploads) {
                            if (upload.getMediaType().equals(ContentMediaType.IMAGE)) {
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
        String fileSize = Formatter.formatShortFileSize(getActivity(), attachmentSize);
        XoDialogs.showPositiveNegativeDialog("TransferLimitExceededDialog",
                getString(R.string.attachment_over_limit_title),
                getString(R.string.attachment_over_limit_upload_question, fileSize),
                getActivity(),
                R.string.attachment_over_limit_confirm,
                R.string.common_no,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sendMessageWithAttachments(uploads);
                    }
                },
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteCachedFiles(uploads);
                        clearAttachment();
                    }
                });
    }

    private void deleteCachedFiles(List<TalkClientUpload> uploads) {
        for (TalkClientUpload upload : uploads) {
            String path = upload.getTempCompressedFilePath();
            if (path != null) {
                FileUtils.deleteQuietly(new File(path));
            }
        }
    }

    private void sendMessageWithAttachments(List<TalkClientUpload> uploads) {
        String messageText = mTextField.getText().toString();
        List<TalkClientMessage> messages;

        if (uploads.size()==1){
            messages = new ArrayList<>();
            messages.add(XoApplication.get().getClient().composeClientMessage(mContact, messageText, uploads.get(0)));
        } else {
            messages = XoApplication.get().getClient().composeClientMessage(mContact, messageText, uploads);
        }
        List<String> messageTags = new ArrayList<String>();

        for (TalkClientMessage message : messages) {
            messageTags.add(message.getMessageTag());

        }
        XoApplication.get().getClient().sendMessages(messageTags);
        clearComposedMessage();
        mLastMessage = messageText;
    }

    private boolean sizeExceedsUploadLimit(long attachmentSize) {
        int transferLimit = XoApplication.get().getClient().getUploadLimit();
        if (transferLimit == -1) {
            return false;
        }
        if (transferLimit == -2) {
            return true;
        }
        return attachmentSize >= transferLimit;
    }

    private static long calculateAttachmentSize(List<TalkClientUpload> uploads) {
        long fileSize = 0;
        for (TalkClientUpload upload : uploads) {
            fileSize += upload.getContentLength();
        }
        return fileSize;
    }

    private void showAlertGroupIsEmpty() {
        XoDialogs.showOkDialog("EmptyGroupDialog",
                R.string.dialog_empty_group_title,
                R.string.dialog_empty_group_message,
                getActivity(),
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

        Bitmap bitmap = ImageUtils.resizeImageToMaxPixelCount(imageFile, XoApplication.get().getClient().getImageUploadMaxPixelCount());
        if (bitmap == null) {
            throw new IOException("Could not resize image '" + imageFile.getAbsolutePath() + "' to bitmap");
        }

        boolean success = ImageUtils.compressBitmapToFile(bitmap, compressedImageFile, XoApplication.get().getClient().getImageUploadEncodingQuality(),
                Bitmap.CompressFormat.JPEG);
        if (!success) {
            throw new IOException("Could not compress bitmap to '" + compressedImageFile.getAbsolutePath() + "'");
        }

        ImageUtils.copyExifData(imageFile.getAbsolutePath(), compressedImageFile.getAbsolutePath());
        upload.setTempCompressedFilePath(compressedImageFile.getAbsolutePath());
    }

    private void showSelectAttachmentDialog() {
        DialogFragment dialogFragment = new ContentSelectionDialogFragment();
        dialogFragment.setTargetFragment(this, -1);
        dialogFragment.show(getActivity().getSupportFragmentManager(), ATTACHMENT_SELECTION_DIALOG_TAG);
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
            if (mSelectedContent.isEmpty()) {
                showSelectAttachmentDialog();
            } else {
                showEditAttachmentDialog();
            }
        }
    }

    private class SendButtonListener implements View.OnClickListener, View.OnLongClickListener {
        @Override
        public void onClick(View v) {
            if (isComposed()) {
                processMessage();
            }
        }

        @Override
        public boolean onLongClick(View v) {
            boolean longPressHandled = false;
            if (mLastMessage != null) {
                for (int i = 0; i < STRESS_TEST_MESSAGE_COUNT; i++) {
                    XoApplication.get().getClient().sendMessage(XoApplication.get().getClient().composeClientMessage(mContact, mLastMessage + " " + Integer.toString(i)).getMessageTag());
                }
                longPressHandled = true;
                clearComposedMessage();
            }
            return longPressHandled;
        }
    }

    private class TextFieldWatcher implements TextWatcher {
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
    }
}
