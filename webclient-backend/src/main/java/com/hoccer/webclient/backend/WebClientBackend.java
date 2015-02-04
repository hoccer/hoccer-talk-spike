package com.hoccer.webclient.backend;

import com.hoccer.talk.client.IXoStateListener;
import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.model.TalkEnvironment;
import com.hoccer.webclient.backend.client.ClientDatabase;
import com.hoccer.webclient.backend.client.ClientHost;
import com.hoccer.webclient.backend.client.TransferListener;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;

public class WebClientBackend implements IXoStateListener {

    private static final Logger LOG = Logger.getLogger(WebClientBackend.class);

    Configuration mConfiguration;

    ClientHost mClientHost;
    XoClient mClient;

    TransferListener mTransferListener;

    boolean mAvatarUploaded = false;

    public WebClientBackend(Configuration configuration) {
        mConfiguration = configuration;

        File databaseFile = new File(
                mConfiguration.getDatabaseDir(),
                mConfiguration.getContactName().replace(" ", "_"));
        ClientDatabase database = new ClientDatabase(databaseFile.getPath());

        mClientHost = new ClientHost(database, mConfiguration);

        mClient = new XoClient(mClientHost, mConfiguration);
        mClient.getSelfContact().getSelf().setRegistrationName(mConfiguration.getContactName());

        String workingDir = System.getProperty("user.dir");

        mClient.setEncryptedDownloadDirectory(mConfiguration.getEncAttachmentDir());
        mClient.setRelativeAttachmentDirectory(mConfiguration.getDecAttachmentDir());
        mClient.setAttachmentDirectory(workingDir + File.separator + mClient.getRelativeAttachmentDirectory());
        mClient.setExternalStorageDirectory(workingDir);

        // create and register client event handler
        mTransferListener = new TransferListener(mClient, mConfiguration);
        mClient.registerTransferListener(mTransferListener);
        mClient.registerStateListener(this);

        mClient.wake();
    }

    public void onClientStateChange(XoClient client, int state) {
        if(state == XoClient.STATE_READY) {
            // send nearby environment
            mClient.sendEnvironmentUpdate(getEnvironment());

            // upload avatar on first active connection
            if (!mAvatarUploaded) {
                uploadAvatar();
            }
        }
    }

    private TalkEnvironment getEnvironment() {
        TalkEnvironment environment = new TalkEnvironment();
        environment.setType(TalkEnvironment.TYPE_NEARBY);

        Double[] geoLocation = { mConfiguration.getLongitude(), mConfiguration.getLatitude() };
        environment.setGeoLocation(geoLocation);
        environment.setLocationType("GPS_PROVIDER");
        environment.setAccuracy(0.0f);
        environment.setTimestamp(new Date());
        return environment;
    }

    private void uploadAvatar() {
        // only proceed if a custom avatar file is set
        String avatarFile = mConfiguration.getAvatarFile();
        if (avatarFile.isEmpty()) {
            return;
        }

        // retrieve length of file
        int length;
        File file = new File(avatarFile);

        if(!file.exists()) {
            LOG.error("The given avatar file does not exist: " + avatarFile);
            return;
        }
        length = (int)file.length();

        // retrieve absolute file path
        String absoluteFilePath;
        try {
            absoluteFilePath = file.getCanonicalPath();
        } catch(IOException e) {
            LOG.error("Could not retrieve absolute avatar image file path: " + avatarFile, e);
            return;
        }

        // prepare image upload
        TalkClientUpload upload = new TalkClientUpload();
        upload.initializeAsAvatar(
                absoluteFilePath,
                "image/jpeg"
        );

        // upload and register avatar
        try {
            mClient.getDatabase().saveClientUpload(upload);
            mClient.setClientAvatar(upload);
            mAvatarUploaded = true;
        } catch (SQLException e) {
            LOG.error("Avatar could not be set: " + avatarFile, e);
        }
    }

    public XoClient getXoClient() {
        return mClient;
    }
}
