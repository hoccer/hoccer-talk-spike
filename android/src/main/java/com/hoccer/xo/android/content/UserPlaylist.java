package com.hoccer.xo.android.content;

import com.hoccer.talk.client.IXoDownloadListener;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientMessage;
import com.hoccer.talk.content.ContentMediaType;
import com.hoccer.talk.content.IContentObject;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Playlist instance wrapping a user filtered list of media items.
 */
public class UserPlaylist extends MediaPlaylist implements IXoDownloadListener {

    private static final Logger LOG = Logger.getLogger(UserPlaylist.class);

    private List<TalkClientDownload> mList;
    private TalkClientContact mContact;
    private XoClientDatabase mDatabase;

    /*
     * Constructs a playlist filtered by the given contact.
     * Note: If contact is null no filtering is performed.
     */
    public UserPlaylist(XoClientDatabase database, TalkClientContact contact) {
        mContact = contact;
        mList = new ArrayList<TalkClientDownload>();
        mDatabase = database;
        mDatabase.registerDownloadListener(this);

        try {
            if(contact != null) {
                mList = mDatabase.findClientDownloadByMediaTypeAndConversationContactId(ContentMediaType.AUDIO, mContact.getClientContactId());
            } else {
                mList = mDatabase.findClientDownloadByMediaType(ContentMediaType.AUDIO);
            }
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public TalkClientContact getContact() {
        return mContact;
    }

    @Override
    public TalkClientDownload getItem(int index) {
        return mList.get(index);
    }

    @Override
    public int size() {
        return mList.size();
    }

    @Override
    public boolean hasItem(IContentObject item) {
        return mList.contains(item);
    }

    @Override
    public int indexOf(IContentObject item) {
        return mList.indexOf(item);
    }

    @Override
    public Iterator<IContentObject> iterator() {
        return new Iterator<IContentObject>() {
            private Iterator<TalkClientDownload> mIterator = mList.iterator();

            @Override
            public boolean hasNext() {
                return mIterator.hasNext();
            }

            @Override
            public IContentObject next() {
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
        // do nothing if the download is incomplete or already contained
        if(download.getState() != TalkClientDownload.State.COMPLETE || mList.contains(download))
            return;

        if(mContact != null) {
            try {
                TalkClientMessage message = mDatabase.findClientMessageByTalkClientDownloadId(download.getClientDownloadId());
                if(message != null && message.getConversationContact().getClientId() == mContact.getClientId()) {
                    mList = mDatabase.findClientDownloadByMediaTypeAndConversationContactId(ContentMediaType.AUDIO, mContact.getClientContactId());
                    invokeItemAdded(download);
                }
            } catch (SQLException e) {
                LOG.error(e.getMessage(), e);
            }
        } else {
            try {
                mList = mDatabase.findClientDownloadByMediaType(ContentMediaType.AUDIO);
                invokeItemAdded(download);
            } catch (SQLException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void onDownloadUpdated(TalkClientDownload download) {
        // do nothing
    }

    @Override
    public void onDownloadDeleted(TalkClientDownload download) {
        if(mContact != null) {
            try {
                TalkClientMessage message = mDatabase.findClientMessageByTalkClientDownloadId(download.getClientDownloadId());
                if(message != null && message.getConversationContact().getClientId() == mContact.getClientId()) {
                    mList = mDatabase.findClientDownloadByMediaTypeAndConversationContactId(ContentMediaType.AUDIO, mContact.getClientContactId());
                    invokeItemRemoved(download);
                }
            } catch (SQLException e) {
                LOG.error(e.getMessage(), e);
            }
        } else {
            try {
                mList = mDatabase.findClientDownloadByMediaType(ContentMediaType.AUDIO);
                invokeItemRemoved(download);
            } catch (SQLException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }
}
