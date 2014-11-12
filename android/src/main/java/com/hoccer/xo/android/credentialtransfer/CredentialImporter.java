package com.hoccer.xo.android.credentialtransfer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import com.hoccer.talk.util.Credentials;
import com.hoccer.xo.android.XoApplication;
import org.apache.log4j.Logger;

/**
 * Provides static credential import methods.
 */
public class CredentialImporter {

    private static final Logger LOG = Logger.getLogger(CredentialImporter.class);

    private static final int SUPPORTED_EXPORT_VERSION = 95;

    public static final String CHANGE_SRP_SECRET_PROPERTY = "change_srp_secret";

    /**
     * Checks whether the package defined in the configuration is installed on the device and support the
     * credential transfer.
     *
     * @param context Used to retrieve package information.
     * @return Whether the package is installed or not.
     * @see XoApplication#getConfiguration()#getCredentialImportPackage()
     */
    public static boolean isCredentialImportPackageInstalled(final Context context) {
        return getCredentialImportPackageInfo(context) != null;
    }

    /**
     * Checks whether the package defined in the configuration is installed on the device and support the
     * credential transfer.
     *
     * @param context Used to retrieve package information.
     * @return Whether credential import is supported by the credential import package.
     * @see XoApplication#getConfiguration()#getCredentialImportPackage()
     */
    public static boolean isCredentialImportSupported(final Context context) {
        final PackageInfo packageInfo = getCredentialImportPackageInfo(context);
        if (packageInfo == null) {
            return false;
        }

        final int versionCode = packageInfo.versionCode;
        return versionCode >= SUPPORTED_EXPORT_VERSION;
    }

    /**
     * Returns the package info of the import package if it is found.
     *
     * @param context Used to retrieve package information.
     * @return The package info instance or null.
     */
    private static PackageInfo getCredentialImportPackageInfo(final Context context) {
        PackageInfo result = null;
        final String packageName = XoApplication.getConfiguration().getCredentialImportPackage();
        if (packageName != null) {
            try {
                result = context.getPackageManager().getPackageInfo(packageName, 0);
            } catch (final PackageManager.NameNotFoundException e) {
                // package not installed
            }
        }
        return result;
    }

    /**
     * Abstract callback class called upon success or failure of the credential import process.
     */
    public interface CredentialImportListener {
        public abstract void onSuccess(Credentials credentials, Integer contactCount);

        public abstract void onFailure();
    }

    /**
     * Initiates the import process and returns immediately.
     *
     * @param context  Used to send the import broadcast to the import package.
     * @param listener Gets called on success or failure.
     */
    public static void importCredentials(final Context context, final CredentialImportListener listener) {
        LOG.info("Try to import credentials");
        final String packageName = XoApplication.getConfiguration().getCredentialImportPackage();
        if (packageName != null) {
            final Intent intent = new Intent();
            final String className = CredentialTransferReceiver.class.getName();
            intent.setClassName(packageName, className);
            intent.setAction(CredentialExportService.INTENT_ACTION_FILTER);
            intent.putExtra(CredentialExportService.EXTRA_RECEIVER, new CredentialResultReceiver(listener));
            context.sendBroadcast(intent);
        }
    }

    /**
     * Sends a client disconnection intent to the other application.
     *
     * @param context Used to send the import broadcast to the import package.
     */
    public static void sendDisconnectRequestToImportPackageClient(final Context context) {
        LOG.info("Try to disconnect import package client");
        final String packageName = XoApplication.getConfiguration().getCredentialImportPackage();
        if (packageName != null) {
            final Intent intent = new Intent();
            final String className = CredentialTransferReceiver.class.getName();
            intent.setClassName(packageName, className);
            intent.setAction(DisconnectService.INTENT_ACTION_FILTER);
            context.sendBroadcast(intent);
        }
    }

    /**
     * Sets a flag in the shared preferences to change the srp secret the next time the client logs in.
     *
     * @param context Needed to get the shared preferences.
     * @see com.hoccer.xo.android.credentialtransfer.SrpChangeListener
     */
    public static void setSrpChangeOnNextLoginFlag(final Context context) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        final SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(CHANGE_SRP_SECRET_PROPERTY, true);
        editor.apply();
    }
}
