package com.hoccer.xo.android.database;

import android.content.Context;
import android.net.Uri;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.XoTransfer;
import com.hoccer.xo.android.util.UriUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.SQLException;

public class DatabaseOperations {

    private static final Logger LOG = Logger.getLogger(DatabaseOperations.class);

    private final XoClientDatabase mDatabase;
    private final Context mContext;

    public DatabaseOperations(XoClientDatabase database, Context context) {
        mDatabase = database;
        mContext = context;
    }

    public void removeMissingTransfers() {
        try {
            for (XoTransfer transfer : mDatabase.findAllTransfers()) {
                if (isMissing(transfer)) {
                    removeMissingTransfer(transfer);
                }
            }
        } catch (SQLException e) {
            LOG.error("Error while removing missing transfer", e);
        }
    }

    private boolean isMissing(XoTransfer transfer) {
        Uri dataUri = UriUtils.getAbsoluteFileUri(transfer.getContentDataUrl());

        if (dataUri != null) {
            if (UriUtils.isFileUri(dataUri)) {
                File dataFile = new File(dataUri.getPath());
                return !dataFile.exists();
            } else if (UriUtils.isContentUri(dataUri)) {
                InputStream inputStream = null;
                try {
                    inputStream = mContext.getContentResolver().openInputStream(dataUri);
                } catch (FileNotFoundException e) {
                    return true;
                } finally {
                    IOUtils.closeQuietly(inputStream);
                }
            }
        }

        return false;
    }

    private void removeMissingTransfer(XoTransfer transfer) {
        try {
            LOG.info("Removing missing transfer " + transfer.getContentDataUrl());
            mDatabase.deleteTransferAndUpdateMessage(transfer, mContext.getResources().getString(R.string.deleted_attachment));
        } catch (SQLException e) {
            LOG.error("Error while removing missing transfer " + transfer.getContentDataUrl(), e);
        }
    }
}
