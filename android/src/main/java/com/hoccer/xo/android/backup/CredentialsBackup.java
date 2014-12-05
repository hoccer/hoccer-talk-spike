package com.hoccer.xo.android.backup;

import com.hoccer.talk.util.Credentials;
import com.hoccer.xo.android.XoApplication;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.Date;


public class CredentialsBackup extends Backup {

    private static final Logger LOG = Logger.getLogger(CredentialsBackup.class);

    public static final String BACKUP_FILENAME = "credentials.json";

    protected CredentialsBackup(File backupFile) {
        super(backupFile);
    }

    static Backup create(String password) throws IOException {
        Credentials credentials = XoApplication.getXoClient().exportCredentials();
        final byte[] credentialsContainer = credentials.toEncryptedBytes(password);

        if (exists()) {
            LOG.info("Overwriting existing credentials.json backup file.");
        }

        File backupFile = new File(XoApplication.getExternalStorage(), BACKUP_FILENAME);
        final FileOutputStream fos = new FileOutputStream(backupFile);
        fos.write(credentialsContainer);
        fos.flush();
        fos.close();

        return new CredentialsBackup(backupFile);
    }

    public static boolean exists() {
        File backupFile = new File(XoApplication.getExternalStorage(), BACKUP_FILENAME);
        return backupFile.exists();
    }

    @Nullable
    @Override
    public String getClientName() {
        return null;
    }

    @Override
    public Date getCreationDate() {
        return new Date(mBackupFile.lastModified());
    }

    @Override
    public void restore(String password) throws Exception {
        InputStream in = new FileInputStream(mBackupFile);
        byte[] credentialsData = new byte[(int) mBackupFile.length()];
        in.read(credentialsData);
        Credentials credentials = Credentials.fromEncryptedBytes(credentialsData, password);
        XoApplication.getXoClient().importCredentials(credentials);
    }
}
