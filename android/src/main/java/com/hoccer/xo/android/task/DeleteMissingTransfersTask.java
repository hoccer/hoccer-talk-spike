package com.hoccer.xo.android.task;

import android.content.Context;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.database.DatabaseOperations;

public class DeleteMissingTransfersTask implements IStartupTask {

    @Override
    public void execute(Context context) {
        DatabaseOperations databaseOperations = new DatabaseOperations(XoApplication.get().getClient().getDatabase(), context);
        databaseOperations.removeMissingTransfers();
    }
}
