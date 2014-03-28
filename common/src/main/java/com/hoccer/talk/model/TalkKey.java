package com.hoccer.talk.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hoccer.talk.crypto.RSACryptor;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.bouncycastle.util.encoders.Base64;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;

/**
 * Public keys used by a given client
 *
 * The client provides these, the server will provide them
 * to anyone who knows both the client and key id.
 *
 * The timestamp is updated by the server when the
 * object is stored in the database.
 */
@DatabaseTable(tableName="key")
public class TalkKey {

    private String _id;

    @DatabaseField(generatedId = true)
    private int publicKeyId;

    @DatabaseField
    private String clientId;

    @DatabaseField
    private String keyId;

    @DatabaseField(width = 1024)
    private String key;

    @DatabaseField
    private Date timestamp;

    @DatabaseField
    private String sharedKeyIdSalt;

    @DatabaseField
    private String sharedKeyId;

    public TalkKey() {
    }

    @JsonIgnore
    public PublicKey getAsNative() {
        PublicKey key = null;
        try {
            byte[] decoded = Base64.decode(this.key);
            key = RSACryptor.makePublicRSAKey(decoded);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return key;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getSharedKeyId() {
        return sharedKeyId;
    }

    public void setSharedKeyId(String sharedKeyId) {
        this.sharedKeyId = sharedKeyId;
    }

    public String getSharedKeyIdSalt() {
        return sharedKeyIdSalt;
    }

    public void setSharedKeyIdSalt(String sharedKeyIdSalt) {
        this.sharedKeyIdSalt = sharedKeyIdSalt;
    }
}
