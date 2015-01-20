package com.hoccer.xo.android.task;

import android.content.Context;
import android.media.MediaScannerConnection;
import com.hoccer.talk.client.XoTransfer;
import com.hoccer.xo.android.XoApplication;

import java.sql.SQLException;
import java.util.List;

public class ScanTransfersTask implements IStartupTask {
    @Override
    public void execute(Context context) {
        try {
            List<? extends XoTransfer> allTransfers = XoApplication.getXoClient().getDatabase().findAllTransfers();
            for(XoTransfer transfer : allTransfers) {
                String[] path = new String[]{transfer.getFilePath()};
                String[] ctype = new String[]{transfer.getContentType()};
                MediaScannerConnection.scanFile(context, path, ctype, null);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
