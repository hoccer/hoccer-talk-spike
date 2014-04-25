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
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Enumeration;


public class AESCryptor {

    public static final int KEY_SIZE = 32; // in bytes

    public static final byte[] NULL_SALT = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

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
        byte[] rawKey = getRawKey(secret_key, salt);
        //System.out.println("rawKey=" + CryptoUtils.toHex(rawKey));
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

    public static byte[] crypt(Cipher cipher, byte[] text)
            throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
//        System.out.println("text="+RSACryptor.toHex(text));
//        System.out.println("text len="+text.length);

//        int calcLen = cipher.getOutputSize(text.length);
//        byte[] crypted = new byte[calcLen];
//        int ctLength = cipher.update(text, 0, text.length, crypted, 0);
//        ctLength += cipher.doFinal(crypted, ctLength);
//        RSACryptor.shorten(crypted,ctLength);
//        System.out.println("crypted="+RSACryptor.toHex(crypted));
//        System.out.println("crypted len=" + crypted.length + ",ctLength=" + ctLength + ",calcLen=" + calcLen);

        byte[] crypted = cipher.doFinal(text);
        return crypted;
    }
    public static String encryptString(byte[] secret_key, byte[] salt,
                                 String cleartext) throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidKeyException, BadPaddingException,
            IllegalBlockSizeException, UnsupportedEncodingException,
            InvalidAlgorithmParameterException {
        byte[] clearbytes = cleartext.getBytes("UTF-8");
        byte[] result = encrypt(secret_key, salt, clearbytes);
//        return Base64.encodeBase64String(result);
        return new String(Base64.encodeBase64(result));
    }


    public static String decryptString(byte[] secret_key, byte[] salt,
                                 String encrypted_b64) throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidKeyException, BadPaddingException,
            IllegalBlockSizeException, IOException,
            InvalidAlgorithmParameterException, InvalidKeyException {
        byte[] encrypted = Base64.decodeBase64(encrypted_b64);
        byte[] result = decrypt(secret_key, salt, encrypted);
        return new String(result, "UTF-8");
    }

    public static CipherInputStream encryptingInputStream(InputStream is, byte[] secret_key, byte[] salt) throws Exception {

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

    public static CipherOutputStream decryptingOutputStream(OutputStream os, byte[] secret_key, byte[] salt) throws Exception {
        byte[] nulliv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        Cipher c = makeCipher(secret_key, salt, nulliv, Cipher.DECRYPT_MODE, "AES");
        return new CipherOutputStream(os, c);
    }

    public static byte[] calcSymmetricKeyId(byte[] key, byte[] salt) throws NoSuchAlgorithmException {
        Mac hmacAlgo = Mac.getInstance("HmacSHA256");
        byte[] keyHash = derivePBKDF2Key(hmacAlgo, key, salt, 10000, 256);
        return CryptoUtils.shorten(keyHash,8);
    }

    public static byte[] make256BitKeyFromPassword_PBKDF2WithHmacSHA1(String password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException, UnsupportedEncodingException {
        //SecretKeyFactory kf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        SecretKeyFactory kf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        KeySpec specs = new PBEKeySpec(password.toCharArray(), salt, 10000, 256);
        //KeySpec specs = new PBEKeySpec(password.getBytes("UTF-8"), salt, 10000, 256);
        SecretKey key = kf.generateSecret(specs);
        return key.getEncoded();
    }

    // for strange reasons PBKDF2WithHmacSHA256 is not available on most Java implementations even with full crypto policy on
    // we use therefore our own key derivation function below
    // We should replace this code once PBKDF2WithHmacSHA256 is generally available which should be the case with JDK8
    public static byte[] make256BitKeyFromPassword_PBKDF2WithHmacSHA256(String password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException, UnsupportedEncodingException {

        Mac hmacAlgo = Mac.getInstance("HmacSHA256");
        byte[] key = derivePBKDF2Key(hmacAlgo, password.getBytes("UTF-8"),salt,10000,256);
        return key;
    }

    private static byte[] derivePBKDF2Key(Mac prf, byte[] password, byte[] salt,
                                    int iterCount, int keyLengthInBit) {
        int keyLength = keyLengthInBit/8;
        byte[] key = new byte[keyLength];
        try {
            int hlen = prf.getMacLength();
            int intL = (keyLength + hlen - 1)/hlen; // ceiling
            int intR = keyLength - (intL - 1)*hlen; // residue
            byte[] ui = new byte[hlen];
            byte[] ti = new byte[hlen];
            SecretKey macKey = new SecretKeySpec(password, prf.getAlgorithm());
            prf.init(macKey);

            byte[] ibytes = new byte[4];
            for (int i = 1; i <= intL; i++) {
                prf.update(salt);
                ibytes[3] = (byte) i;
                ibytes[2] = (byte) ((i >> 8) & 0xff);
                ibytes[1] = (byte) ((i >> 16) & 0xff);
                ibytes[0] = (byte) ((i >> 24) & 0xff);
                prf.update(ibytes);
                prf.doFinal(ui, 0);
                System.arraycopy(ui, 0, ti, 0, ui.length);

                for (int j = 2; j <= iterCount; j++) {
                    prf.update(ui);
                    prf.doFinal(ui, 0);
                    // XOR the intermediate Ui's together.
                    for (int k = 0; k < ui.length; k++) {
                        ti[k] ^= ui[k];
                    }
                }
                if (i == intL) {
                    System.arraycopy(ti, 0, key, (i-1)*hlen, intR);
                } else {
                    System.arraycopy(ti, 0, key, (i-1)*hlen, hlen);
                }
            }
        } catch (GeneralSecurityException gse) {
            throw new RuntimeException("Error deriving PBKDF2 keys");
        }
        return key;
    }
}
