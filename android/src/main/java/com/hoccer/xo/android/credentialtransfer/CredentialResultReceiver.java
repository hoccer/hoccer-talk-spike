package com.hoccer.xo.android.credentialtransfer;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoccer.talk.crypto.CryptoJSON;
import com.hoccer.talk.util.Credentials;
import com.hoccer.xo.android.XoApplication;
import org.apache.log4j.Logger;

import java.util.concurrent.TimeUnit;

/**
 * Custom receiver processes the received credentials and timeouts.
 */
public class CredentialResultReceiver extends ResultReceiver {

    private static final Logger LOG = Logger.getLogger(CredentialResultReceiver.class);

    private static final int CREDENTIAL_RECEIVE_TIMEOUT_SECONDS = 15;

    CredentialImporter.CredentialImportListener mListener;
    private boolean mAnswerReceived = false;

    public CredentialResultReceiver(final CredentialImporter.CredentialImportListener listener) {
        super(new Handler());
        mListener = listener;

        // schedule invoke callback in case of timeout if no answer has been received
        XoApplication.getExecutor().schedule(new Runnable() {
            @Override
            public void run() {
                if (!mAnswerReceived) {
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
        if (!mAnswerReceived) {
            mAnswerReceived = true;

            if (resultCode == Activity.RESULT_OK && resultData != null) {
                try {
                    final byte[] encryptedPayload = resultData.getByteArray(CredentialExportService.EXTRA_PAYLOAD);
                    final byte[] payloadBytes = CryptoJSON.decrypt(encryptedPayload, CredentialExportService.CREDENTIALS_ENCRYPTION_PASSWORD, CredentialExportService.CREDENTIALS_CONTENT_TYPE);

                    final ObjectMapper objectMapper = new ObjectMapper();
                    final JsonNode rootNode = objectMapper.readTree(new String(payloadBytes, CredentialExportService.PAYLOAD_CHARSET));

                    final JsonNode credentialsNode = rootNode.get(CredentialExportService.CREDENTIALS_NODE_NAME);
                    if (credentialsNode == null) {
                        mListener.onFailure();
                        return;
                    }

                    final Credentials credentials = Credentials.fromJsonNode(credentialsNode);
                    if (credentials == null) {
                        mListener.onFailure();
                        return;
                    }

                    final JsonNode contactCountNode = rootNode.get(CredentialExportService.CONTACT_COUNT_FIELD_NAME);
                    if (contactCountNode == null) {
                        mListener.onFailure();
                        return;
                    }

                    final int contactCount = contactCountNode.asInt();

                    mListener.onSuccess(credentials, contactCount);
                    return;
                } catch (final Exception e) {
                    LOG.error("onReceiveResult", e);
                }
            }
            mListener.onFailure();
        }
    }
}
