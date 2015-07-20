package com.hoccer.xo.android.view.chat.attachments;


import android.content.res.Resources;
import android.text.format.Formatter;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.TransferStateListener;
import com.hoccer.talk.client.XoTransfer;
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
public class AttachmentTransferHandler implements View.OnClickListener, TransferStateListener {

    private static final Logger LOG = Logger.getLogger(AttachmentTransferHandler.class);

    public enum TransferAction {
        NONE,
        REQUEST_DOWNLOAD,
        CANCEL_DOWNLOAD,
        REQUEST_UPLOAD,
        CANCEL_UPLOAD,
    }

    private final AttachmentTransferListener mListener;
    private final TextView mTransferStateView;
    private final TransferControlView mTransferControlView;
    private TransferAction mTransferAction = TransferAction.NONE;

    private final XoTransfer mTransfer;

    public AttachmentTransferHandler(RelativeLayout transferContainer, XoTransfer transfer, AttachmentTransferListener listener) {
        mListener = listener;

        mTransfer = transfer;

        mTransferStateView = (TextView) transferContainer.findViewById(R.id.tv_transfer_state);
        mTransferControlView = (TransferControlView) transferContainer.findViewById(R.id.view_transfer_control);
        mTransferControlView.setOnClickListener(this);

        resetTransferControlView();

        setTransferAction();
        updateTransferControlView();
    }

    private void resetTransferControlView() {
        mTransferControlView.clean();
    }

