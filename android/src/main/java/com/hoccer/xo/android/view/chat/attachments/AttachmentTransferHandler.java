package com.hoccer.xo.android.view.chat.attachments;


import android.content.res.Resources;
import android.text.format.Formatter;
import android.view.View;
import com.hoccer.talk.client.IXoTransferListener;
import com.hoccer.talk.client.model.IXoTransferState;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.content.ContentState;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.xo.android.XoApplication;
import com.artcom.hoccer.R;
import org.apache.log4j.Logger;

/**
 * This class handles interactions with an AttachmentTransferControlView.
 * <p/>
 * It receives click events and pauses / resumes the transfer of the given attachment.
 */
public class AttachmentTransferHandler implements View.OnClickListener, IXoTransferListener {

    private static final Logger LOG = Logger.getLogger(AttachmentTransferHandler.class);

    public enum TransferAction {
        NONE,
        REQUEST_DOWNLOAD,
        CANCEL_DOWNLOAD,
        REQUEST_UPLOAD,
        CANCEL_UPLOAD,
    }

    private final AttachmentTransferListener mListener;
    private final AttachmentTransferControlView mTransferControl;
    private TransferAction mTransferAction = TransferAction.NONE;

    private final IContentObject mContent;

    public AttachmentTransferHandler(AttachmentTransferControlView transferProgress, IContentObject content, AttachmentTransferListener listener) {
        mListener = listener;
        mTransferControl = transferProgress;
        mContent = content;
        setTransferAction(mContent.getContentState());
    }

