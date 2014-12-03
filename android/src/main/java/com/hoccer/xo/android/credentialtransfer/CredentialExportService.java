package com.hoccer.xo.android.credentialtransfer;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hoccer.talk.crypto.CryptoJSON;
import com.hoccer.talk.model.TalkGroupMembership;
import com.hoccer.talk.model.TalkRelationship;
import com.hoccer.talk.util.Credentials;
import com.hoccer.xo.android.XoApplication;
import org.apache.log4j.Logger;

/**
 * Exports the credentials and additional account information.
 */
public class CredentialExportService extends IntentService {

    private static final Logger LOG = Logger.getLogger(CredentialExportService.class);

    public static final String INTENT_ACTION_FILTER = "com.hoccer.android.action.EXPORT_DATA";

    public static final String EXTRA_RECEIVER = "receiver";

    public static final String EXTRA_PAYLOAD = "payload";

    public static final String CREDENTIALS_NODE_NAME = "credentials";

    public static final String CONTACT_COUNT_FIELD_NAME = "contact_count";

    public static final String CREDENTIALS_ENCRYPTION_PASSWORD = "4brj3paAr8D2Qvgw";

    public static final String CREDENTIALS_CONTENT_TYPE = "export_payload";

    public static final String PAYLOAD_CHARSET = "UTF-8";

    public CredentialExportService() {
        super("DataExportService");
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        if (!intent.hasExtra(EXTRA_RECEIVER)) {
            LOG.warn("Export request received from '" + intent.getPackage() + "' but no receiver provided.");
            return;
        }

        final ResultReceiver resultReceiver = intent.getParcelableExtra(EXTRA_RECEIVER);

        if (resultReceiver == null) {
            LOG.warn("Export request received from '" + intent.getPackage() + "' but receiver provided is null.");
            return;
        }

        exportPayload(resultReceiver);
    }

    private static void exportPayload(final ResultReceiver resultReceiver) {
        try {
            LOG.info("Exporting payload");

            final byte[] payload = createPayload();
            if (payload != null) {
                final Bundle bundle = new Bundle();
                bundle.putByteArray(EXTRA_PAYLOAD, payload);

                // send payload
                resultReceiver.send(Activity.RESULT_OK, bundle);
                return;
            }
        } catch (final Exception e) {
            LOG.error("exportPayload", e);
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
                final ObjectNode credentialNode = rootNode.putObject(CREDENTIALS_NODE_NAME);
                credentials.toJsonNode(credentialNode);

                // write friend contact and joined group count
                final int clients = XoApplication.getXoClient().getDatabase().findClientContactsByState(TalkRelationship.STATE_FRIEND).size();
                final int groups = XoApplication.getXoClient().getDatabase().findGroupContactsByMembershipState(TalkGroupMembership.STATE_JOINED).size();
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
