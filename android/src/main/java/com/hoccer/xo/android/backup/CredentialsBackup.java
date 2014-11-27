package com.hoccer.xo.android.backup;

import java.io.File;

public class CredentialsBackup extends Backup {

    public CredentialsBackup(File backupFile) {
        super(backupFile);
    }

    public static CredentialsBackup create() {
        return null;
    }
}
