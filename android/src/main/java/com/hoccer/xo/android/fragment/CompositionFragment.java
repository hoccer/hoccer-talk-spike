package com.hoccer.xo.android.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
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
import com.hoccer.talk.content.IContentObject;
import com.hoccer.talk.model.TalkRelationship;
import com.hoccer.xo.android.XoConfiguration;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.base.XoFragment;
import com.hoccer.xo.android.content.ContentMediaTypes;
import com.hoccer.xo.android.content.SelectedContent;
import com.hoccer.xo.android.gesture.Gestures;
import com.hoccer.xo.android.gesture.MotionGestureListener;
import com.hoccer.xo.android.gesture.MotionInterpreter;
import com.hoccer.xo.release.R;

public class CompositionFragment extends XoFragment implements View.OnClickListener,
        View.OnLongClickListener, MotionGestureListener {

    private static final int STRESS_TEST_MESSAGE_COUNT = 15;

    private MotionInterpreter mMotionInterpreter;

    private EditText mTextEdit;

    private TextWatcher mTextWatcher;

    private ImageButton mSendButton;

    private IContentObject mAttachment;

    private TalkClientContact mContact;

    private String mLastMessage = null;

    private ImageButton mAddAttachmentButton;

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
        if (XoConfiguration.DEVELOPMENT_MODE_ENABLED) {
            mSendButton.setOnLongClickListener(this);
            mSendButton.setLongClickable(true);
        }

        mAddAttachmentButton = (ImageButton) v.findViewById(R.id.btn_messaging_composer_add_attachment);
        mAddAttachmentButton.setOnClickListener(new AddAttachmentOnClickListener());

        mMotionInterpreter = new MotionInterpreter(Gestures.Transaction.SHARE, getXoActivity(), this);

        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_composition, menu);
    }

    @Override
    public void onResume() {
        super.onResume();

        mTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateSendButton();
            }
        };
        mTextEdit.addTextChangedListener(mTextWatcher);

        configureMotionInterpreterForContact(mContact);
        updateSendButton();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mTextWatcher != null) {
            mTextEdit.removeTextChangedListener(mTextWatcher);
            mTextWatcher = null;
        }
        mMotionInterpreter.deactivate();
    }

    @Override
    public void onClick(View v) {
        sendComposedMessage();
    }

    @Override
    public void onAttachmentSelected(IContentObject contentObject) {
        LOG.debug("onAttachmentSelected(" + contentObject.getContentDataUrl() + ")");
        showAttachment(contentObject);
        mSendButton.setEnabled(isComposed());
    }

    private void showAttachment(IContentObject contentObject) {
        mAddAttachmentButton.setOnClickListener(new AttachmentOnClickListener());
        mAttachment = contentObject;
        String mediaType = contentObject.getContentMediaType();

        int imageResource = -1;
        if (mediaType != null) {
            if (mediaType.equals(ContentMediaTypes.MediaTypeImage)) {
                imageResource = R.drawable.ic_dark_image;
            } else if (mediaType.equals(ContentMediaTypes.MediaTypeVideo)) {
                imageResource = R.drawable.ic_dark_video;
            } else if (mediaType.equals(ContentMediaTypes.MediaTypeVCard)) {
                imageResource = R.drawable.ic_dark_contact;
            } else if (mediaType.equals(ContentMediaTypes.MediaTypeGeolocation)) {
                imageResource = R.drawable.ic_dark_location;
            } else if (mediaType.equals(ContentMediaTypes.MediaTypeData)) {
                imageResource = R.drawable.ic_dark_data;
            } else if (mediaType.equals(ContentMediaTypes.MediaTypeAudio)) {
                imageResource = R.drawable.ic_dark_music;
            }
        } else {
            imageResource = android.R.drawable.stat_notify_error;
        }
        mAddAttachmentButton.setImageResource(imageResource);
    }

    public void setContact(TalkClientContact contact) {
        LOG.debug("setContact(" + contact.getClientContactId() + ")");
        mContact = contact;
        configureMotionInterpreterForContact(mContact);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTextEdit.setVisibility(View.VISIBLE);
                mSendButton.setVisibility(View.VISIBLE);
                mAddAttachmentButton.setVisibility(View.VISIBLE);

                mTextEdit.setEnabled(true);
                mSendButton.setEnabled(true);
                mAddAttachmentButton.setEnabled(true);
            }
        });
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
        mAddAttachmentButton.setImageResource(R.drawable.ic_light_content_attachment);
        mAttachment = null;
        updateSendButton();
    }

    private void updateSendButton() {
        boolean enabled = (isComposed() || (XoConfiguration.DEVELOPMENT_MODE_ENABLED && mLastMessage != null));
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
        if (mContact.isGroup() && mContact.getGroupMemberships() != null && mContact.getGroupMemberships().size() <= 1) {
            return true;
        }
        return false;
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
            Toast.makeText(getXoActivity(), R.string.error_send_message_empty_group, Toast.LENGTH_LONG).show();
            return;
        }

        String messageText = mTextEdit.getText().toString();

        if (messageText != null && !messageText.equals("")) {
            mLastMessage = messageText;
        }

        TalkClientUpload upload = null;
        if (mAttachment != null) {
            upload = SelectedContent.createAttachmentUpload(mAttachment);
        }

        if (isAborted) {
            TalkClientMessage message = getXoClient().composeClientMessage(mContact, messageText, upload);
            getXoClient().markMessagesAsAborted(message);
        } else {
            getXoClient().sendMessage(getXoClient().composeClientMessage(mContact, messageText, upload).getMessageTag());
        }
        clearComposedMessage();
    }

    private void configureMotionInterpreterForContact(TalkClientContact contact) {
        // react on gestures only when contact is nearby
        if (contact != null && (contact.isNearby() || (contact.isGroup() && contact.getGroupPresence().isTypeNearby()))) {
            mMotionInterpreter.activate();
        } else {
            mMotionInterpreter.deactivate();
        }
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
            getXoSoundPool().playThrowSound();
            sendComposedMessage();
        }
    }

    private class AddAttachmentOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            getXoActivity().selectAttachment();
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
                    getXoActivity().selectAttachment();
                    break;
                case 1:
                    clearAttachment();
                    break;
            }
        }
    }
}
