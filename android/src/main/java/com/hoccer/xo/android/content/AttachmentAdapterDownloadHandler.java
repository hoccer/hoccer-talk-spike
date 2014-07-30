package com.hoccer.xo.android.content;

import android.app.Activity;
import com.hoccer.talk.client.IXoDownloadListener;
import com.hoccer.talk.client.IXoTransferListener;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.adapter.AttachmentListAdapter;
import com.hoccer.xo.android.service.MediaPlayerService;

import java.sql.SQLException;

/**
 * Created by nico on 23/07/2014.
 */
public class AttachmentAdapterDownloadHandler implements IXoTransferListener, IXoDownloadListener {

    private Activity mActivity;
    private AttachmentListAdapter mAdapter;

    public AttachmentAdapterDownloadHandler(Activity activity, AttachmentListAdapter adapter) {
        mActivity = activity;
        mAdapter = adapter;
    }

    @Override
    public void onDownloadRegistered(TalkClientDownload download) {

    }

    @Override
    public void onDownloadStarted(TalkClientDownload download) {

    }

    @Override
    public void onDownloadProgress(TalkClientDownload download) {

    }

    @Override
    public void onDownloadFinished(TalkClientDownload download) {
        int contactId = MediaPlayerService.UNDEFINED_CONTACT_ID;

        try {
            TalkClientMessage message = XoApplication.getXoClient().getDatabase().findMessageByDownloadId(download.getClientDownloadId());
            contactId = message.getConversationContact().getClientContactId();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (download.getContentMediaType().equals(mAdapter.getContentMediaType())) {
            if ((mAdapter.getConversationContactId() == MediaPlayerService.UNDEFINED_CONTACT_ID) ||
                    (mAdapter.getConversationContactId() == contactId)) {
                mAdapter.addItemAt(download, 0);
                notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onDownloadFailed(TalkClientDownload download) {

    }

    @Override
    public void onDownloadStateChanged(TalkClientDownload download) {

    }

    @Override
    public void onUploadStarted(TalkClientUpload upload) {

    }

    @Override
    public void onUploadProgress(TalkClientUpload upload) {

    }

    @Override
    public void onUploadFinished(TalkClientUpload upload) {

    }

    @Override
    public void onUploadFailed(TalkClientUpload upload) {

    }

    @Override
    public void onUploadStateChanged(TalkClientUpload upload) {

    }

    @Override
    public void onDownloadSaved(TalkClientDownload download, boolean isCreated) {

    }

    @Override
    public void onDownloadRemoved(TalkClientDownload download) {
        mAdapter.removeItem(download);
        notifyDataSetChanged();
    }

    private void notifyDataSetChanged() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyDataSetChanged();
            }
        });
    }
}