    @Override
    public void onClick(View v) {
        if (v == mTransferControl) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    setTransferAction(mContent.getContentState());
                    switch (mTransferAction) {
                        case REQUEST_DOWNLOAD:
                            if (mContent instanceof TalkClientDownload) {
                                LOG.debug("Will resume download for " + ((TalkClientDownload) mContent).getDownloadUrl());
                                TalkClientDownload download = (TalkClientDownload) mContent;
                                XoApplication.getXoClient().requestDownload(download, true);
                            }
                            break;
                        case CANCEL_DOWNLOAD:
                            if (mContent instanceof TalkClientDownload) {
                                LOG.debug("Will pause download for " + ((TalkClientDownload) mContent).getDownloadUrl());
                                TalkClientDownload download = (TalkClientDownload) mContent;
                                XoApplication.getXoClient().getTransferAgent().pauseDownload(download);
                            }
                            break;
                        case REQUEST_UPLOAD:
                            if (mContent instanceof TalkClientUpload) {
                                LOG.debug("Will resume upload for " + ((TalkClientUpload) mContent).getUploadUrl());
                                TalkClientUpload upload = (TalkClientUpload) mContent;
                                XoApplication.getXoClient().getTransferAgent().startOrRestartUpload(upload);
                            }
                            break;
                        case CANCEL_UPLOAD:
                            if (mContent instanceof TalkClientUpload) {
                                LOG.debug("Will pause upload for " + ((TalkClientUpload) mContent).getUploadUrl());
                                TalkClientUpload upload = (TalkClientUpload) mContent;
                                XoApplication.getXoClient().getTransferAgent().pauseUpload(upload);
                            }
                    }
                    mTransferControl.post(new Runnable() {
                        @Override
                        public void run() {
                            mTransferControl.invalidate();
                        }
                    });
                }
            }).start();
        }
    }

    private void setTransferAction(ContentState state) {
        mTransferControl.setEnabled(true);
        mTransferAction = TransferAction.NONE;
        switch (state) {
            case DOWNLOAD_NEW:
            case DOWNLOAD_PAUSED:
            case DOWNLOAD_ON_HOLD:
                mTransferAction = TransferAction.REQUEST_DOWNLOAD;
                break;
            case DOWNLOAD_DETECTING:
                //mContentTransferControl.setEnabled(false); // TODO: is this needed / balanced?
                break;
            case DOWNLOAD_DECRYPTING:
            case DOWNLOAD_DOWNLOADING:
                mTransferAction = TransferAction.CANCEL_DOWNLOAD;
                break;
            case UPLOAD_NEW:
            case UPLOAD_PAUSED:
                mTransferAction = TransferAction.REQUEST_UPLOAD;
                break;
            case UPLOAD_REGISTERING:
                //mContentTransferControl.setEnabled(false); // TODO: is this needed / balanced?
                break;
            case UPLOAD_ENCRYPTING:
            case UPLOAD_UPLOADING:
                mTransferAction = TransferAction.CANCEL_UPLOAD;
                break;
            case SELECTED:
                break;
            case UPLOAD_FAILED:
                break;
            case UPLOAD_COMPLETE:
                break;
            case DOWNLOAD_FAILED:
                break;
            case DOWNLOAD_COMPLETE:
                break;
            default:
                break;
        }

        updateTransferControl();
    }

    /**
     * Updates the AttachmentTransferControlView from a given IContentObject
     * <p/>
     * TODO: move this into the AttachmentTransferControlView class.
     */
    protected void updateTransferControl() {
        mTransferControl.post(new Runnable() {
            @Override
            public void run() {
                int length = 0;
                int progress = 0;
                Resources res = mTransferControl.getResources();
                ContentState contentState = mContent.getContentState();
                String fileSize;
                switch (contentState) {
                    case DOWNLOAD_DETECTING:
                        break;

                    case DOWNLOAD_NEW:
                        mTransferControl.setVisibility(View.VISIBLE);
                        mTransferControl.prepareToDownload();
                        mTransferControl.setText(res.getString(R.string.transfer_state_pause));
                        mTransferControl.pause();
                        break;

                    case DOWNLOAD_PAUSED:
                        length = mContent.getTransferLength();
                        progress = mContent.getTransferProgress();
                        mTransferControl.setMax(length);
                        mTransferControl.setProgressImmediately(progress);
                        mTransferControl.setText(res.getString(R.string.transfer_state_pause));
                        mTransferControl.prepareToDownload();
                        mTransferControl.pause();
                        break;

                    case DOWNLOAD_ON_HOLD:
                        length = mContent.getTransferLength();
                        progress = mContent.getTransferProgress();
                        mTransferControl.setMax(length);
                        mTransferControl.setProgressImmediately(progress);
                        TalkClientDownload download = (TalkClientDownload)mContent;
                        fileSize = Formatter.formatShortFileSize(mTransferControl.getContext(), download.getTransmittedContentLength());
                        mTransferControl.setText(res.getString(R.string.attachment_on_hold_download_question, fileSize));
                        mTransferControl.prepareToDownload();
                        mTransferControl.pause();
                        break;

                    case DOWNLOAD_DOWNLOADING:
                        length = mContent.getTransferLength();
                        progress = mContent.getTransferProgress();
                        if (length == 0 || progress == 0) {
                            length = 360;
//                            progress = 18;
                        }
                        mTransferControl.prepareToDownload();
                        mTransferControl.setText(res.getString(R.string.transfer_state_downloading));
                        mTransferControl.setMax(length);
                        mTransferControl.setProgressImmediately(progress);
                        break;

                    case DOWNLOAD_DECRYPTING:
                        mTransferControl.setText(res.getString(R.string.transfer_state_decrypting));
                        mTransferControl.spin();
//                mWaitUntilOperationIsFinished = true;
                        break;

                    case DOWNLOAD_COMPLETE:
                        mTransferControl.finishSpinningAndProceed();
                        mListener.onAttachmentTransferComplete(mContent);
                        break;

                    case UPLOAD_REGISTERING:
                        break;

                    case UPLOAD_NEW:
                        mTransferControl.prepareToUpload();
                        mTransferControl.setText(res.getString(R.string.transfer_state_encrypting));
                        mTransferControl.setVisibility(View.VISIBLE);
                        break;

                    case UPLOAD_ENCRYPTING:
                        mTransferControl.prepareToUpload();
                        mTransferControl.setText(res.getString(R.string.transfer_state_encrypting));
                        mTransferControl.setVisibility(View.VISIBLE);
                        mTransferControl.spin();
                        break;

                    case UPLOAD_PAUSED:
                        length = mContent.getTransferLength();
                        progress = mContent.getTransferProgress();
                        mTransferControl.setMax(length);
                        mTransferControl.setProgressImmediately(progress);
                        mTransferControl.setText(res.getString(R.string.transfer_state_pause));
                        mTransferControl.pause();
                        break;

                    case UPLOAD_UPLOADING:
                        mTransferControl.finishSpinningAndProceed();
                        mTransferControl.setText(res.getString(R.string.transfer_state_uploading));
                        //mWaitUntilOperationIsFinished = true;
                        length = mContent.getTransferLength();
                        progress = mContent.getTransferProgress();
                        mTransferControl.setMax(length);
                        mTransferControl.setProgressImmediately(progress);
                        break;

                    case UPLOAD_COMPLETE:
                        mTransferControl.completeAndGone();
                        mListener.onAttachmentTransferComplete(mContent);
                        break;

                    default:
                        mTransferControl.setVisibility(View.GONE);
                        mListener.onAttachmentTransferComplete(mContent);
                        break;
                }

            }
        });
    }

    @Override
    public void onStateChanged(IXoTransferState state) {
        LOG.debug("transfer state changed to " + state.toString() + ". Update ControlView");
        setTransferAction(mContent.getContentState());
    }

    @Override
    public void onProgressUpdated(int progress, int max) {
        setTransferAction(mContent.getContentState());
        mTransferControl.setMax(max);
    }

    @Override
    public void onProgress(int progress) {

    }
}
