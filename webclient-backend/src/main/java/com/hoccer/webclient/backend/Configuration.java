package com.hoccer.webclient.backend;

import com.hoccer.talk.client.XoDefaultClientConfiguration;
import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Properties;

/**
 * Encapsulates all available command line settings.
 * This gets initialized with defaults and can then
 * be overloaded from a property file.
 */
public class Configuration extends XoDefaultClientConfiguration {

    private static final Logger LOG = Logger.getLogger(Configuration.class);

    private String mServerUri = "wss://talkserver.talk.hoccer.de/";
    private String mTrustedCertFile = null;

    private int mBackendPort = 5000;
    private String mContactName = "webclient";
    private String mAvatarFile = "";

    private double mLatitude = 0.0;
    private double mLongitude = 0.0;

    private String mEncAttDir = "encrypted_attachments";
    private String mDecAttDir = "decrypted_attachments";
    private String mDatabaseDir = "database";

    @Override
    public String getServerUri() {
        return mServerUri;
    }

    public String getTrustedCertFile() {
        return mTrustedCertFile;
    }

    public int getBackendPort() {
        return mBackendPort;
    }

    public String getContactName() {
        return mContactName;
    }

    public String getAvatarFile() {
        return mAvatarFile;
    }

    public String getEncAttachmentDir() {
        return mEncAttDir;
    }

    public String getDecAttachmentDir() {
        return mDecAttDir;
    }

    public String getDatabaseDir() {
        return mDatabaseDir;
    }

    public double getLatitude() {
        return mLatitude;
    }

    public double getLongitude() {
        return mLongitude;
    }

    public Configuration(String configFile) {
        if (configFile != null) {
            LOG.info("Loading configuration from property file: '" + configFile + "'");

            try {
                FileInputStream configInputStream = new FileInputStream(configFile);
                Properties properties = new Properties();
                properties.load(configInputStream);
                loadFromProperties(properties);
            } catch (FileNotFoundException e) {
                LOG.error("Could not find configuration file", e);
            } catch (IOException e) {
                LOG.error("Could not load configuration", e);
            }
        }
    }

    private void loadFromProperties(Properties properties) {
        mServerUri = properties.getProperty("server.uri", mServerUri);
        mTrustedCertFile = properties.getProperty("server.certificate_file", mTrustedCertFile);
        mBackendPort = Integer.valueOf(properties.getProperty("backend.port", Integer.toString(mBackendPort)));
        mContactName = properties.getProperty("client.contact_name", mContactName);
        mAvatarFile = properties.getProperty("client.avatar_file", mAvatarFile);
        mEncAttDir = properties.getProperty("client.encrypted_attachment_dir", mEncAttDir);
        mDecAttDir = properties.getProperty("client.decrypted_attachment_dir", mDecAttDir);
        mDatabaseDir = properties.getProperty("client.database_dir", mDatabaseDir);
        mLatitude = Double.valueOf(properties.getProperty("client.latitude", Double.toString(mLatitude)));
        mLongitude = Double.valueOf(properties.getProperty("client.longitude", Double.toString(mLongitude)));
    }

    public void report() {
        LOG.info("Current configuration:" +
            MessageFormat.format("\n  TalkServer URI:              ''{0}''", mServerUri) +
            MessageFormat.format("\n  Trusted Certificate File:    ''{0}''", mTrustedCertFile) +
            MessageFormat.format("\n  Backend Port:                ''{0}''", Integer.toString(mBackendPort)) +
            MessageFormat.format("\n  Contact Name:                ''{0}''", mContactName) +
            MessageFormat.format("\n  Avatar File:                 ''{0}''", mAvatarFile) +
            MessageFormat.format("\n  Encrypted Attachment Folder: ''{0}''", mEncAttDir) +
            MessageFormat.format("\n  Decrypted Attachment Folder: ''{0}''", mDecAttDir) +
            MessageFormat.format("\n  Database Folder:             ''{0}''", mDatabaseDir) +
            MessageFormat.format("\n  Latitude:                    ''{0}''", Double.toString(mLatitude)) +
            MessageFormat.format("\n  Longitude:                   ''{0}''", Double.toString(mLongitude)) +
            "\n-------------------------------------------------------------------------");
    }
}
