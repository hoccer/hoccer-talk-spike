package com.hoccer.xo.android.database;

import android.content.Context;
import android.net.Uri;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.XoTransfer;
import com.hoccer.xo.android.util.UriUtils;
import org.apache.log4j.Logger;

import java.io.File;
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
                if (transfer.getFilePath() != null && isMissing(transfer)) {
                    removeMissingTransfer(transfer);
                }
            }
        } catch (SQLException e) {
            LOG.error("Error while removing missing transfer", e);
        }
    }

    private static boolean isMissing(XoTransfer transfer) {
        Uri filePathUri = UriUtils.getAbsoluteFileUri(transfer.getFilePath());
        if (filePathUri != null) {
            File dataFile = new File(filePathUri.getPath());
            return !dataFile.exists();
        }
        return false;
    }

    private void removeMissingTransfer(XoTransfer transfer) {
        try {
            LOG.info("Removing missing transfer " + transfer.getFilePath());
            mDatabase.deleteTransferAndUpdateMessage(transfer, mContext.getResources().getString(R.string.deleted_attachment));
        } catch (SQLException e) {
            LOG.error("Error while removing missing transfer " + transfer.getFilePath(), e);
        }
    }
}
