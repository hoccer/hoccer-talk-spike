package com.hoccer.xo.android.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
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
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.ArrayList;

public class CompositionFragment extends XoFragment implements View.OnClickListener,
        View.OnLongClickListener, MotionGestureListener {

    private static final Logger LOG = Logger.getLogger(CompositionFragment.class);

    public static final String ARG_CLIENT_CONTACT_ID = "com.hoccer.xo.android.fragment.ARG_CLIENT_CONTACT_ID";
    public static final int REQUEST_SELECT_ATTACHMENT = 42;
    public static final int REQUEST_SELECT_IMAGES_ATTACHMENT = 43;

    private static final int STRESS_TEST_MESSAGE_COUNT = 15;

    private EditText mTextEdit;
    private TextWatcher mTextWatcher;
    private ImageButton mSendButton;
    private IContentObject mAttachment;
    private TalkClientContact mContact;
    private String mLastMessage = null;
    private ImageButton mAddAttachmentButton;

    private ArrayList<IContentObject> mImages;

    private ContentSelection mAttachmentSelection = null;

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
                        sendComposedMessage();
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

        mAddAttachmentButton = (ImageButton) v.findViewById(R.id.btn_messaging_composer_add_attachment);
        mAddAttachmentButton.setOnClickListener(new AddAttachmentOnClickListener());

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
            } else if (requestCode == REQUEST_SELECT_IMAGES_ATTACHMENT) {
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
        sendComposedMessage();
    }

    private void selectAttachment() {
        getXoActivity().setBackgroundActive();
        mAttachmentSelection = ContentRegistry.get(getActivity()).selectAttachment(this, REQUEST_SELECT_ATTACHMENT);
    }

    public void onAttachmentSelected(IContentObject contentObject) {
        setAttachment(contentObject);
        mSendButton.setEnabled(isComposed());
    }

    public void onAttachmentsSelected(ArrayList<IContentObject> contentObjects) {
        LOG.debug("onAttachmentSelected(" + contentObjects.size() + ")");
        showAttachments(null, contentObjects);
        mSendButton.setEnabled(isComposed());
    }

    private void showAttachmentSelectionError() {
        Toast.makeText(getActivity(), R.string.error_attachment_selection, Toast.LENGTH_LONG).show();
    }

    private void setAttachment(IContentObject contentObject) {
        mAttachment = contentObject;
        updateAttachmentButton();
    }

    private void showAttachments(IContentObject contentObject, ArrayList<IContentObject> contentObjects) {
        mAddAttachmentButton.setOnClickListener(new AttachmentOnClickListener());
        if (contentObjects != null) {
            mAttachment = contentObjects.get(0);
            if (contentObjects.size() > 1) {
                mImages = contentObjects;
            }
        } else {
            mAttachment = contentObject;
        }
        String mediaType = mAttachment.getContentMediaType();
        int imageResource = -1;
        if(mediaType != null) {
            if(mediaType.equals(ContentMediaType.IMAGE)) {
                if (mImages != null) {
                    imageResource = R.drawable.ic_light_images;
                } else {
                    imageResource = R.drawable.ic_light_image;
                }
            } else if(mediaType.equals(ContentMediaType.VIDEO)) {
                imageResource = R.drawable.ic_light_video;
            } else if(mediaType.equals(ContentMediaType.VCARD)) {
                imageResource = R.drawable.ic_light_contact;
            } else if(mediaType.equals(ContentMediaType.LOCATION)) {
                imageResource = R.drawable.ic_light_location;
            } else if(mediaType.equals(ContentMediaType.DATA)) {
                imageResource = R.drawable.ic_light_data;
            } else if(mediaType.equals(ContentMediaType.AUDIO)) {
                imageResource = R.drawable.ic_light_video;
            }
        } else {
            imageResource = android.R.drawable.stat_notify_error;
        }

        mAddAttachmentButton.setBackgroundDrawable(ColorSchemeManager.getRepaintedAttachmentDrawable(getXoActivity(), imageResource, true));
        mAddAttachmentButton.setImageResource(android.R.color.transparent);
    }

    private void updateAttachmentButton() {
        mAddAttachmentButton.setOnClickListener(new AttachmentOnClickListener());
        String mediaType = mAttachment.getContentMediaType();

        int imageResource = -1;
        if (mediaType != null) {
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
            }
        } else {
            imageResource = android.R.drawable.stat_notify_error;
        }

        mAddAttachmentButton.setBackgroundDrawable(ColorSchemeManager.getRepaintedAttachmentDrawable(getXoActivity(), imageResource, true));
        mAddAttachmentButton.setImageResource(android.R.color.transparent);
    }

    public void updateContact(TalkClientContact contact) {
        mContact = contact;
    }

    private boolean isComposed() {
        boolean composed = (mTextEdit.getText().length() > 0 || mAttachment != null);
        return composed;
    }

    private void clearComposedMessage() {
        mTextEdit.setText("");
        clearAttachment();
    }

    private void clearAttachment() {
        mAddAttachmentButton.setOnClickListener(new AddAttachmentOnClickListener());
        mAddAttachmentButton.setBackgroundDrawable(null);
        mAddAttachmentButton.setImageResource(R.drawable.ic_light_content_attachment);
        mAttachment = null;
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

    private void sendComposedMessage() {
        if (mContact == null) {
            return;
        }

        boolean isAborted = false;
        if (isBlocked()) {
            Toast.makeText(getXoActivity(), R.string.error_send_message_blocked, Toast.LENGTH_LONG).show();
            isAborted = true;
        } else if (isEmptyGroup()) {
            showAlertSendMessageNotPossible();
            return;
        }

        String messageText = mTextEdit.getText().toString();

        if (messageText != null && !messageText.equals("")) {
            mLastMessage = messageText;
        }

        TalkClientUpload upload = null;
        ArrayList<TalkClientUpload> uploads = new ArrayList<TalkClientUpload>();
        if (mImages != null && mImages.size() > 1) {
            for (IContentObject o: mImages) {
                uploads.add(SelectedContent.createAttachmentUpload(o));
            }
        } else if (mAttachment != null) {
            upload = SelectedContent.createAttachmentUpload(mAttachment);
        }

        if (!uploads.isEmpty()) {
            ArrayList<TalkClientMessage> messages = new ArrayList<TalkClientMessage>();
            if (messageText != null && !messageText.equals("")) {
                messages.add(getXoClient().composeClientMessage(mContact, messageText, null));
            }
            for (TalkClientUpload u: uploads) {
                messages.add(getXoClient().composeClientMessage(mContact, "", u));
            }
            if (isAborted) {
                for (TalkClientMessage m: messages) {
                    getXoClient().markMessagesAsAborted(m);
                }
            } else {
                for (TalkClientMessage m: messages) {
                    getXoClient().sendMessage(m.getMessageTag());
                }
            }
        } else {
            if (isAborted) {
                TalkClientMessage message = getXoClient().composeClientMessage(mContact, messageText, upload);
                getXoClient().markMessagesAsAborted(message);
            } else {
                getXoClient().sendMessage(getXoClient().composeClientMessage(mContact, messageText, upload).getMessageTag());
            }
        }
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
            sendComposedMessage();
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
}
