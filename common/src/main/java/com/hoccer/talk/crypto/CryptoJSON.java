/**
 * Created by pavel on 15.03.14.
 */

package com.hoccer.talk.crypto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.jcajce.provider.symmetric.AES;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;


public class CryptoJSON {

    public static byte[] encryptedContainer(byte[] plainText, String password, String contentType) throws Exception {
        byte[] salt = AESCryptor.makeRandomBytes(32);
        byte[] key = AESCryptor.make256BitKeyFromPassword_PBKDF2WithHmacSHA256(password, salt);
        byte[] cipherText = AESCryptor.encrypt(key,null,plainText);
        System.out.println("key=" + CryptoUtils.toHex(key));
        System.out.println("salt="+CryptoUtils.toHex(salt));
        System.out.println("ciphered="+CryptoUtils.toHex(cipherText));
        String cipherTextString = new String(Base64.encodeBase64(cipherText));
        String saltString = new String(Base64.encodeBase64(salt));

        ObjectMapper jsonMapper = new ObjectMapper();
        ObjectNode rootNode = jsonMapper.createObjectNode();
        rootNode.put("container", "AESPBKDF2");
        rootNode.put("contentType", contentType);
        rootNode.put("salt", saltString);
        rootNode.put("ciphered", cipherTextString);
        String jsonString = jsonMapper.writeValueAsString(rootNode);
        return jsonString.getBytes("UTF-8");
        //return jsonMapper.writeValueAsBytes(rootNode);
    }

    public static byte[] decryptedContainer(byte[] jsonContainer, String password, String contentType) throws Exception {

        ObjectMapper jsonMapper = new ObjectMapper();
        JsonNode json = jsonMapper.readTree(jsonContainer);
        if (json == null ||  !json.isObject()) {
            throw new Exception("parseEncryptedContainer: not a json object");
        }
        JsonNode container = json.get("container");
        //System.out.println("container="+container.asText());
        //System.out.println("container.isObject()="+container.isObject());
        //System.out.println("container equals="+("AESPBKDF2".equals(container.asText())));
        if (container == null || !"AESPBKDF2".equals(container.asText())) {
            throw new Exception("parseEncryptedContainer: bad or missing container identifier");
        }
        JsonNode contentTypeNode = json.get("contentType");
        if (contentTypeNode == null || !contentTypeNode.asText().equals(contentType)) {
            throw new Exception("parseEncryptedContainer: wrong or missing contentType");
        }
        JsonNode saltNode = json.get("salt");
        if (saltNode == null ) {
            throw new Exception("parseEncryptedContainer: wrong or missing salt");
        }
        byte[] salt = Base64.decodeBase64(saltNode.asText());
        if (salt.length != 32) {
            throw new Exception("parseEncryptedContainer: bad salt length (must be 32)");
        }
        JsonNode cipheredNode = json.get("ciphered");
        if (cipheredNode == null) {
            throw new Exception("parseEncryptedContainer: wrong or missing ciphered content");
        }
        byte[] ciphered = Base64.decodeBase64(cipheredNode.asText());
        if (ciphered == null) {
            throw new Exception("parseEncryptedContainer: ciphered content not base64");
        }
        byte[] key = AESCryptor.make256BitKeyFromPassword_PBKDF2WithHmacSHA256(password, salt);
//        System.out.println("key="+CryptoUtils.toHex(key));
//        System.out.println("salt="+CryptoUtils.toHex(salt));
//        System.out.println("ciphered="+CryptoUtils.toHex(ciphered));

        byte[] plainText = AESCryptor.decrypt(key,null,ciphered);

        return plainText;
    }

}
