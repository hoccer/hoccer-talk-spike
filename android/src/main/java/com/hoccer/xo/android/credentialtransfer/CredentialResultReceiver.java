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
    private boolean mAnswerReceived;

    public CredentialResultReceiver(final CredentialImporter.CredentialImportListener listener) {
        super(new Handler());
        mListener = listener;

        // schedule invoke callback in case of timeout if no answer has been received
        XoApplication.get().getExecutor().schedule(new Runnable() {
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
                    final byte[] payloadBytes = decryptPayload(resultData);

                    final ObjectMapper objectMapper = new ObjectMapper();
                    final JsonNode rootNode = objectMapper.readTree(new String(payloadBytes, CredentialExportService.PAYLOAD_CHARSET));

                    try {
                        JsonNode contactCountNode = getContactCount(rootNode);
                        JsonNode credentialsNode = getCredentialsNode(rootNode);
                        Credentials credentials = getCredentials(credentialsNode);

                        mListener.onSuccess(credentials, contactCountNode.asInt());
                        return;
                    } catch (JsonNodeNotAvailableException e) {
                        mListener.onFailure();
                        return;
                    }
                } catch (final Exception e) {
                    LOG.error("onReceiveResult", e);
                }
            }
            mListener.onFailure();
        }
    }

    private static byte[] decryptPayload(Bundle resultData) throws Exception {
        final byte[] encryptedPayload = resultData.getByteArray(CredentialExportService.EXTRA_PAYLOAD);
        return CryptoJSON.decrypt(encryptedPayload, CredentialExportService.CREDENTIALS_ENCRYPTION_PASSWORD, CredentialExportService.CREDENTIALS_CONTENT_TYPE);
    }

    private JsonNode getContactCount(JsonNode rootNode) throws JsonNodeNotAvailableException {
        final JsonNode contactCountNode = rootNode.get(CredentialExportService.CONTACT_COUNT_FIELD_NAME);
        if (contactCountNode == null) {
            throw new JsonNodeNotAvailableException();
        }
        return contactCountNode;
    }

    private JsonNode getCredentialsNode(JsonNode rootNode) throws JsonNodeNotAvailableException {
        final JsonNode credentialsNode = rootNode.get(CredentialExportService.CREDENTIALS_NODE_NAME);
        if (credentialsNode == null) {
            throw new JsonNodeNotAvailableException();
        }
        return credentialsNode;
    }

    private Credentials getCredentials(JsonNode credentialsNode) throws JsonNodeNotAvailableException {
        final Credentials credentials = Credentials.fromJsonNode(credentialsNode);
        if (credentials == null) {
            throw new JsonNodeNotAvailableException();
        }
        return credentials;
    }

    private class JsonNodeNotAvailableException extends Throwable {}
}
