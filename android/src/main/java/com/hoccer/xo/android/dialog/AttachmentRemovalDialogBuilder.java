package com.hoccer.xo.android.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.xo.android.content.AudioAttachmentItem;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by nico on 22/07/2014.
 */
public class AttachmentRemovalDialogBuilder extends AlertDialog.Builder {

    private static final Logger LOG = Logger.getLogger(AttachmentRemovalDialogBuilder.class);

    private boolean mIsCollection = false;
    private int mCollectionId;
    private boolean mRemoveFromCollection = false;
    private List<IContentObject> mAttachments;

    private DeleteCallback mDeleteCallback;
    private RemoveFromCollectionCallback mRemoveFromCollectionCallback;

    public AttachmentRemovalDialogBuilder(Context context, List<IContentObject> attachments) {
        this(context, attachments, false, -1);
    }

    public AttachmentRemovalDialogBuilder(Context context, List<IContentObject> attachments, boolean isCollection, int collectionId) {
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

    public void setRemoveFromCollection(boolean removeFromCollection) {
        mRemoveFromCollection = removeFromCollection;
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
                AttachmentRemovalDialogBuilder builder = new AttachmentRemovalDialogBuilder(getContext(), mAttachments);

                if (mRemoveFromCollection) {
                    builder.mRemoveFromCollectionCallback = mRemoveFromCollectionCallback;
                    builder.mRemoveFromCollection = true;
                    builder.mCollectionId = mCollectionId;
                } else {
                    builder.mDeleteCallback = mDeleteCallback;
                }

                builder.create().show();
            }
        });

        return this;
    }

    private AttachmentRemovalDialogBuilder setupConfirmDeletionDialog() {
        setMessage(R.string.attachment_confirm_delete_dialog_message);
        setPositiveButton(R.string.common_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (mRemoveFromCollection) {
                    if (mRemoveFromCollectionCallback != null) {
                        mRemoveFromCollectionCallback.removeAttachmentsFromCollection(mAttachments, mCollectionId);
                    }
                } else {
                    if (mDeleteCallback != null) {
                        mDeleteCallback.deleteAttachments(mAttachments);
                    }
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
        public void deleteAttachments(List<IContentObject> attachments);
    }

    public interface RemoveFromCollectionCallback {
        public void removeAttachmentsFromCollection(List<IContentObject> attachments, int collectionId);
    }

}
