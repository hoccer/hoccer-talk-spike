/**
 * Created with IntelliJ IDEA.
 * User: pavel
 * Date: 21.06.13
 * Time: 15:10
 * (C) 2013 Hoccer GmbH
 */

package com.hoccer.talk.crypto;

import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.spec.*;
import java.util.Arrays;


public class RSACryptor {

    private static Digest FINGERPRINT_DIGEST = new SHA256Digest();

    public static byte[] decryptRSA(PrivateKey priv, byte[] encrypted)
            throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        // Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, priv);
        byte[] decrypted = cipher.doFinal(encrypted);
        return decrypted;
    }

    public static byte[] encryptRSA(PublicKey pub, byte[] clear)
            throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        // Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, pub);
        byte[] encrypted = cipher.doFinal(clear);
        return encrypted;
    }

    public static String decryptRSA(PrivateKey priv, String encrypted)
            throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        return new String(decryptRSA(priv, toByte(encrypted)));
    }

    public static String encryptRSA(PublicKey pub, String clear)
            throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        return toHex(encryptRSA(pub, clear.getBytes()));
    }

    public static KeyPair generateRSAKeyPair(int len)
            throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(len);
        KeyPair kp = kpg.genKeyPair();
        return kp;
    }

    public static RSAPublicKeySpec getPublicKeySpec(KeyPair kp)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory fact = KeyFactory.getInstance("RSA");
        RSAPublicKeySpec pub = fact.getKeySpec(kp.getPublic(),
                RSAPublicKeySpec.class);
        return pub;
    }

    public static RSAPublicKeySpec getPublicKeySpec(PublicKey pubkey)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory fact = KeyFactory.getInstance("RSA");
        RSAPublicKeySpec pub = fact.getKeySpec(pubkey, RSAPublicKeySpec.class);
        return pub;
    }

    public static RSAPrivateKeySpec getPrivateKeySpec(KeyPair kp)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory fact = KeyFactory.getInstance("RSA");
        RSAPrivateKeySpec priv = fact.getKeySpec(kp.getPrivate(),
                RSAPrivateKeySpec.class);
        return priv;
    }

    public static RSAPrivateKeySpec getPrivateKeySpec(PrivateKey privkey)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory fact = KeyFactory.getInstance("RSA");
        RSAPrivateKeySpec priv = fact.getKeySpec(privkey,
                RSAPrivateKeySpec.class);
        return priv;
    }

    private static byte[] sha256(byte[] bytes)
            throws UnsupportedEncodingException, NoSuchAlgorithmException {
        byte[] out = null;
        synchronized (FINGERPRINT_DIGEST) {
            out = new byte[FINGERPRINT_DIGEST.getDigestSize()];
            FINGERPRINT_DIGEST.reset();
            FINGERPRINT_DIGEST.update(bytes, 0, bytes.length);
            FINGERPRINT_DIGEST.doFinal(out, 0);
        }
        return out;
    }

    public static String calcKeyId(byte[] unwrappedPubKey) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        return RSACryptor.toHex(RSACryptor.shorten(sha256(unwrappedPubKey),8));
    }

    public static String toString(RSAPrivateKeySpec priv) {
        String result = "RSA-Private:" + "modulos:" + priv.getModulus()
                + ",exponent:" + priv.getPrivateExponent();
        return result;
    }

    public static String toString(RSAPublicKeySpec pub) {
        String result = "RSA-Public:" + "modulos:" + pub.getModulus()
                + ",exponent:" + pub.getPublicExponent();
        return result;
    }

    public static byte[] wrapRSA1024_X509(byte[] pure_DER_rsa_pub_key)
            throws InvalidKeyException {
        if (pure_DER_rsa_pub_key.length > 150) {
            throw new InvalidKeyException("Key too long, not RSA1024");
        }
        byte[] header = toByte("30819f300d06092a864886f70d010101050003818d00");
        byte seq1LenIndex = 2;
        byte seq2LenIndex = 20;

        byte seq2Len = (byte) (pure_DER_rsa_pub_key.length + 1);
        byte seq1Len = (byte) (seq2Len + 18);

        header[seq1LenIndex] = seq1Len;
        header[seq2LenIndex] = seq2Len;

        return concat(header, pure_DER_rsa_pub_key);
    }

    public static byte[] unwrapRSA1024_X509(byte[] X509_rsa_pub_key)
            throws InvalidKeyException {
        if (X509_rsa_pub_key.length > 150 + 21) {
            throw new InvalidKeyException("Key too long");
        }
        return skip(X509_rsa_pub_key, 22);
    }

    public static byte[] wrapRSA1024_PKCS8(byte[] pure_DER_rsa_priv_key)
            throws InvalidKeyException {
        if (pure_DER_rsa_priv_key.length > 650) {
            throw new InvalidKeyException("Key too long, not RSA1024");
        }
        byte[] header = toByte("30820278020100300d06092a864886f70d010101050004820262");
        byte seq1LenIndex = 2;
        byte seq2LenIndex = 24;

        int seq2Len = pure_DER_rsa_priv_key.length - 2;
        int seq1Len = seq2Len + 22;

        header[seq1LenIndex] = (byte) (seq1Len / 255);
        header[seq1LenIndex + 1] = (byte) (seq1Len % 255);
        header[seq2LenIndex] = (byte) (seq2Len / 255);
        header[seq2LenIndex + 1] = (byte) (seq2Len % 255);

        return concat(header, pure_DER_rsa_priv_key);
    }

    public static byte[] unwrapRSA1024_PKCS8(byte[] PKCS8_rsa_priv_key)
            throws InvalidKeyException {
        if (PKCS8_rsa_priv_key.length > 650 + 25) {
            throw new InvalidKeyException("Key too long");
        }
        return skip(PKCS8_rsa_priv_key, 26);
    }

    public static PublicKey makePublicRSA1024Key(byte[] pure_DER_rsa_pub_key)
            throws NoSuchAlgorithmException, InvalidKeyException,
            InvalidKeySpecException {
        KeyFactory kf = KeyFactory.getInstance("RSA");

        X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(
                wrapRSA1024_X509(pure_DER_rsa_pub_key));
        PublicKey myPublicKey = kf.generatePublic(pubSpec);
        return myPublicKey;
    }

    public static PrivateKey makePrivateRSA1024Key(byte[] pure_DER_rsa_priv_key)
            throws NoSuchAlgorithmException, InvalidKeyException,
            InvalidKeySpecException {
        KeyFactory kf = KeyFactory.getInstance("RSA");

        PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(
                wrapRSA1024_PKCS8(pure_DER_rsa_priv_key));
        PrivateKey myPrivateKey = kf.generatePrivate(privSpec);
        return myPrivateKey;
    }

    public static byte[] concat(byte[] first, byte[] second) {
        byte[] result = new byte[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    public static byte[] shorten(byte[] array, int length) {
        if (length == array.length) {
            return array;
        }
        byte[] result = new byte[length];
        System.arraycopy(array, 0, result, 0, length);
        return result;
    }

    public static byte[] skip(byte[] array, int skiplength) {
        byte[] result = new byte[array.length - skiplength];
        System.arraycopy(array, skiplength, result, 0, result.length);
        return result;
    }

    public static byte[] overwrite(byte[] array, byte[] into, int offset) {
        byte[] result = new byte[into.length];
        System.arraycopy(into, 0, result, 0, into.length);
        System.arraycopy(array, 0, result, offset, array.length);
        return result;
    }

    // String conversion
    public static String toHex(String txt) {
        return toHex(txt.getBytes());
    }

    public static String fromHex(String hex) {
        return new String(toByte(hex));
    }

    public static byte[] toByte(String hexString) {
        int len = hexString.length() / 2;
        byte[] result = new byte[len];
        for (int i = 0; i < len; i++)
            result[i] = Integer.valueOf(hexString.substring(2 * i, 2 * i + 2),
                    16).byteValue();
        return result;
    }

    public static String toHex(byte[] buf) {
        if (buf == null)
            return "";
        StringBuffer result = new StringBuffer(2 * buf.length);
        for (int i = 0; i < buf.length; i++) {
            appendHex(result, buf[i]);
        }
        return result.toString();
    }

    private final static String HEX = "0123456789abcdef";

    private static void appendHex(StringBuffer sb, byte b) {
        sb.append(HEX.charAt((b >> 4) & 0x0f)).append(HEX.charAt(b & 0x0f));
    }

}