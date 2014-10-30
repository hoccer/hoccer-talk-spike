package com.hoccer.talk.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hoccer.talk.crypto.CryptoJSON;
import org.apache.log4j.Logger;

/**
 * Wraps the credentials exported from XoClient and provides from/to Json conversion.
 */
public class Credentials {

    public static final String CREDENTIALS_CONTENT_TYPE = "credentials";

    private static final Logger LOG = Logger.getLogger(Credentials.class);

    private final String mClientId;

    private final String mClientName;

    private final String mPassword;

    private final String mSalt;

    public Credentials(final String clientId, final String clientName, final String password, final String salt) {
        mClientId = clientId;
        mClientName = clientName;
        mPassword = password;
        mSalt = salt;
    }

    public String getClientId() {
        return mClientId;
    }

    public String getClientName() {
        return mClientName;
    }

    public String getPassword() {
        return mPassword;
    }

    public String getSalt() {
        return mSalt;
    }

    public byte[] toEncryptedBytes(final String password) {
        return toEncryptedBytes(password, new ObjectMapper());
    }

    public byte[] toEncryptedBytes(final String password, final ObjectMapper mapper) {
        try {
            final String credentialsString = toString(mapper);
            if (credentialsString != null) {
                return CryptoJSON.encrypt(credentialsString.getBytes("UTF-8"), password, CREDENTIALS_CONTENT_TYPE);
            }
        } catch (final Exception e) {
            LOG.error("toEncryptedBytes", e);
        }
        return null;
    }

    @Override
    public String toString() {
        return toString(new ObjectMapper());
    }

    public String toString(final ObjectMapper mapper) {
        try {
            final ObjectNode credentialsNode = toJson(mapper);
            if (credentialsNode != null) {
                return mapper.writeValueAsString(credentialsNode);
            }
        } catch (final Exception e) {
            LOG.error("toJsonString", e);
        }

        return null;
    }

    public ObjectNode toJson() {
        return toJson(new ObjectMapper());
    }

    public ObjectNode toJson(final ObjectMapper mapper) {
        try {
            final ObjectNode node = mapper.createObjectNode();
            node.put("password", mPassword);
            node.put("salt", mSalt);
            node.put("clientId", mClientId);
            node.put("clientName", mClientName);
            return node;
        } catch (final Exception e) {
            LOG.error("toJson", e);
            return null;
        }
    }

    public static Credentials fromEncryptedBytes(final byte[] encryptedCredentials, final String password) {
        return fromEncryptedBytes(encryptedCredentials, password, new ObjectMapper());
    }

    private static Credentials fromEncryptedBytes(final byte[] encryptedCredentials, final String password, final ObjectMapper mapper) {
        try {
            final byte[] credentialsBytes = CryptoJSON.decrypt(encryptedCredentials, password, CREDENTIALS_CONTENT_TYPE);
            return fromJsonString(new String(credentialsBytes, "UTF-8"));
        } catch (final Exception e) {
            LOG.error("fromEncryptedBytes", e);
            return null;
        }
    }

    public static Credentials fromJsonString(final String jsonCredentialsString) {
        return fromJsonString(jsonCredentialsString, new ObjectMapper());
    }

    public static Credentials fromJsonString(final String jsonCredentialsString, final ObjectMapper mapper) {
        try {
            final JsonNode jsonCredentials = mapper.readTree(jsonCredentialsString);
            if (jsonCredentials != null && jsonCredentials.isObject()) {
                return fromJson(jsonCredentials);
            } else {
                LOG.error("fromJsonString: Not a json object");
            }
        } catch (final Exception e) {
            LOG.error("fromJsonString", e);
        }
        return null;
    }

    public static Credentials fromJson(final JsonNode jsonCredentials) {
        return fromJson(jsonCredentials, new ObjectMapper());
    }

    public static Credentials fromJson(final JsonNode jsonCredentials, final ObjectMapper mapper) {
        final JsonNode clientIdNode = jsonCredentials.get("clientId");
        if (clientIdNode == null) {
            LOG.error("Missing clientId node");
            return null;
        }

        final JsonNode clientNameNode = jsonCredentials.get("clientName");
        if (clientNameNode == null) {
            LOG.error("Missing clientName node");
            return null;
        }

        final JsonNode passwordNode = jsonCredentials.get("password");
        if (passwordNode == null) {
            LOG.error("Missing password node");
            return null;
        }

        final JsonNode saltNode = jsonCredentials.get("salt");
        if (saltNode == null) {
            LOG.error("Missing salt node");
            return null;
        }

        return new Credentials(clientIdNode.asText(), clientNameNode.asText(), passwordNode.asText(), saltNode.asText());
    }
}
