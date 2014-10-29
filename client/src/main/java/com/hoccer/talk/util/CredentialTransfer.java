package com.hoccer.talk.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.client.model.TalkClientSelf;
import com.hoccer.talk.crypto.CryptoJSON;
import org.apache.log4j.Logger;

/**
 * Provides utility methods to get or set credentials from/to XoClient.
 * The credentials are stored/restored via a Json structure and can be encrypted.
 */
public class CredentialTransfer {

    private static final Logger LOG = Logger.getLogger(CredentialTransfer.class);

    private final XoClient mClient;

    private ObjectMapper mObjectMapper = new ObjectMapper();

    public CredentialTransfer(final XoClient client) {
        this(client, new ObjectMapper());
    }

    public CredentialTransfer(final XoClient client, final ObjectMapper objectMapper) {
        mClient = client;
        mObjectMapper = objectMapper;
    }

    public byte[] getCredentialsAsEncryptedJson(final String containerPassword) {
        final String credentials = getCredentialsAsJsonString();
        try {
            return CryptoJSON.encrypt(credentials.getBytes("UTF-8"), containerPassword, "credentials");
        } catch (final Exception e) {
            LOG.error("getCredentialsAsEncryptedJson", e);
            return null;
        }
    }

    public String getCredentialsAsJsonString() {
        try {
            final ObjectNode credentialsNode = getCredentialsAsJson();
            if (credentialsNode != null) {
                return mObjectMapper.writeValueAsString(credentialsNode);
            }
        } catch (final Exception e) {
            LOG.error("getCredentialsAsJsonString", e);
        }
        return null;
    }

    public ObjectNode getCredentialsAsJson() throws RuntimeException {
        try {
            final TalkClientContact selfContact = mClient.getSelfContact();
            final String clientId = selfContact.getClientId();
            final TalkClientSelf self = selfContact.getSelf();

            final ObjectNode node = mObjectMapper.createObjectNode();
            node.put("password", self.getSrpSecret());
            node.put("salt", self.getSrpSalt());
            node.put("clientId", clientId);
            node.put("clientName", selfContact.getName());
            return node;
        } catch (final Exception e) {
            LOG.error("getCredentialsAsJson", e);
            return null;
        }
    }

    public boolean setCredentialsFromEncryptedJson(final byte[] jsonContainer, final String containerPassword) {
        try {
            final byte[] credentials = CryptoJSON.decrypt(jsonContainer, containerPassword, "credentials");
            return setCredentialsFromJsonString(new String(credentials, "UTF-8"));
        } catch (final Exception e) {
            LOG.error("setCredentialsFromEncryptedJson", e);
        }
        return false;
    }

    public boolean setCredentialsFromJsonString(final String jsonCredentials) {
        try {
            final ObjectMapper jsonMapper = new ObjectMapper();
            final JsonNode rootNode = jsonMapper.readTree(jsonCredentials);
            if (rootNode != null && rootNode.isObject()) {
                return setCredentialsFromJson(rootNode);
            } else {
                LOG.error("setCredentialsFromJsonString: Not a json object");
            }
        } catch (final Exception e) {
            LOG.error("setCredentialsFromJsonString", e);
        }
        return false;
    }

    public boolean setCredentialsFromJson(final JsonNode rootNode) {
        try {
            final JsonNode passwordNode = rootNode.get("password");
            if (passwordNode == null) {
                throw new Exception("Missing password node");
            }
            final JsonNode saltNode = rootNode.get("salt");
            if (saltNode == null) {
                throw new Exception("Missing salt node");
            }
            final JsonNode clientIdNode = rootNode.get("clientId");
            if (clientIdNode == null) {
                throw new Exception("Missing clientId node");
            }
            final JsonNode clientNameNode = rootNode.get("clientName");
            if (clientNameNode == null) {
                throw new Exception("Missing clientName node");
            }

            // update credentials
            final TalkClientContact selfContact = mClient.getSelfContact();
            final TalkClientSelf self = selfContact.getSelf();
            self.provideCredentials(saltNode.asText(), passwordNode.asText());

            // update client id
            selfContact.updateSelfRegistered(clientIdNode.asText());

            // update client name
            selfContact.getClientPresence().setClientName(clientNameNode.asText());

            final XoClientDatabase database = mClient.getDatabase();

            // save credentials and contact
            database.saveCredentials(self);
            database.savePresence(selfContact.getClientPresence());
            database.saveContact(selfContact);

            // remove contacts + groups from DB
            database.eraseAllRelationships();
            database.eraseAllClientContacts();
            database.eraseAllGroupMemberships();
            database.eraseAllGroupContacts();

            mClient.reconnect("Credentials imported.");

            return true;
        } catch (final Exception e) {
            LOG.error("setCredentialsFromJson", e);
        }

        return false;
    }
}
