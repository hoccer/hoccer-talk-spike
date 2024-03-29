package com.hoccer.xo.android;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.Environment;
import android.support.multidex.MultiDexApplication;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hoccer.talk.client.IXoClientHost;
import com.hoccer.talk.client.model.TalkClientDownload;
import com.hoccer.talk.client.model.TalkClientUpload;
import com.hoccer.xo.android.credentialtransfer.SrpChangeListener;
import com.hoccer.xo.android.polling.Polling;
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
public class XoApplication extends MultiDexApplication {

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
    private ScheduledExecutorService mExecutor;

    // global executor for incoming connections
    private static ScheduledExecutorService sIncomingExecutor;

    private XoAndroidClient mClient;
    private static XoAndroidClientConfiguration sConfiguration;
    private static SoundPool sSoundPool;
    private static DisplayImageOptions sImageOptions;

    private static StartupTasks sStartupTasks;

    private static XoApplication sInstance;
    private CrashMonitor mCrashMonitor;

    public static XoApplication get() {
        return sInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        sInstance = this;
        mCrashMonitor = CrashMonitor.get(this);

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

        Thread.setDefaultUncaughtExceptionHandler(mCrashMonitor);

        // log storage roots
        sLog.info("internal storage at " + sInternalStorage);
        sLog.info("external storage at " + sExternalStorage);

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
        ensureNoMedia(getAvatarDirectory());
        ensureDirectory(getGeneratedDirectory());
        ensureNoMedia(getGeneratedDirectory());
        ensureDirectory(getThumbnailDirectory());
        ensureDirectory(getEncryptedDownloadDirectory());
        ensureNoMedia(getEncryptedDownloadDirectory());

        // create executor
        sLog.info("creating background executor");
        ThreadFactoryBuilder tfb = new ThreadFactoryBuilder();
        tfb.setNameFormat("client-%d");
        tfb.setUncaughtExceptionHandler(mCrashMonitor);
        mExecutor = Executors.newScheduledThreadPool(CLIENT_THREAD_COUNT, tfb.build());
        ThreadFactoryBuilder tfb2 = new ThreadFactoryBuilder();
        tfb2.setNameFormat("receiving client-%d");
        tfb2.setUncaughtExceptionHandler(mCrashMonitor);
        sIncomingExecutor = Executors.newScheduledThreadPool(CLIENT_THREAD_COUNT, tfb2.build());

        // create client instance
        sLog.info("creating client");
        IXoClientHost clientHost = new XoAndroidClientHost(this);
        mClient = new XoAndroidClient(clientHost, sConfiguration);
        mClient.setAvatarDirectory(getAvatarDirectory().toString());
        mClient.setRelativeAvatarDirectory(sConfiguration.getAvatarsDirectory());
        mClient.setAttachmentDirectory(getAttachmentDirectory().toString());
        mClient.setRelativeAttachmentDirectory(sConfiguration.getAttachmentsDirectory());
        mClient.setEncryptedDownloadDirectory(getEncryptedDownloadDirectory().toString());
        mClient.setExternalStorageDirectory(sExternalStorage.getAbsolutePath());

        // add srp secret change listener
        mClient.registerStateListener(new SrpChangeListener(this));

        if (isFirstConnectionAfterCrashOrUpdate()) {
            sLog.debug("First connection after crash or update. Full sync triggered.");
            mClient.setFullSyncRequired(true);
            mCrashMonitor.saveCrashState(false);
        }
        // create sound pool instance
        sSoundPool = new SoundPool(this);

        sStartupTasks = new StartupTasks(this);
        sStartupTasks.executeRegisteredTasks();

        Polling.update(this);
    }

    private boolean isFirstConnectionAfterCrashOrUpdate() {
        return UpdateHelper.getInstance(this).isApplicationUpdated() || UpdateHelper.getInstance(this).isFreshInstall() || mCrashMonitor.isCrashedBefore();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        sLog.warn("Received onTrimMemory(" + level + ").");
    }

    public static void ensureDirectory(File directory) {
        if (!directory.exists()) {
            sLog.info("creating directory " + directory);
            if (!directory.mkdirs()) {
                sLog.info("Error creating directory " + directory);
            }
        }
    }

    private static void ensureNoMedia(File directory) {
        if (directory.exists()) {
            File noMedia = new File(directory, ".nomedia");
            if (!noMedia.exists()) {
                sLog.info("creating noMedia marker " + noMedia);
                try {
                    if (!noMedia.createNewFile()) {
                        sLog.info("Error creating directory " + noMedia);
                    }
                } catch (IOException e) {
                    sLog.error("error creating " + noMedia, e);
                }
            }
        }
    }

    public static File getAvatarLocation(TalkClientDownload download) {
        if (download.getState() == TalkClientDownload.State.COMPLETE) {
            File avatarDir = getAvatarDirectory();
            ensureDirectory(avatarDir);
            String filePath = download.getFilePath();
            if (filePath != null) {
                return new File(avatarDir, filePath);
            }
        }
        return null;
    }

    public static File getAvatarLocation(TalkClientUpload upload) {
        String filePath = upload.getFilePath();
        if (filePath != null) {
            return new File(filePath);
        }
        return null;
    }

    public static File getAttachmentLocation(TalkClientDownload download) {
        if (download.getState() == TalkClientDownload.State.COMPLETE) {
            File attachmentDir = getAttachmentDirectory();
            ensureDirectory(attachmentDir);
            String filePath = download.getFilePath();
            if (filePath != null) {
                return new File(attachmentDir, filePath);
            }
        }
        return null;
    }

    public static File getAttachmentLocation(TalkClientUpload upload) {
        String filePath = upload.getFilePath();
        if (filePath != null) {
            return new File(filePath);
        }
        return null;
    }

    public static void registerForNextStart(Class clazz) {
        sStartupTasks.registerForNextStart(clazz);
    }

    public ScheduledExecutorService getExecutor() {
        return mExecutor;
    }

    public XoAndroidClient getClient() {
        return mClient;
    }

    public static XoAndroidClientConfiguration getConfiguration() {
        return sConfiguration;
    }

    public static SoundPool getSoundPool() {
        return sSoundPool;
    }

    public static File getExternalStorage() {
        return sExternalStorage;
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

    public static void restartApplication() {
        android.os.Process.killProcess(android.os.Process.myPid());
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