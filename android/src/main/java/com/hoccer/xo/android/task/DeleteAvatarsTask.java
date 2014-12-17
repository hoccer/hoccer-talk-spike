package com.hoccer.xo.android.task;

import com.hoccer.xo.android.XoApplication;
import org.apache.log4j.Logger;

import java.sql.SQLException;

public class DeleteAvatarsTask implements IStartupTask {

    private static final Logger LOG = Logger.getLogger(DeleteAvatarsTask.class);

    @SuppressWarnings("AccessStaticViaInstance")
    @Override
    public void execute(XoApplication application) {
        try {
            application.getXoClient().getDatabase().deleteAllAvatarClientDownloads();
        } catch (SQLException e) {
            LOG.error(e);
        }
    }
}
