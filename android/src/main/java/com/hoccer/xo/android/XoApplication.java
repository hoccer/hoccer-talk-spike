package com.hoccer.xo.android;

import android.app.Application;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Environment;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hoccer.talk.client.IXoClientHost;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.talk.model.TalkPresence;
import com.hoccer.xo.android.credentialtransfer.SrpChangeListener;
import com.hoccer.xo.android.error.EnvironmentUpdaterException;
import com.hoccer.xo.android.nearby.EnvironmentUpdater;
import com.hoccer.xo.android.task.StartupTasks;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * This class handles the application lifecycle and is responsible
 * for such things as initializing the logger and setting up the
 * XO client itself. All global initialization should go here.
 */
public class XoApplication extends Application implements Thread.UncaughtExceptionHandler {

    private static Logger sLog;

    /* Directories in internal storage */
    private static final String DOWNLOADS_DIRECTORY = "downloads";
    private static final String GENERATED_DIRECTORY = "generated";
    private static final String THUMBNAILS_DIRECTORY = "thumbnails";

    public static final String HOCCER_CLASSIC_ATTACHMENTS_DIRECTORY = "hoccer";

    private static File sExternalStorage;
    private static File sInternalStorage;
    private static File sInternalCacheStorage;

    private static String sPackageName;

    /**
     * Background executor thread count
     * AFAIK this must be at least 3 for RPC to work.
     */
    private static final int CLIENT_THREAD_COUNT = 100;

    // global executor for client background activity (initialized in onCreate)
    private static ScheduledExecutorService sExecutor;

    // global executor for incoming connections
    private static ScheduledExecutorService sIncomingExecutor;

    private static Thread.UncaughtExceptionHandler sUncaughtExceptionHandler;
    private Thread.UncaughtExceptionHandler mPreviousHandler;

    private static IXoClientHost sClientHost;
    private static XoAndroidClient sClient;
    private static XoAndroidClientConfiguration sConfiguration;
    private static XoSoundPool sSoundPool;
    private static EnvironmentUpdater sEnvironmentUpdater;
    private static DisplayImageOptions sImageOptions;

    private static boolean sIsNearbySessionRunning;

    private static StartupTasks sStartupTasks;

    @Override
    public void onCreate() {
        super.onCreate();

        // currently we use our own instance here
        sUncaughtExceptionHandler = this;

        // initialize storage roots (do so early for log files)
        sExternalStorage = Environment.getExternalStorageDirectory();
        sInternalStorage = this.getFilesDir();
        sInternalCacheStorage = this.getCacheDir();

        // Initialize configuration
        sConfiguration = new XoAndroidClientConfiguration(this);

        // set package name
        sPackageName = getPackageName();

        // initialize logging system
        XoLogging.initialize(this, sConfiguration.getAppName().replace(" ", ""));

        // configure ormlite to use log4j
        System.setProperty("com.j256.ormlite.logger.type", "LOG4J");

        // get logger for this class
        sLog = Logger.getLogger(XoApplication.class);

        // announce sdk version
        sLog.info("system sdk " + Build.VERSION.SDK_INT);
        sLog.info("system release " + Build.VERSION.RELEASE);
        sLog.info("system codename " + Build.VERSION.CODENAME);
        sLog.info("system revision " + Build.VERSION.INCREMENTAL);
        sLog.info("system brand " + Build.BRAND);
        sLog.info("system model " + Build.MODEL);
        sLog.info("system manufacturer " + Build.MANUFACTURER);
        sLog.info("system device " + Build.DEVICE);
        sLog.info("system product " + Build.PRODUCT);
        sLog.info("system type " + Build.TYPE);

        // install a default exception handler
        sLog.info("setting up default exception handler");
        mPreviousHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);

        // log storage roots
        sLog.info("internal storage at " + sInternalStorage.toString());
        sLog.info("external storage at " + sExternalStorage.toString());

        // initialize version information
        XoVersion.initialize(this);
        sLog.info("application build time " + XoVersion.getBuildTime());
        sLog.info("application branch " + XoVersion.getBranch());
        sLog.info("application commit " + XoVersion.getCommitId());
        sLog.info("application describe " + XoVersion.getCommitDescribe());

        // configure ssl
        XoSsl.initialize(this);

