package com.hoccer.talk.crypto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.codec.binary.Base64;

import java.io.IOException;

/**
 * Provides utility methods to encrypt/decrypt a given string in/from a encrypted JSON container.
 */
public class CryptoJSON {

    /**
     * Creates a JSON container containing the given content.
     *
     * @param content     The content to encrypt.
     * @param password    The password used for encryption.
     * @param contentType The content type information. This information is added to the container.
     * @return The byte array of the container.
     * @throws Exception
     */
    public static byte[] encrypt(final byte[] content, final String password, final String contentType) throws Exception {
        final byte[] salt = AESCryptor.makeRandomBytes(32);
        final byte[] key = AESCryptor.make256BitKeyFromPassword_PBKDF2WithHmacSHA256(password, salt);
        final byte[] cipherText = AESCryptor.encrypt(key, null, content);
        final String cipherTextString = new String(Base64.encodeBase64(cipherText)); // TODO: workaround for library BASE64 < version 1.4
        final String saltString = new String(Base64.encodeBase64(salt)); // TODO: workaround for library BASE64 < version 1.4

        final ObjectMapper jsonMapper = new ObjectMapper();
        final ObjectNode rootNode = jsonMapper.createObjectNode();
        rootNode.put("container", "AESPBKDF2");
        rootNode.put("contentType", contentType);
        rootNode.put("salt", saltString);
        rootNode.put("ciphered", cipherTextString);
        final String jsonString = jsonMapper.writeValueAsString(rootNode);
        return jsonString.getBytes("UTF-8");
    }

    /**
     * Encrypts the content of the given encrypted container.
     *
     * @param container           The byte array of the container.
     * @param password            The password used to decrypt the container.
     * @param expectedContentType The expected content type. Needs to match with the container content type.
     * @return The byte array of the decrypted content.
     * @throws Exception
     */
    public static byte[] decrypt(final byte[] container, final String password, final String expectedContentType) throws IOException, DecryptionException {
        final ObjectMapper jsonMapper = new ObjectMapper();
        final JsonNode json = jsonMapper.readTree(container);

        if (json == null || !json.isObject()) {
            throw new IOException("parseEncryptedContainer: not a json object");
        }
        final JsonNode containerNode = json.get("container");
        if (containerNode == null || !"AESPBKDF2".equals(containerNode.asText())) {
            throw new IOException("parseEncryptedContainer: bad or missing container identifier");
        }
        final JsonNode contentTypeNode = json.get("contentType");
        if (contentTypeNode == null || !contentTypeNode.asText().equals(expectedContentType)) {
            throw new IOException("parseEncryptedContainer: wrong or missing contentType");
        }
        final JsonNode saltNode = json.get("salt");
        if (saltNode == null) {
            throw new IOException("parseEncryptedContainer: wrong or missing salt");
        }
        final byte[] salt = Base64.decodeBase64(saltNode.asText().getBytes()); // TODO: workaround for library BASE64 < version 1.4
        if (salt.length != 32) {
            throw new IOException("parseEncryptedContainer: bad salt length (must be 32)");
        }
        final JsonNode cipheredNode = json.get("ciphered");
        if (cipheredNode == null) {
            throw new IOException("parseEncryptedContainer: wrong or missing ciphered content");
        }

        final byte[] ciphered = Base64.decodeBase64(cipheredNode.asText().getBytes()); // TODO: workaround for library BASE64 < version 1.4
        if (ciphered == null) {
            throw new IOException("parseEncryptedContainer: ciphered content not base64");
        }

        try {
            final byte[] key = AESCryptor.make256BitKeyFromPassword_PBKDF2WithHmacSHA256(password, salt);
            return AESCryptor.decrypt(key, null, ciphered);
        } catch (Exception e) {
            throw new DecryptionException(e);
        }
    }

    public static class DecryptionException extends Exception {
        public DecryptionException(Exception e) {
            super(e.getMessage());
            initCause(e);
            setStackTrace(e.getStackTrace());
        }
    }
}