    @Override
    public void onClick(View v) {
        if (v == mTransferControlView) {
            setTransferAction();
            updateTransferControlView();
            switch (mTransferAction) {
                case REQUEST_DOWNLOAD:
                    if (mTransfer instanceof TalkClientDownload) {
                        LOG.debug("Will resume download for " + ((TalkClientDownload) mTransfer).getDownloadUrl());
                        TalkClientDownload download = (TalkClientDownload) mTransfer;
                        XoApplication.get().getXoClient().getDownloadAgent().startDownloadTask(download);
                    }
                    break;
                case CANCEL_DOWNLOAD:
                    if (mTransfer instanceof TalkClientDownload) {
                        LOG.debug("Will pause download for " + ((TalkClientDownload) mTransfer).getDownloadUrl());
                        final TalkClientDownload download = (TalkClientDownload) mTransfer;
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                XoApplication.get().getXoClient().getDownloadAgent().pauseDownload(download);
                            }
                        }).start();
                    }
                    break;
                case REQUEST_UPLOAD:
                    if (mTransfer instanceof TalkClientUpload) {
                        LOG.debug("Will resume upload for " + ((TalkClientUpload) mTransfer).getUploadUrl());
                        final TalkClientUpload upload = (TalkClientUpload) mTransfer;
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                XoApplication.get().getXoClient().getUploadAgent().resumeUpload(upload);
                            }
                        }).start();
                    }
                    break;
                case CANCEL_UPLOAD:
                    if (mTransfer instanceof TalkClientUpload) {
                        LOG.debug("Will pause upload for " + ((TalkClientUpload) mTransfer).getUploadUrl());
                        final TalkClientUpload upload = (TalkClientUpload) mTransfer;
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                XoApplication.get().getXoClient().getUploadAgent().pauseUpload(upload);
                            }
                        }).start();
                    }
            }
            mTransferControlView.post(new Runnable() {
                @Override
                public void run() {
                    mTransferControlView.invalidate();
                }
            });
        }
    }

    private void setTransferAction() {
        mTransferAction = TransferAction.NONE;
        switch (mTransfer.getContentState()) {
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
            case DOWNLOAD_COMPLETE:
                break;
            case DOWNLOAD_FAILED:
                break;
            case UPLOAD_NEW:
            case UPLOAD_PAUSED:
            case UPLOAD_FAILED:
                mTransferAction = TransferAction.REQUEST_UPLOAD;
                break;
            case UPLOAD_REGISTERING:
                break;
            case UPLOAD_ENCRYPTING:
            case UPLOAD_UPLOADING:
                mTransferAction = TransferAction.CANCEL_UPLOAD;
                break;
            case UPLOAD_COMPLETE:
                break;
            default:
                break;
        }
    }

    private void updateTransferControlView() {
        mTransferControlView.setEnabled(true);
        mTransferControlView.setVisibility(View.VISIBLE);

        long length;
        long progress;
        Resources res = mTransferControlView.getResources();
        ContentState contentState = mTransfer.getContentState();
        String fileSize;
        switch (contentState) {
            case DOWNLOAD_DETECTING:
                break;

            case DOWNLOAD_NEW:
                mTransferControlView.setEnabled(false);
                mTransferControlView.setVisibility(View.GONE);
                mTransferStateView.setText(res.getString(R.string.transfer_state_initializing_download));
                break;

            case DOWNLOAD_PAUSED:
                length = mTransfer.getTransferLength();
                progress = mTransfer.getTransferProgress();
                mTransferControlView.setMax(length);
                mTransferControlView.setProgressImmediately(progress);
                mTransferControlView.prepareToDownload();
                mTransferControlView.pause();
                mTransferStateView.setText(res.getString(R.string.transfer_state_pause));
                break;

            case DOWNLOAD_ON_HOLD:
                length = mTransfer.getTransferLength();
                progress = mTransfer.getTransferProgress();
                mTransferControlView.setMax(length);
                mTransferControlView.setProgressImmediately(progress);
                TalkClientDownload download = (TalkClientDownload) mTransfer;
                fileSize = Formatter.formatShortFileSize(mTransferControlView.getContext(), download.getTransmittedContentLength());
                mTransferControlView.prepareToDownload();
                mTransferControlView.pause();
                mTransferStateView.setText(res.getString(R.string.attachment_on_hold_download_question, fileSize));
                break;

            case DOWNLOAD_DOWNLOADING:
                length = mTransfer.getTransferLength();
                progress = mTransfer.getTransferProgress();
                if (length == 0 || progress == 0) {
                    length = 360;
                }
                mTransferControlView.prepareToDownload();
                mTransferControlView.setMax(length);
                mTransferControlView.setProgressImmediately(progress);
                mTransferStateView.setText(res.getString(R.string.transfer_state_downloading));
                break;

            case DOWNLOAD_DECRYPTING:
                mTransferControlView.spin();
                mTransferStateView.setText(res.getString(R.string.transfer_state_decrypting));
                break;

            case DOWNLOAD_COMPLETE:
                mTransferControlView.finishSpinningAndProceed();
                mListener.onAttachmentTransferComplete(mTransfer);
                break;

            case DOWNLOAD_FAILED:
                mTransferControlView.setOnClickListener(null);
                mTransferStateView.setText(res.getString(R.string.transfer_state_downloading_failed));
                break;

            case UPLOAD_REGISTERING:
                break;

            case UPLOAD_NEW:
                mTransferControlView.prepareToUpload();
                mTransferControlView.setVisibility(View.VISIBLE);
                mTransferStateView.setText(res.getString(R.string.transfer_state_initializing_upload));
                break;

            case UPLOAD_ENCRYPTING:
                mTransferControlView.prepareToUpload();
                mTransferControlView.setVisibility(View.VISIBLE);
                mTransferControlView.spin();
                mTransferStateView.setText(res.getString(R.string.transfer_state_encrypting));
                break;

            case UPLOAD_PAUSED:
                length = mTransfer.getTransferLength();
                progress = mTransfer.getTransferProgress();
                mTransferControlView.setMax(length);
                mTransferControlView.setProgressImmediately(progress);
                mTransferControlView.pause();
                mTransferStateView.setText(res.getString(R.string.transfer_state_pause));
                break;

            case UPLOAD_UPLOADING:
                mTransferControlView.finishSpinningAndProceed();
                length = mTransfer.getTransferLength();
                progress = mTransfer.getTransferProgress();
                mTransferControlView.setMax(length);
                mTransferControlView.setProgressImmediately(progress);
                mTransferStateView.setText(res.getString(R.string.transfer_state_uploading));
                break;

            case UPLOAD_COMPLETE:
                mTransferControlView.completeAndGone();
                mListener.onAttachmentTransferComplete(mTransfer);
                break;

            case UPLOAD_FAILED:
                mTransferControlView.setOnClickListener(null);
                mTransferStateView.setText(res.getString(R.string.transfer_state_uploading_failed));
                break;

            default:
                mTransferControlView.setVisibility(View.GONE);
                mListener.onAttachmentTransferComplete(mTransfer);
                break;
        }
    }

    @Override
    public void onStateChanged(XoTransfer transfer) {
        LOG.debug("transfer " + transfer.getFilename() + " state changed to " + transfer.getContentState());

        setTransferAction();

        mTransferControlView.post(new Runnable() {
            @Override
            public void run() {
                updateTransferControlView();
            }
        });
    }

    @Override
    public void onProgressUpdated(long progress, long max) {
        setTransferAction();

        mTransferControlView.post(new Runnable() {
            @Override
            public void run() {
                updateTransferControlView();
            }
        });

        mTransferControlView.setMax(max);
    }

    @Override
    public void onProgress(int progress) {

    }
}
