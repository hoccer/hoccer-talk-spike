package com.hoccer.xo.android.credentialtransfer;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hoccer.talk.crypto.CryptoJSON;
import com.hoccer.talk.model.TalkGroupMember;
import com.hoccer.talk.model.TalkRelationship;
import com.hoccer.talk.util.Credentials;
import com.hoccer.xo.android.XoApplication;
import org.apache.log4j.Logger;

/**
 * Exports the credentials.
 */
public class CredentialExportService extends IntentService {

    private static final Logger LOG = Logger.getLogger(CredentialExportService.class);

    public static final String INTENT_ACTION_FILTER = "com.hoccer.android.action.EXPORT_DATA";

    public static final String EXTRA_RESULT_CREDENTIALS_JSON = "credentialsJson";

    public static final String CREDENTIALS_ENCRYPTION_PASSWORD = "4brj3paAr8D2Qvgw";

    public static final String CREDENTIALS_CONTENT_TYPE = "credentials";

    public static final String CREDENTIALS_FIELD_NAME = "credentials";

    public static final String CONTACT_COUNT_FIELD_NAME = "contact_count";

    public static final String PAYLOAD_CHARSET = "UTF-8";

    public CredentialExportService() {
        super("DataExportService");
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        if (!intent.hasExtra("receiver")) {
            LOG.warn("Export request received from '" + intent.getPackage() + "' but no receiver provided.");
            return;
        }

        final ResultReceiver resultReceiver = intent.getParcelableExtra("receiver");

        if (resultReceiver == null) {
            LOG.warn("Export request received from '" + intent.getPackage() + "' but receiver provided is null.");
            return;
        }

        exportCredentials(resultReceiver);
    }

    private static void exportCredentials(final ResultReceiver resultReceiver) {
        try {
            LOG.info("Exporting credentials");

            final byte[] payload = createPayload();
            if (payload != null) {
                final Bundle bundle = new Bundle();
                bundle.putByteArray(EXTRA_RESULT_CREDENTIALS_JSON, payload);

                // send payload
                resultReceiver.send(Activity.RESULT_OK, bundle);
                return;
            }
        } catch (final Exception e) {
            LOG.error("exportCredentials", e);
        }

        // send failure result
        resultReceiver.send(Activity.RESULT_CANCELED, null);
    }

    private static byte[] createPayload() {
        try {
            final Credentials credentials = XoApplication.getXoClient().exportCredentials();
            if(credentials != null) {
                final ObjectMapper mapper = new ObjectMapper();
                final ObjectNode rootNode = mapper.createObjectNode();

                // write credentials
                final ObjectNode credentialNode = rootNode.putObject(CREDENTIALS_FIELD_NAME);
                credentials.toJsonNode(credentialNode);

                // write friend contact and joined group count
                final int clients = XoApplication.getXoClient().getDatabase().findClientContactsByState(TalkRelationship.STATE_FRIEND).size();
                final int groups = XoApplication.getXoClient().getDatabase().findGroupContactsByState(TalkGroupMember.STATE_JOINED).size();
                rootNode.put(CONTACT_COUNT_FIELD_NAME, clients + groups);

                final String payloadString = mapper.writeValueAsString(rootNode);
                return CryptoJSON.encrypt(payloadString.getBytes(PAYLOAD_CHARSET), CREDENTIALS_ENCRYPTION_PASSWORD, CREDENTIALS_CONTENT_TYPE);
            }
        } catch (Exception e) {
            LOG.error("createPayload", e);
        }

        return null;
    }
}
