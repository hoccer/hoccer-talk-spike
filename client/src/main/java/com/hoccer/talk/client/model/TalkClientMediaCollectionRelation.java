package com.hoccer.talk.client.model;

import com.hoccer.talk.client.XoTransfer;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.apache.log4j.Logger;

/**
 * Encapsulates one relation between a media collection and one item.
 */
@DatabaseTable(tableName = "mediaCollectionRelation")
public class TalkClientMediaCollectionRelation {

    private static final Logger LOG = Logger.getLogger(TalkClientMediaCollectionRelation.class);

    @DatabaseField(generatedId = true, columnName = "relationId")
    private int mRelationId;

    @DatabaseField(columnName = "collection_id")
    private int mMediaCollectionId;

    @DatabaseField(columnName = "uploadItem", foreign = true, foreignAutoRefresh = true)
    private TalkClientUpload mUploadItem;

    @DatabaseField(columnName = "item", foreign = true, foreignAutoRefresh = true)
    private TalkClientDownload mDownloadItem;

    @DatabaseField(columnName = "index")
    private int mIndex;

    // do not call constructor directly but create instances via IXoMediaCollectionDatabase.createMediaCollectionRelation()
    private TalkClientMediaCollectionRelation() {
    }

    public TalkClientMediaCollectionRelation(int collectionId, XoTransfer item, int index) {
        mMediaCollectionId = collectionId;

        switch (item.getDirection()) {
            case UPLOAD:
                mUploadItem = (TalkClientUpload)item;
                break;
            case DOWNLOAD:
                mDownloadItem = (TalkClientDownload)item;
                break;
        }

        mIndex = index;
    }

    public int getRelationId() {
        return mRelationId;
    }

    public int getMediaCollectionId() {
        return mMediaCollectionId;
    }

    public XoTransfer getTransferItem() {
        if (mUploadItem != null) {
            return mUploadItem;
        } else {
            return mDownloadItem;
        }
    }

    // This setter updates the index locally only and does not update database fields automatically.
    public void setIndex(int index) {
        mIndex = index;
    }

    public int getIndex() {
        return mIndex;
    }
}
