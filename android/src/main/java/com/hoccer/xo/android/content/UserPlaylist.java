package com.hoccer.xo.android.content;

import com.hoccer.talk.client.IXoDownloadListener;
import com.hoccer.talk.client.IXoUploadListener;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.XoTransfer;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.content.ContentMediaType;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Playlist instance wrapping a user filtered list of media items.
 */
public class UserPlaylist extends MediaPlaylist implements IXoUploadListener, IXoDownloadListener {

    private static final Logger LOG = Logger.getLogger(UserPlaylist.class);

    private List<XoTransfer> mList;
    private final TalkClientContact mContact;
    private final XoClientDatabase mDatabase;

    /*
     * Constructs a playlist filtered by the given contact.
     * Note: If contact is null no filtering is performed.
     */
    public UserPlaylist(XoClientDatabase database, TalkClientContact contact) {
        mContact = contact;
        mList = new ArrayList<XoTransfer>();
        mDatabase = database;
        mDatabase.registerUploadListener(this);
        mDatabase.registerDownloadListener(this);

        try {
            if (contact != null) {
                mList = new ArrayList<XoTransfer>(mDatabase.findClientDownloadsByMediaTypeAndContactId(ContentMediaType.AUDIO, mContact.getClientContactId()));
            } else {
                mList = mDatabase.findTransfersByMediaTypeDistinct(ContentMediaType.AUDIO);
            }
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public TalkClientContact getContact() {
        return mContact;
    }

    @Override
    public XoTransfer getItem(int index) {
        return mList.get(index);
    }

    @Override
    public int size() {
        return mList.size();
    }

    @Override
    public boolean hasItem(XoTransfer item) {
        return mList.contains(item);
    }

    @Override
    public int indexOf(XoTransfer item) {
        return mList.indexOf(item);
    }

    @Override
    public Iterator<XoTransfer> iterator() {
        return new Iterator<XoTransfer>() {
            private final Iterator<XoTransfer> mIterator = mList.iterator();

            @Override
            public boolean hasNext() {
                return mIterator.hasNext();
            }

            @Override
            public XoTransfer next() {
                return mIterator.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public void onDownloadCreated(TalkClientDownload download) {
        // do nothing if the download is incomplete
        if (download.getState() == TalkClientDownload.State.COMPLETE) {
            addItem(download);
        }
    }

    @Override
    public void onDownloadUpdated(TalkClientDownload download) {
        // do nothing if the download is incomplete
        if (download.getState() == TalkClientDownload.State.COMPLETE) {
            addItem(download);
        }
    }

    @Override
    public void onDownloadDeleted(TalkClientDownload download) {
        removeItem(download);
    }

    @Override
    public void onUploadCreated(TalkClientUpload upload) {
        addItem(upload);
    }

    @Override
    public void onUploadUpdated(TalkClientUpload upload) {
        addItem(upload);
    }

    @Override
    public void onUploadDeleted(TalkClientUpload upload) {
        removeItem(upload);
    }

    private void addItem(XoTransfer transfer) {
        // check if the item is already in the playlist
        if (mList.contains(transfer)) {
            return;
        }

        if (mContact != null) {
            // check if contact matches
            try {
                TalkClientMessage message = transfer.isUpload() ?
                        mDatabase.findClientMessageByTalkClientUploadId(transfer.getUploadOrDownloadId()) :
                        mDatabase.findClientMessageByTalkClientDownloadId(transfer.getUploadOrDownloadId());

                if (message != null && message.getConversationContact().getClientContactId() == mContact.getClientContactId()) {
                    mList = new ArrayList<XoTransfer>(mDatabase.findClientDownloadsByMediaTypeAndContactIdDistinct(ContentMediaType.AUDIO, mContact.getClientContactId()));
                    invokeItemAdded(transfer);
                }
            } catch (SQLException e) {
                LOG.error(e.getMessage(), e);
            }
        } else {
            try {
                mList = mDatabase.findTransfersByMediaTypeDistinct(ContentMediaType.AUDIO);
                invokeItemAdded(transfer);
            } catch (SQLException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    private void removeItem(XoTransfer transfer) {
        if (mContact != null) {
            try {
                TalkClientMessage message = transfer.isUpload() ?
                        mDatabase.findClientMessageByTalkClientUploadId(transfer.getUploadOrDownloadId()) :
                        mDatabase.findClientMessageByTalkClientDownloadId(transfer.getUploadOrDownloadId());

                if (message != null && message.getConversationContact().getClientContactId() == mContact.getClientContactId()) {
                    mList = new ArrayList<XoTransfer>(mDatabase.findClientDownloadsByMediaTypeAndContactIdDistinct(ContentMediaType.AUDIO, mContact.getClientContactId()));
                    invokeItemRemoved(transfer);
                }
            } catch (SQLException e) {
                LOG.error(e.getMessage(), e);
            }
        } else {
            try {
                mList = mDatabase.findTransfersByMediaTypeDistinct(ContentMediaType.AUDIO);
                invokeItemRemoved(transfer);
            } catch (SQLException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }
}
