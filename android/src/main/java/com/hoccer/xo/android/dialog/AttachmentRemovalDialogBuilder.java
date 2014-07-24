package com.hoccer.xo.android.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import com.hoccer.xo.android.content.AudioAttachmentItem;
import com.hoccer.xo.release.R;

import java.util.List;

/**
 * Created by nico on 22/07/2014.
 */
public class AttachmentRemovalDialogBuilder extends AlertDialog.Builder {

    private boolean mIsCollection = false;
    private int mCollectionId;
    private boolean mRemoveFromCollection = false;
    private List<AudioAttachmentItem> mAttachments;

    private DeleteCallback mDeleteCallback;
    private RemoveFromCollectionCallback mRemoveFromCollectionCallback;

    public AttachmentRemovalDialogBuilder(Context context, List<AudioAttachmentItem> attachments) {
        this(context, attachments, false, -1);
    }

    public AttachmentRemovalDialogBuilder(Context context, List<AudioAttachmentItem> attachments, boolean isCollection, int collectionId) {
        super(context);
        mIsCollection = isCollection;
        mCollectionId = collectionId;
        mAttachments = attachments;

    }

    @Override
    public AlertDialog create() {
        if (mIsCollection && mCollectionId >= 0) {
            setupRemoveFromCollectionDialog();
        } else {
            setupConfirmDeletionDialog();
        }

        return super.create();
    }

    public void setDeleteCallbackHandler(DeleteCallback callbackHandler) {
        mDeleteCallback = callbackHandler;
    }

    public void setRemoveFromCollectionCallbackHandler(RemoveFromCollectionCallback callbackHandler) {
        mRemoveFromCollectionCallback = callbackHandler;
    }

    private AttachmentRemovalDialogBuilder setupRemoveFromCollectionDialog() {

        setSingleChoiceItems(R.array.delete_options, 0, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    mRemoveFromCollection = true;
                } else {
                    mRemoveFromCollection = false;
                }
            }
        });
        setPositiveButton(R.string.common_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (mRemoveFromCollection && mRemoveFromCollectionCallback != null) {
                    mRemoveFromCollectionCallback.removeAttachmentsFromCollection(mAttachments, mCollectionId);
                } else {
                    AttachmentRemovalDialogBuilder builder = new AttachmentRemovalDialogBuilder(getContext(),mAttachments, true, mCollectionId);
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                }
            }
        });

        return this;
    }

    private AttachmentRemovalDialogBuilder setupConfirmDeletionDialog() {
        setMessage(R.string.attachment_confirm_delete_dialog_message);
        setPositiveButton(R.string.common_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (mRemoveFromCollection && mRemoveFromCollectionCallback != null) {
                    mRemoveFromCollectionCallback.removeAttachmentsFromCollection(mAttachments, mCollectionId);
                } else if (mDeleteCallback != null) {
                    mDeleteCallback.deleteAttachments(mAttachments);
                }
            }
        });

        setNegativeButton(R.string.common_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });

        return this;
    }

    public interface DeleteCallback {
        public void deleteAttachments(List<AudioAttachmentItem> attachments);
    }

    public interface RemoveFromCollectionCallback {
        public void removeAttachmentsFromCollection(List<AudioAttachmentItem> attachments, int collectionId);
    }
}
