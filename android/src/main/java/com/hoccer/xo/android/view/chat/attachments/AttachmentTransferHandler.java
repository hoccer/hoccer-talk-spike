package com.hoccer.xo.android.view.chat.attachments;


import android.content.res.Resources;
import android.text.format.Formatter;
import android.view.View;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.IXoTransferListener;
import com.hoccer.talk.client.XoTransfer;
import com.hoccer.talk.client.model.IXoTransferState;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.content.ContentState;
import com.hoccer.xo.android.XoApplication;
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

    private final XoTransfer mTransfer;

    public AttachmentTransferHandler(AttachmentTransferControlView transferProgress, XoTransfer transfer, AttachmentTransferListener listener) {
        mListener = listener;
        mTransferControl = transferProgress;
        mTransfer = transfer;
        setTransferAction(mTransfer.getContentState());
    }

    @Override
    public void onClick(View v) {
        if (v == mTransferControl) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    setTransferAction(mTransfer.getContentState());
                    switch (mTransferAction) {
                        case REQUEST_DOWNLOAD:
                            if (mTransfer instanceof TalkClientDownload) {
                                LOG.debug("Will resume download for " + ((TalkClientDownload) mTransfer).getDownloadUrl());
                                TalkClientDownload download = (TalkClientDownload) mTransfer;
                                XoApplication.get().getXoClient().getTransferAgent().startOrRestartDownload(download, true);
                            }
                            break;
                        case CANCEL_DOWNLOAD:
                            if (mTransfer instanceof TalkClientDownload) {
                                LOG.debug("Will pause download for " + ((TalkClientDownload) mTransfer).getDownloadUrl());
                                TalkClientDownload download = (TalkClientDownload) mTransfer;
                                XoApplication.get().getXoClient().getTransferAgent().pauseDownload(download);
                            }
                            break;
                        case REQUEST_UPLOAD:
                            if (mTransfer instanceof TalkClientUpload) {
                                LOG.debug("Will resume upload for " + ((TalkClientUpload) mTransfer).getUploadUrl());
                                TalkClientUpload upload = (TalkClientUpload) mTransfer;
                                XoApplication.get().getXoClient().getTransferAgent().startOrRestartUpload(upload);
                            }
                            break;
                        case CANCEL_UPLOAD:
                            if (mTransfer instanceof TalkClientUpload) {
                                LOG.debug("Will pause upload for " + ((TalkClientUpload) mTransfer).getUploadUrl());
                                TalkClientUpload upload = (TalkClientUpload) mTransfer;
                                XoApplication.get().getXoClient().getTransferAgent().pauseUpload(upload);
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

    protected void updateTransferControl() {
        mTransferControl.post(new Runnable() {
            @Override
            public void run() {
                long length;
                long progress;
                Resources res = mTransferControl.getResources();
                ContentState contentState = mTransfer.getContentState();
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
                        length = mTransfer.getTransferLength();
                        progress = mTransfer.getTransferProgress();
                        mTransferControl.setMax(length);
                        mTransferControl.setProgressImmediately(progress);
                        mTransferControl.setText(res.getString(R.string.transfer_state_pause));
                        mTransferControl.prepareToDownload();
                        mTransferControl.pause();
                        break;

                    case DOWNLOAD_ON_HOLD:
                        length = mTransfer.getTransferLength();
                        progress = mTransfer.getTransferProgress();
                        mTransferControl.setMax(length);
                        mTransferControl.setProgressImmediately(progress);
                        TalkClientDownload download = (TalkClientDownload) mTransfer;
                        fileSize = Formatter.formatShortFileSize(mTransferControl.getContext(), download.getTransmittedContentLength());
                        mTransferControl.setText(res.getString(R.string.attachment_on_hold_download_question, fileSize));
                        mTransferControl.prepareToDownload();
                        mTransferControl.pause();
                        break;

                    case DOWNLOAD_DOWNLOADING:
                        length = mTransfer.getTransferLength();
                        progress = mTransfer.getTransferProgress();
                        if (length == 0 || progress == 0) {
                            length = 360;
                        }
                        mTransferControl.prepareToDownload();
                        mTransferControl.setText(res.getString(R.string.transfer_state_downloading));
                        mTransferControl.setMax(length);
                        mTransferControl.setProgressImmediately(progress);
                        break;

                    case DOWNLOAD_DECRYPTING:
                        mTransferControl.setText(res.getString(R.string.transfer_state_decrypting));
                        mTransferControl.spin();
                        break;

                    case DOWNLOAD_COMPLETE:
                        mTransferControl.finishSpinningAndProceed();
                        mListener.onAttachmentTransferComplete(mTransfer);
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
                        length = mTransfer.getTransferLength();
                        progress = mTransfer.getTransferProgress();
                        mTransferControl.setMax(length);
                        mTransferControl.setProgressImmediately(progress);
                        mTransferControl.setText(res.getString(R.string.transfer_state_pause));
                        mTransferControl.pause();
                        break;

                    case UPLOAD_UPLOADING:
                        mTransferControl.finishSpinningAndProceed();
                        mTransferControl.setText(res.getString(R.string.transfer_state_uploading));
                        length = mTransfer.getTransferLength();
                        progress = mTransfer.getTransferProgress();
                        mTransferControl.setMax(length);
                        mTransferControl.setProgressImmediately(progress);
                        break;

                    case UPLOAD_COMPLETE:
                        mTransferControl.completeAndGone();
                        mListener.onAttachmentTransferComplete(mTransfer);
                        break;

                    default:
                        mTransferControl.setVisibility(View.GONE);
                        mListener.onAttachmentTransferComplete(mTransfer);
                        break;
                }
            }
        });
    }

    @Override
    public void onStateChanged(IXoTransferState state) {
        LOG.debug("transfer state changed to " + state + ". Update ControlView");
        setTransferAction(mTransfer.getContentState());
    }

    @Override
    public void onProgressUpdated(long progress, long max) {
        setTransferAction(mTransfer.getContentState());
        mTransferControl.setMax(max);
    }

    @Override
    public void onProgress(int progress) {

    }
}
