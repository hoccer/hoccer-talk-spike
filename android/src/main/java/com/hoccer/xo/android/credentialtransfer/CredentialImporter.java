package com.hoccer.xo.android.credentialtransfer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import com.hoccer.xo.android.XoApplication;
import org.apache.log4j.Logger;

import java.util.concurrent.TimeUnit;

/**
 * Provides static credential import methods.
 */
public class CredentialImporter {

    private static final Logger LOG = Logger.getLogger(CredentialImporter.class);

    private static final int CREDENTIAL_RECEIVE_TIMEOUT_SECONDS = 15;

    /**
     * Checks wether the package defined in the configuraiton is installed on the device and support the
     * credential transfer.
     * @see XoApplication#getConfiguration()#getCredentialImportPackage()
     * @param context Used to retrieve package information.
     * @return Wether the package is installed or not.
     */
    public static boolean isCredentialImportPackageInstalled(final Context context) {
        return getCredentialImportPackageInfo(context) != null;
    }

    /**
     * Checks wether the package defined in the configuraiton is installed on the device and support the
     * credential transfer.
     * @see XoApplication#getConfiguration()#getCredentialImportPackage()
     * @param context Used to retrieve package information.
     * @return Wether credential import is supported by the credential import package.
     */
    public static boolean isCredentialImportSupported(final Context context) {
        final PackageInfo packageInfo = getCredentialImportPackageInfo(context);
        if(packageInfo == null) {
            return false;
        }

        final int versionCode = packageInfo.versionCode;
        return versionCode >= 89;
    }

    /*
     * Abstract callback class called upon success or failure of the credential import process.
     */
    public interface CredentialImportListener {
        public abstract void onSuccess();
        public abstract void onFailure();
    }

    /**
     * Initiates the import process and returns immediately.
     * @param context Used to send the import broadcast to the import package.
     * @param listener Gets called on success or failure.
     */

    public static void importCredentials(final Context context, final CredentialImportListener listener) {
        LOG.info("Try to import credentials");
        final String packageName = XoApplication.getConfiguration().getCredentialImportPackage();
        if (packageName != null) {
            final Intent intent = new Intent();
            final String className = CredentialExportReceiver.class.getName();
            intent.setClassName(packageName, className);
            intent.setAction(packageName + ".action.EXPORT_DATA");
            intent.putExtra("receiver", new CredentialResultReceiver(new Handler(), listener));
            context.sendBroadcast(intent);
        }
    }

    /**
     * Returns the package info of the import package if it is found.
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
     * Custom receiver processes the received credentials and timeouts.
     */
    private static class CredentialResultReceiver extends ResultReceiver {

        CredentialImportListener mListener;
        private boolean mAnswerReceived = false;

        public CredentialResultReceiver(final Handler handler, final CredentialImportListener listener) {
            super(handler);
            mListener = listener;

            // schedule invoke callback in case of timeout if no answer has been received
            XoApplication.getExecutor().schedule(new Runnable() {
                @Override
                public void run() {
                    if(!mAnswerReceived) {
                        mAnswerReceived = true;
                        mListener.onFailure();
                    }
                }
            }, CREDENTIAL_RECEIVE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        @Override
        protected void onReceiveResult(final int resultCode, final Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);

            // handle first answer only and avoid execution after timeout
            if(!mAnswerReceived) {
                mAnswerReceived = true;

                if (resultCode == Activity.RESULT_OK && resultData != null) {
                    final String credentialsJson = resultData.getString(CredentialExportService.EXTRA_RESULT_CREDENTIALS_JSON);

                    // TODO save new credentials, renew srp secret and restart

                    mListener.onSuccess();
                } else {
                    // TODO handle error case gracefully

                    mListener.onFailure();
                }
            }
        }
    }
}
