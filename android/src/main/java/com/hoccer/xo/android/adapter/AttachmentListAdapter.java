package com.hoccer.xo.android.adapter;

import android.app.Activity;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.hoccer.talk.client.IXoDownloadListener;
import com.hoccer.talk.client.IXoTransferListener;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientMediaCollection;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.talk.content.IContentObject;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.service.MediaPlayerService;
import com.hoccer.xo.android.view.AudioAttachmentView;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AttachmentListAdapter extends BaseAdapter implements IXoTransferListener, IXoDownloadListener {

    protected Logger LOG = Logger.getLogger(AttachmentListAdapter.class);

    private final Activity mActivity;
    private List<IContentObject> mAttachmentItems = new ArrayList<IContentObject>();

    private String mContentMediaType;
    private int mConversationContactId = MediaPlayerService.UNDEFINED_CONTACT_ID;

    private SparseBooleanArray mSelections;

    public AttachmentListAdapter(Activity activity) {
        this(activity, null, MediaPlayerService.UNDEFINED_CONTACT_ID);
    }

    public AttachmentListAdapter(Activity activity, String pContentMediaType) {
        this(activity, pContentMediaType, MediaPlayerService.UNDEFINED_CONTACT_ID);
    }

    public AttachmentListAdapter(Activity activity, int pConversationContactId) {
        this(activity, null, pConversationContactId);
    }

    public AttachmentListAdapter(Activity activity, String pContentMediaType, int pConversationContactId) {
        mActivity = activity;
        setContentMediaTypeFilter(pContentMediaType);
        setContactIdFilter(pConversationContactId);
    }

    public AttachmentListAdapter createDuplicate() {
        AttachmentListAdapter adapter = new AttachmentListAdapter(mActivity, mContentMediaType, mConversationContactId);
        adapter.setAttachmentItems(mAttachmentItems);

        return adapter;
    }

    public List<IContentObject> getAttachmentList() {
        return mAttachmentItems;
    }

    public void setAttachmentItems(List<IContentObject> items) {
        mAttachmentItems.clear();
        mAttachmentItems.addAll(items);
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public int getCount() {
        return mAttachmentItems.size();
    }

    @Override
    public IContentObject getItem(int position) {
        return mAttachmentItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (mAttachmentItems.get(position) == null) {
            mAttachmentItems.remove(position);
            if (mAttachmentItems.size() <= position) {
                return null;
            }
            return getView(position, convertView, parent);
        }

        AudioAttachmentView audioRowView = (AudioAttachmentView) convertView;
        if (audioRowView == null) {
            audioRowView = new AudioAttachmentView(mActivity);
        }

        audioRowView.setMediaItem(mAttachmentItems.get(position));
        audioRowView.updatePlayPauseView();

        if (mSelections != null) {
            audioRowView.getChildAt(0).setSelected(mSelections.get(position));
        }
        return audioRowView;
    }

    public void setContentMediaTypeFilter(String pContentMediaType) {
        mContentMediaType = pContentMediaType;
    }

    public String getContentMediaType() {
        return mContentMediaType;
    }

    public void setContactIdFilter(int pConversationContactId) {
        mConversationContactId = pConversationContactId;
    }

    public int getConversationContactId() {
        return mConversationContactId;
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

        if (download.getContentMediaType().equals(this.mContentMediaType)) {
            if ((mConversationContactId == MediaPlayerService.UNDEFINED_CONTACT_ID) || (mConversationContactId == contactId)) {
                mAttachmentItems.add(0, download);

                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mSelections != null && mSelections.size() > 0) {
                            updateCheckedItems();
                        }

                        notifyDataSetChanged();
                    }
                });
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

    public void removeItem(int pos) {
        mAttachmentItems.remove(pos);
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    public boolean removeItem(IContentObject item) {
        if (mAttachmentItems.contains(item)) {
            mAttachmentItems.remove(mAttachmentItems.indexOf(item));
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
            return true;
        }
        return false;
    }

    public void addItem(IContentObject item) {
        mAttachmentItems.add(item);
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    public void clear() {
        mAttachmentItems.clear();
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    public void setSelections(SparseBooleanArray selections) {
        this.mSelections = selections;
    }

    public void loadAttachments() {
        try {
            List<TalkClientDownload> downloads;
            if (mContentMediaType != null) {
                if (mConversationContactId != MediaPlayerService.UNDEFINED_CONTACT_ID) {
                    downloads = XoApplication.getXoClient().getDatabase().findClientDownloadByMediaTypeAndConversationContactId(ContentMediaType.AUDIO, mConversationContactId);
                } else {
                    downloads = XoApplication.getXoClient().getDatabase().findClientDownloadByMediaType(mContentMediaType);
                }
            } else {
                downloads = XoApplication.getXoClient().getDatabase().findAllClientDownloads();
            }

            createAttachmentsFromTalkClientDownloads(downloads);
        } catch (SQLException e) {
            LOG.error(e);
        }
    }

    public void loadAttachmentsFromCollection(TalkClientMediaCollection collection) {
        List<TalkClientDownload> downloads = new ArrayList<TalkClientDownload>();
        for (int i = 0; i < collection.size(); ++i) {
            downloads.add(collection.getItem(i));
        }
        createAttachmentsFromTalkClientDownloads(downloads);
    }

    public Activity getActivity() {
        return mActivity;
    }

    private void createAttachmentsFromTalkClientDownloads(Iterable<TalkClientDownload> downloads) {
        if (downloads != null) {
            for (TalkClientDownload download : downloads) {
                if (!isRecordedAudio(download.getFileName())) {
                    mAttachmentItems.add(download);
                }
            }
        }
    }

    private void updateCheckedItems() {
        SparseBooleanArray updatedSelection = new SparseBooleanArray(mSelections.size());

        for (int i = 0; i < mSelections.size(); ++i) {
            boolean b = mSelections.valueAt(i);
            int k = mSelections.keyAt(i);
            updatedSelection.put(k + 1, b);
        }

        mSelections.clear();

        //@Info Setting mSelection to updatedSelection won't work since the reference changes, which is not allowed
        for (int i = 0; i < updatedSelection.size(); ++i) {
            mSelections.append(updatedSelection.keyAt(i), updatedSelection.valueAt(i));
        }
    }

    private boolean isRecordedAudio(String fileName) {
        if (fileName.startsWith("recording")) {
            return true;
        }
        return false;
    }

    @Override
    public void onDownloadSaved(TalkClientDownload download, boolean isCreated) {

    }

    @Override
    public void onDownloadRemoved(TalkClientDownload download) {
        removeItem(download);
    }
}
