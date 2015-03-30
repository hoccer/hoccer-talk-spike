package com.hoccer.xo.android.backup;

import android.os.Parcel;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.task.DeleteMissingTransfersTask;
import com.hoccer.xo.android.task.ScanTransfersTask;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class CompleteBackup extends DatabaseBackup {

    protected CompleteBackup(File backupFile, BackupMetadata metadata) {
        super(backupFile, metadata);
    }

    public static Backup create(String password) throws Exception {
        String filename = BackupFileUtils.createUniqueBackupFilename(XoApplication.getConfiguration().getAppName());
        File backupFile = new File(XoApplication.getBackupDirectory(), filename + "." + BackupFileUtils.FILE_EXTENSION_ZIP);

        File database = new File(DB_PATH_NAME);

        String clientName = XoApplication.get().getXoClient().getSelfContact().getName();
        BackupMetadata metadata = new BackupMetadata(BackupType.COMPLETE, clientName, new Date());

        File[] files = XoApplication.getAttachmentDirectory().listFiles(BackupFileUtils.IS_FILE_AND_NOT_HIDDEN_FILTER);

        List<File> attachments = new ArrayList<File>();
        if (files != null) {
            attachments = Arrays.asList(files);
        }

        BackupFileUtils.createBackupFile(backupFile, metadata, database, password, attachments);

        return new CompleteBackup(backupFile, metadata);
    }

    @Override
    public void restore(String password) throws Exception {
        File databaseTarget = new File(DB_PATH_NAME);
        File attachmentsTargetDir = XoApplication.getAttachmentDirectory();
        new CompleteRestoreOperation(mBackupFile, databaseTarget, attachmentsTargetDir, password).invoke();

        XoApplication.registerForNextStart(DeleteMissingTransfersTask.class);
        XoApplication.registerForNextStart(ScanTransfersTask.class);
    }

    private CompleteBackup(Parcel source) {
        super(source);
    }

    public static final Creator<? extends CompleteBackup> CREATOR = new Creator<CompleteBackup>() {
        @Override
        public CompleteBackup createFromParcel(Parcel source) {
            return new CompleteBackup(source);
        }

        @Override
        public CompleteBackup[] newArray(int size) {
            return new CompleteBackup[size];
        }
    };
}