        // configure image loader
        sLog.info("configuring image loader");
        sImageOptions = new DisplayImageOptions.Builder()
                .cacheOnDisc(false)
                .cacheInMemory(false)
                .imageScaleType(ImageScaleType.IN_SAMPLE_INT)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .considerExifParams(true)
                .build();

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this)
                .threadPoolSize(2)
                .build();
        ImageLoader.getInstance().init(config);

        // if updated from Hoccer Classic
        renameHoccerClassicAttachmentDirectory();

        // set up directories
        sLog.info("setting up directory structure");
        ensureDirectory(getAttachmentDirectory());
        ensureDirectory(getBackupDirectory());
        ensureDirectory(getAvatarDirectory());
        ensureDirectory(getGeneratedDirectory());
        ensureNoMedia(getGeneratedDirectory());
        ensureDirectory(getThumbnailDirectory());
        ensureDirectory(getEncryptedDownloadDirectory());
        ensureNoMedia(getEncryptedDownloadDirectory());

        // create executor
        sLog.info("creating background executor");
        ThreadFactoryBuilder tfb = new ThreadFactoryBuilder();
        tfb.setNameFormat("client-%d");
        tfb.setUncaughtExceptionHandler(this);
        sExecutor = Executors.newScheduledThreadPool(CLIENT_THREAD_COUNT, tfb.build());
        ThreadFactoryBuilder tfb2 = new ThreadFactoryBuilder();
        tfb2.setNameFormat("receiving client-%d");
        tfb2.setUncaughtExceptionHandler(this);
        sIncomingExecutor = Executors.newScheduledThreadPool(CLIENT_THREAD_COUNT, tfb2.build());

        // create client instance
        sLog.info("creating client");
        sClientHost = new XoAndroidClientHost(this);
        XoAndroidClient client = new XoAndroidClient(sClientHost, sConfiguration);
        client.setAvatarDirectory(getAvatarDirectory().toString());
        client.setAttachmentDirectory(getAttachmentDirectory().toString());
        client.setEncryptedDownloadDirectory(getEncryptedDownloadDirectory().toString());
        sClient = client;

        // add srp secret change listener
        client.registerStateListener(new SrpChangeListener(this));

        // create sound pool instance
        sSoundPool = new XoSoundPool(this);

        sEnvironmentUpdater = new EnvironmentUpdater(this, sClient);

        sStartupTasks = new StartupTasks(this);
        sStartupTasks.executeRegisteredTasks();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        sLog.info("deactivating client");
        if (sClient != null) {
            sClient.deactivateNow();
            sClient = null;
        }

        sLog.info("removing uncaught exception handler");
        if (mPreviousHandler != null) {
            Thread.setDefaultUncaughtExceptionHandler(mPreviousHandler);
            mPreviousHandler = null;
        }

        sLog.info("shutting down executor");
        if (sExecutor != null) {
            sExecutor.shutdownNow();
            sExecutor = null;
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        sLog.warn("Received onTrimMemory(" + level + ").");
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        sLog.error("uncaught exception on thread " + thread.getName(), ex);
        if (mPreviousHandler != null) {
            mPreviousHandler.uncaughtException(thread, ex);
        }
    }

    public static void ensureDirectory(File directory) {
        if (!directory.exists()) {
            sLog.info("creating directory " + directory.toString());
            if (!directory.mkdirs()) {
                sLog.info("Error creating directory " + directory.toString());
            }
        }
    }

    public static void ensureNoMedia(File directory) {
        if (directory.exists()) {
            File noMedia = new File(directory, ".nomedia");
            if (!noMedia.exists()) {
                sLog.info("creating noMedia marker " + noMedia.toString());
                try {
                    if (!noMedia.createNewFile()) {
                        sLog.info("Error creating directory " + noMedia.toString());
                    }
                } catch (IOException e) {
                    sLog.error("error creating " + noMedia.toString(), e);
                }
            }
        }
    }

    public static File getAvatarLocation(TalkClientDownload download) {
        if (download.getState() == TalkClientDownload.State.COMPLETE) {
            File avatarDir = getAvatarDirectory();
            ensureDirectory(avatarDir);
            String dataFile = download.getDataFile();
            if (dataFile != null) {
                return new File(avatarDir, dataFile);
            }
        }
        return null;
    }

    public static File getAvatarLocation(TalkClientUpload upload) {
        String dataFile = upload.getDataFile();
        if (dataFile != null) {
            return new File(dataFile);
        }
        return null;
    }

    public static File getAttachmentLocation(TalkClientDownload download) {
        if (download.getState() == TalkClientDownload.State.COMPLETE) {
            File attachmentDir = getAttachmentDirectory();
            ensureDirectory(attachmentDir);
            String dataFile = download.getDataFile();
            if (dataFile != null) {
                return new File(attachmentDir, dataFile);
            }
        }
        return null;
    }

    public static File getAttachmentLocation(TalkClientUpload upload) {
        String dataFile = upload.getDataFile();
        if (dataFile != null) {
            return new File(dataFile);
        }
        return null;
    }

    public static void reinitializeXoClient() {
        if (sClient != null) {
            sClient.initialize(sClientHost);
        }
    }

    public static void registerForNextStart(Class clazz) {
        sStartupTasks.registerForNextStart(clazz);
    }

    /**
     * Starts a nearby session if not yet started.
     * Sets sIsNearbySessionRunning = true.
     */
    public static void startNearbySession(boolean force) {
        if (!sEnvironmentUpdater.isEnabled() || force) {
            try {
                sEnvironmentUpdater.startEnvironmentTracking();
                sIsNearbySessionRunning = true;
            } catch (EnvironmentUpdaterException e) {
                sLog.error("Error when starting EnvironmentUpdater: ", e);
            }
        }
    }

    /**
     * Stops current nearby session if running.
     */
    public static void suspendNearbySession() {
        if (sEnvironmentUpdater.isEnabled()) {
            sIsNearbySessionRunning = true;
            sEnvironmentUpdater.stopEnvironmentTracking();
        }
    }

    /**
     * Stops current nearby session if running.
     */
    public static void stopNearbySession() {
        if (sIsNearbySessionRunning) {
            suspendNearbySession();
            sIsNearbySessionRunning = false;
        }
    }

    public static void enterBackgroundMode() {
        // set presence to inactive
        sClient.setClientConnectionStatus(TalkPresence.CONN_STATUS_BACKGROUND);

        // suspend nearby environment
        suspendNearbySession();

        sLog.info("Entered background mode");
    }

    public static void enterForegroundMode() {
        // set presence to active
        sClient.setClientConnectionStatus(TalkPresence.CONN_STATUS_ONLINE);

        // wake up suspended nearby session
        if (sIsNearbySessionRunning) {
            startNearbySession(false);
        }

        sLog.info("Entered foreground mode");
    }

    public static void enterBackgroundActiveMode() {
        sLog.info("Entered background active mode");
    }

    public static ScheduledExecutorService getExecutor() {
        return sExecutor;
    }

    public static XoAndroidClient getXoClient() {
        return sClient;
    }

    public static XoAndroidClientConfiguration getConfiguration() {
        return sConfiguration;
    }

    public static XoSoundPool getXoSoundPool() {
        return sSoundPool;
    }

    public static EnvironmentUpdater getEnvironmentUpdater() {
        return sEnvironmentUpdater;
    }

    public static File getExternalStorage() {
        return sExternalStorage;
    }

    public static Thread.UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return sUncaughtExceptionHandler;
    }

    public static DisplayImageOptions getImageOptions() {
        return sImageOptions;
    }

    public static File getCacheStorage() {
        return sInternalCacheStorage;
    }

    public static File getAttachmentDirectory() {
        return new File(sExternalStorage, sConfiguration.getAttachmentsDirectory());
    }

    public static File getBackupDirectory() {
        return new File(sExternalStorage, sConfiguration.getBackupDirectory());
    }

    public static File getEncryptedDownloadDirectory() {
        return new File(sInternalStorage, DOWNLOADS_DIRECTORY);
    }

    public static File getGeneratedDirectory() {
        return new File(sInternalStorage, GENERATED_DIRECTORY);
    }

    public static File getAvatarDirectory() {
        return new File(sExternalStorage, sConfiguration.getAvatarsDirectory());
    }

    public static File getThumbnailDirectory() {
        return new File(sInternalStorage, THUMBNAILS_DIRECTORY);
    }

    public static String getAppPackageName() {
        return sPackageName;
    }

    public static ScheduledExecutorService getIncomingExecutor() {
        return sIncomingExecutor;
    }

    private static void renameHoccerClassicAttachmentDirectory() {
        if (sExternalStorage.list() != null) {
            if (Arrays.asList(sExternalStorage.list()).contains(HOCCER_CLASSIC_ATTACHMENTS_DIRECTORY)) {

                File classicDir = new File(sExternalStorage, HOCCER_CLASSIC_ATTACHMENTS_DIRECTORY);

                if (classicDir.exists()) {
                    File tempDir = new File(sExternalStorage, "_" + HOCCER_CLASSIC_ATTACHMENTS_DIRECTORY);
                    classicDir.renameTo(tempDir);
                    tempDir.renameTo(getAttachmentDirectory());
                }
            }
        }
    }
}
