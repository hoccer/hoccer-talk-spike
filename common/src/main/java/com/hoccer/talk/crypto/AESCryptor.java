/**
 * Created with IntelliJ IDEA.
 * User: pavel
 * Date: 21.06.13
 * Time: 15:42
 * (C) 2013 Hoccer GmbH
 */

package com.hoccer.talk.crypto;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;


public class AESCryptor {

    public static byte[] getRawKey(byte[] secret_key, byte[] salt) throws InvalidKeyException {
        if (salt == null) {
            return secret_key;
        }
        return xor(secret_key, salt);
    }

    public static byte[] xor(byte[] a, byte[]b) throws InvalidKeyException {
        if (a.length != b.length) {
            throw new InvalidKeyException();
        }
        byte[] result = new byte[a.length];

        for (int i = 0; i < a.length; ++i) {
            result[i] = (byte)(a[i] ^ b[i]);
        }
        return result;
    }

    public static byte[] makeRandomBytes(int bytes) {
        SecureRandom sr;
        try {
            sr = SecureRandom.getInstance("SHA1PRNG");
            byte[] salt = new byte[bytes];
            sr.nextBytes(salt);
            return salt;
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public static Cipher makeCipher(byte[] secret_key, byte[] salt, byte[] iv, int mode, String transformation)
            throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, UnsupportedEncodingException,
            InvalidAlgorithmParameterException
    {
        byte[] rawKey = getRawKey(salt, secret_key);
        SecretKeySpec skeySpec = new SecretKeySpec(rawKey, transformation);
        Cipher cipher = Cipher.getInstance(transformation + "/CBC/PKCS7Padding");
        cipher.init(mode, skeySpec, new IvParameterSpec(iv));
        return cipher;
    }

    public static byte[] encrypt(byte[] secret_key, byte[] salt,
                                 byte[] cleartext) throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidKeyException, BadPaddingException,
            IllegalBlockSizeException, UnsupportedEncodingException,
            InvalidAlgorithmParameterException {

        byte[] nulliv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        Cipher c = makeCipher(secret_key, salt, nulliv, Cipher.ENCRYPT_MODE, "AES");
        byte[] result = crypt(c, cleartext);
        return result;
    }

    public static String encryptString(byte[] secret_key, byte[] salt,
                                 String cleartext) throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidKeyException, BadPaddingException,
            IllegalBlockSizeException, UnsupportedEncodingException,
            InvalidAlgorithmParameterException {

        byte[] result = encrypt(secret_key, salt, cleartext.getBytes("UTF-8"));
        return Base64.encodeBase64String(result);
    }


    public static String decryptString(byte[] secret_key, byte[] salt,
                                 String encrypted_b64) throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidKeyException, BadPaddingException,
            IllegalBlockSizeException, IOException,
            InvalidAlgorithmParameterException, InvalidKeyException {
        byte[] encrypted = Base64.decodeBase64(encrypted_b64);
        byte[] result = decrypt(secret_key, salt, encrypted);
        return Base64.encodeBase64String(result);

    }
    public static byte[] decrypt(byte[] secret_key, byte[] salt,
                                 byte[] ciphertext) throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidKeyException, BadPaddingException,
            IllegalBlockSizeException, IOException,
            InvalidAlgorithmParameterException {
        byte[] nulliv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        Cipher c = makeCipher(secret_key, salt, nulliv, Cipher.DECRYPT_MODE, "AES");
        byte[] result = crypt(c, ciphertext);
        return result;

    }

    public static byte[] crypt(Cipher cipher, byte[] clear)
            throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        byte[] encrypted = cipher.doFinal(clear);
        return encrypted;
    }

    public static InputStream encryptingInputStream(InputStream is, byte[] secret_key, byte[] salt) throws Exception {

        byte[] nulliv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        Cipher encryptionCipher = makeCipher(secret_key, salt, nulliv, Cipher.ENCRYPT_MODE, "AES");
        return new CipherInputStream(is, encryptionCipher);

    }

    public static int calcEncryptedSize(int plainSize, byte[] secret_key, byte[] salt)
            throws InvalidKeyException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
                    NoSuchPaddingException, UnsupportedEncodingException {
        byte[] nulliv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        Cipher encryptionCipher = makeCipher(secret_key, salt, nulliv, Cipher.ENCRYPT_MODE, "AES");
        return encryptionCipher.getOutputSize(plainSize);
    }

    public static OutputStream decryptingOutputStream(OutputStream os, byte[] secret_key, byte[] salt) throws Exception {
        byte[] nulliv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        Cipher c = makeCipher(secret_key, salt, nulliv, Cipher.DECRYPT_MODE, "AES");
        return new CipherOutputStream(os, c);
    }

    public void testAES() throws Exception {
        byte[] testsalt = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14,
                15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
                30, 31, 32 };
        byte[] testkey = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14,
                15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
                30, 31};

        byte[] nulliv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

        System.out.println("AES encryption/decryption Testing:");
        System.out.println("salt=" + Base64.encodeBase64String(testsalt));
        System.out.println("salt=" + RSACryptor.toHex(testsalt));

        Cipher c = makeCipher(testkey, testsalt, nulliv, Cipher.ENCRYPT_MODE, "AES");
        byte[] encrypted = crypt(c, new String("test").getBytes("UTF-8"));

        System.out.println("AES-encrypted=" + Base64.encodeBase64String(encrypted));
        System.out.println("AES-encrypted=" + RSACryptor.toHex(encrypted));

        Cipher d = makeCipher(testkey, testsalt, nulliv, Cipher.DECRYPT_MODE, "AES");
        byte[] decrypted = crypt(d, encrypted);
        System.out.println("AES-decrypted=" + new String(decrypted));

        String myClearText = "This is a new Text";
        String myCipherText = encryptString(testkey, testsalt, myClearText);
        String myDecipheredText = decryptString(testkey, testkey, myCipherText);

        if (!myClearText.equals(myDecipheredText)) {
            System.out.println("ERROR: encryption/decryption failed, myDecipheredText=" + myDecipheredText);
        }

        System.out.println("done test");
    }
}
