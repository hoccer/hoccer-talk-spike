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
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.talk.model.TalkRelationship;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.XoConfiguration;
import com.hoccer.xo.android.XoDialogs;
import com.hoccer.xo.android.base.XoFragment;
import com.hoccer.xo.android.content.SelectedContent;
import com.hoccer.xo.android.gesture.Gestures;
import com.hoccer.xo.android.gesture.MotionGestureListener;
import com.hoccer.xo.android.util.ColorSchemeManager;
import com.hoccer.xo.release.R;

import java.sql.SQLException;

public class CompositionFragment extends XoFragment implements View.OnClickListener,
        View.OnLongClickListener, MotionGestureListener {

    public static final String ARG_CLIENT_CONTACT_ID = "com.hoccer.xo.android.fragment.ARG_CLIENT_CONTACT_ID";

    private static final int STRESS_TEST_MESSAGE_COUNT = 15;

    private EditText mTextEdit;

    private TextWatcher mTextWatcher;

    private ImageButton mSendButton;

    private IContentObject mAttachment;

    private TalkClientContact mContact;

    private String mLastMessage = null;

    private ImageButton mAddAttachmentButton;

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

        View view = inflater.inflate(R.layout.fragment_composition, container, false);

        setHasOptionsMenu(true);

        mTextEdit = (EditText) view.findViewById(R.id.messaging_composer_text);
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

        mSendButton = (ImageButton) view.findViewById(R.id.btn_messaging_composer_send);
        mSendButton.setEnabled(false || XoConfiguration.DEVELOPMENT_MODE_ENABLED);
        mSendButton.setOnClickListener(this);
        if (XoConfiguration.DEVELOPMENT_MODE_ENABLED) {
            mSendButton.setOnLongClickListener(this);
            mSendButton.setLongClickable(true);
        }

        mAddAttachmentButton = (ImageButton) view
                .findViewById(R.id.btn_messaging_composer_add_attachment);
        mAddAttachmentButton.setOnClickListener(new AddAttachmentOnClickListener());

        return view;
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
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                boolean enable = isComposed() || s.toString().length() > 0;
                mSendButton.setEnabled(enable || XoConfiguration.DEVELOPMENT_MODE_ENABLED);
            }
        };
        mTextEdit.addTextChangedListener(mTextWatcher);
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
        if(mediaType != null) {
            if(mediaType.equals(ContentMediaType.IMAGE)) {
                imageResource = R.drawable.ic_light_image;
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

    public void updateContact(TalkClientContact contact) {
        mContact = contact;
    }

    private boolean isComposed() {
        return mTextEdit.getText().length() > 0 || mAttachment != null;
    }

    private void clearComposedMessage() {
        mTextEdit.setText(null);
        mSendButton.setEnabled(false || XoConfiguration.DEVELOPMENT_MODE_ENABLED);
        clearAttachment();
    }

    private void clearAttachment() {
        mAddAttachmentButton.setOnClickListener(new AddAttachmentOnClickListener());
        mAddAttachmentButton.setBackgroundDrawable(null);
        mAddAttachmentButton.setImageResource(R.drawable.ic_light_content_attachment);
        mAttachment = null;
    }

    private boolean isBlocked() {
        if (!mContact.isGroup() && !mContact.isNearby()) {
            TalkRelationship clientRelationship = mContact.getClientRelationship();
            if (clientRelationship != null && clientRelationship.getState()
                    .equals(TalkRelationship.STATE_BLOCKED)) {
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
            getXoClient()
                    .sendMessage(getXoClient().composeClientMessage(mContact, messageText, upload).getMessageTag());
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
                });
    }

    @Override
    public boolean onLongClick(View v) {
        boolean longpressHandled = false;
        if (mLastMessage != null && !mLastMessage.equals("")) {
            for (int i = 0; i < STRESS_TEST_MESSAGE_COUNT; i++) {
                getXoClient().sendMessage(getXoClient()
                        .composeClientMessage(mContact, mLastMessage + " " + Integer.toString(i)).getMessageTag());
            }
            longpressHandled = true;
            clearComposedMessage();
        }
        return longpressHandled;
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
