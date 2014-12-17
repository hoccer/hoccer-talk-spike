package com.hoccer.xo.android.task;

import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.database.DatabaseOperations;

public class DeleteMissingTransfersTask implements IStartupTask {

    @SuppressWarnings("AccessStaticViaInstance")
    @Override
    public void execute(XoApplication application) {
        DatabaseOperations databaseOperations = new DatabaseOperations(application.getXoClient().getDatabase(), application);
        databaseOperations.removeMissingTransfers();
    }
}
