/**
 * Created with IntelliJ IDEA.
 * User: pavel
 * Date: 21.06.13
 * Time: 15:31
 * (C) 2013 Hoccer GmbH
 */

package com.hoccer.talk.crypto;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import com.hoccer.talk.crypto.RSACryptor;


public class RSAKeypairStore {

    // Instance Fields ---------------------------------------------------

    private byte[] mEncodedPrivateKey;

    private byte[] mEncodedPublicKey;

    private String mPublicKeyString;

    private PrivateKey mPrivateKey;

    private PublicKey mPublicKey;

    private boolean mPublicPrivateKeyOK = false;

    // Public Instance Methods -------------------------------------------

    public boolean areKeysOk() {

        return mPublicPrivateKeyOK;
    }

    public String getPublicKeyString() {

        return mPublicKeyString;
    }

    public void ensureKeyPair() {

        mPublicPrivateKeyOK = false;
        boolean keySetupFailed = true;

        if (AsyncLinccer.getFlagFromSharedPreferences(HoccerActivity.this, AsyncLinccer.PREF_RENEW_KEYPAIR, false)) {
            keySetupFailed = !createKeyPair();
        } else {
            try {
                Logger.v(LOG_TAG, "Loading RSA key pair");
                mEncodedPrivateKey = IoHelper.readByteArrayFromFile(PRIVATE_KEY_FILENAME, HoccerActivity.this);
                mEncodedPublicKey = IoHelper.readByteArrayFromFile(PUBLIC_KEY_FILENAME, HoccerActivity.this);
                keySetupFailed = false;
                Logger.v(LOG_TAG, "Loading RSA key pair done");
            } catch (IOException e) {
                // create new key pair
                keySetupFailed = !createKeyPair();
            }
        }
        if (!keySetupFailed) {
            KeyFactory kf;
            try {
                kf = KeyFactory.getInstance("RSA");

                X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(
                        RSACryptor.wrapRSA1024_X509(mEncodedPublicKey));
                mPublicKey = kf.generatePublic(pubSpec);

                PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(
                        RSACryptor.wrapRSA1024_PKCS8(mEncodedPrivateKey));
                mPrivateKey = kf.generatePrivate(privSpec);

                String testString = "Blafasel12345678";
                String encr = RSACryptor.encryptRSA(mPublicKey, testString);
                String decr = RSACryptor.decryptRSA(mPrivateKey, encr);

                if (testString.contentEquals(decr)) {
                    mPublicPrivateKeyOK = true;
                }
            } catch (NoSuchAlgorithmException e) {
                Logger.e(LOG_TAG, e);
            } catch (InvalidKeyException e) {
                Logger.e(LOG_TAG, e);
            } catch (InvalidKeySpecException e) {
                Logger.e(LOG_TAG, e);
            } catch (NoSuchPaddingException e) {
                Logger.e(LOG_TAG, e);
            } catch (BadPaddingException e) {
                Logger.e(LOG_TAG, e);
            } catch (IllegalBlockSizeException e) {
                Logger.e(LOG_TAG, e);
            }

        }
        if (!mPublicPrivateKeyOK) {
            Toast.makeText(HoccerActivity.this, "RSA test failed, public key encryption disabled", 2000);
            AsyncLinccer
                    .setFlagInSharedPreferences(HoccerActivity.this, AsyncLinccer.PREF_DISTRIBUTE_PUBKEY, false);
        } else {
            Logger.v(LOG_TAG, "RSA key pair check ok.");
            mPublicKeyString = Base64.encodeBytes(mEncodedPublicKey);
        }
        try {
            AsyncLinccer.setInSharedPreferences(HoccerActivity.this, AsyncLinccer.PREF_PUBLIC_KEY,
                    Base64.encodeBytes(RSACryptor.unwrapRSA1024_X509(mPublicKey.getEncoded())));
        } catch (InvalidKeyException e) {

            Logger.e(LOG_TAG, e);
        }
        try {
            AsyncLinccer.setInSharedPreferences(HoccerActivity.this, AsyncLinccer.PREF_PRIVATE_KEY,
                    Base64.encodeBytes(RSACryptor.unwrapRSA1024_PKCS8(mPrivateKey.getEncoded())));
        } catch (InvalidKeyException e) {

            Logger.e(LOG_TAG, e);
        }
    }

    public boolean createKeyPair() {

        Logger.v(LOG_TAG, "creating new RSA key pair");
        KeyPair kp;
        try {
            kp = RSACryptor.generateRSAKeyPair(1024);

            Logger.v(LOG_TAG, "creating new RSA key pair done");

            byte[] pubenc = kp.getPublic().getEncoded();
            byte[] privenc = kp.getPrivate().getEncoded();

            Logger.v(LOG_TAG, "RSA-pub-ts[" + pubenc.length + "]:" + Base64.encodeBytes(pubenc));
            Logger.v(LOG_TAG, "RSA-priv-ts[" + privenc.length + "]:" + Base64.encodeBytes(privenc));

            mEncodedPublicKey = RSACryptor.unwrapRSA1024_X509(pubenc);
            Logger.v(LOG_TAG,
                    "RSA-pub-IOS[" + mEncodedPublicKey.length + ":" + Base64.encodeBytes(mEncodedPublicKey));

            mEncodedPrivateKey = RSACryptor.unwrapRSA1024_PKCS8(privenc);
            Logger.v(LOG_TAG,
                    "RSA-priv-IOS[" + mEncodedPrivateKey.length + "]:" + Base64.encodeBytes(mEncodedPrivateKey));

            IoHelper.writeByteArrayToFile(mEncodedPrivateKey, PRIVATE_KEY_FILENAME, HoccerActivity.this);
            IoHelper.writeByteArrayToFile(mEncodedPublicKey, PUBLIC_KEY_FILENAME, HoccerActivity.this);

            return true;

        } catch (NoSuchAlgorithmException e1) {
            Logger.e(LOG_TAG, e1);
        } catch (InvalidKeyException e1) {
            Logger.e(LOG_TAG, e1);
        } catch (IOException e1) {
            Logger.e(LOG_TAG, e1);
        }
        return false;
    }

}
