package com.hoccer.talk.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hoccer.talk.crypto.CryptoJSON;
import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;


/**
 * Wraps the credentials exported from XoClient and provides from/to Json conversion.
 */
public class Credentials {

    private static final Logger LOG = Logger.getLogger(Credentials.class);

    public static final String CREDENTIALS_CONTENT_TYPE = "credentials";
    private static final int IOS_ASCII_PASSWORD_LENGTH = 23;

    private final String mClientId;
    private final String mPassword;
    private final String mSalt;

    private final String mClientName;

    private final Long mTimeStamp;

    public Credentials(String clientId, String password, String salt, String clientName, Long timestamp) {
        mClientId = clientId;
        mPassword = password;
        mSalt = salt;
        mClientName = clientName;
        mTimeStamp = timestamp;
    }

    public String getClientId() {
        return mClientId;
    }

    @Nullable
    public String getClientName() {
        return mClientName;
    }

    @Nullable
    public Long getTimeStamp() {
        return mTimeStamp;
    }

    public String getPassword() {
        return mPassword;
    }

    public String getSalt() {
        return mSalt;
    }

    public byte[] toEncryptedBytes(String password) {
        return toEncryptedBytes(password, new ObjectMapper());
    }

    public byte[] toEncryptedBytes(String password, ObjectMapper mapper) {
        try {
            String credentialsString = toString(mapper);
            if (credentialsString != null) {
                return CryptoJSON.encrypt(credentialsString.getBytes("UTF-8"), password, CREDENTIALS_CONTENT_TYPE);
            }
        } catch (Exception e) {
            LOG.error("toEncryptedBytes", e);
        }
        return null;
    }

    @Override
    public String toString() {
        return toString(new ObjectMapper());
    }

    public String toString(ObjectMapper mapper) {
        try {
            ObjectNode credentialsNode = mapper.createObjectNode();
            if (toJsonNode(credentialsNode)) {
                return mapper.writeValueAsString(credentialsNode);
            }
        } catch (Exception e) {
            LOG.error("toJsonString", e);
        }

        return null;
    }

    public boolean toJsonNode(ObjectNode node) {
        try {
            node.put("password", mPassword);
            node.put("salt", mSalt);
            node.put("clientId", mClientId);

            if (mClientName != null) {
                node.put("clientName", mClientName);
            }

            if (mTimeStamp != null) {
                node.put("credentialsDate", mTimeStamp);
            }

            return true;
        } catch (Exception e) {
            LOG.error("toJsonNode", e);
            return false;
        }
    }

    public static Credentials fromEncryptedBytes(byte[] encryptedCredentials, String password) {
        return fromEncryptedBytes(encryptedCredentials, password, new ObjectMapper());
    }

    private static Credentials fromEncryptedBytes(byte[] encryptedCredentials, String password, ObjectMapper mapper) {
        try {
            byte[] credentialsBytes = CryptoJSON.decrypt(encryptedCredentials, password, CREDENTIALS_CONTENT_TYPE);
            return fromJsonString(new String(credentialsBytes, "UTF-8"));
        } catch (Exception e) {
            LOG.error("fromEncryptedBytes", e);
            return null;
        }
    }

    public static Credentials fromJsonString(String jsonCredentialsString) {
        return fromJsonString(jsonCredentialsString, new ObjectMapper());
    }

    public static Credentials fromJsonString(String jsonCredentialsString, ObjectMapper mapper) {
        try {
            JsonNode jsonCredentials = mapper.readTree(jsonCredentialsString);
            if (jsonCredentials != null && jsonCredentials.isObject()) {
                return fromJsonNode(jsonCredentials);
            } else {
                LOG.error("fromJsonString: Not a json object");
            }
        } catch (Exception e) {
            LOG.error("fromJsonString", e);
        }
        return null;
    }

    public static Credentials fromJsonNode(JsonNode jsonCredentials) {
        JsonNode clientIdNode = jsonCredentials.get("clientId");
        if (clientIdNode == null) {
            LOG.error("Missing clientId node");
            return null;
        }

        JsonNode passwordNode = jsonCredentials.get("password");
        if (passwordNode == null) {
            LOG.error("Missing password node");
            return null;
        }
        String passwordText = convertToHexIfASCII(passwordNode.asText());

        JsonNode saltNode = jsonCredentials.get("salt");
        if (saltNode == null) {
            LOG.error("Missing salt node");
            return null;
        }

        String clientName = null;
        JsonNode clientNameNode = jsonCredentials.get("clientName");
        if (clientNameNode != null) {
            clientName = clientNameNode.asText();
        }

        Long timestamp = null;
        JsonNode timestampNode = jsonCredentials.get("credentialsDate");
        if (timestampNode != null) {
            timestamp = Long.parseLong(timestampNode.asText());
        }

        return new Credentials(clientIdNode.asText(), passwordText, saltNode.asText(), clientName, timestamp);
    }

    private static String convertToHexIfASCII(String byteString) {
        if (byteString.length() == IOS_ASCII_PASSWORD_LENGTH) {
            return new String(Hex.encodeHex(byteString.getBytes()));
        } else {
            return byteString;
        }
    }
}
