/**
 * Created with IntelliJ IDEA.
 * User: pavel
 * Date: 21.06.13
 * Time: 15:10
 * (C) 2013 Hoccer GmbH
 */

package com.hoccer.talk.crypto;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;


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
        return new String(decryptRSA(priv, CryptoUtils.toByte(encrypted)));
    }

    public static String encryptRSA(PublicKey pub, String clear)
            throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        return CryptoUtils.toHex(encryptRSA(pub, clear.getBytes()));
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
        return CryptoUtils.toHex(CryptoUtils.shorten(sha256(unwrappedPubKey),8));
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

    public static byte[] wrapRSA1024_X509_deprecated(byte[] pure_DER_rsa_pub_key)
            throws InvalidKeyException {
        if (pure_DER_rsa_pub_key.length > 150) {
            throw new InvalidKeyException("Key too long, not RSA1024");
        }
        byte[] header = CryptoUtils.toByte("30819f300d06092a864886f70d010101050003818d00");
        byte seq1LenIndex = 2;
        byte seq2LenIndex = 20;

        byte seq2Len = (byte) (pure_DER_rsa_pub_key.length + 1);
        byte seq1Len = (byte) (seq2Len + 18);

        header[seq1LenIndex] = seq1Len;
        header[seq2LenIndex] = seq2Len;

        return CryptoUtils.concat(header, pure_DER_rsa_pub_key);
    }

    public static byte[] wrapRSA_X509(byte[] pure_DER_rsa_pub_key)
            throws InvalidKeyException
    {
        int bitstringEncLength;
        if ((pure_DER_rsa_pub_key.length + 1) < 128) {
            bitstringEncLength = 1;
        }
        else {
            bitstringEncLength = ((pure_DER_rsa_pub_key.length + 1) / 256) + 2;
        }
        byte[] oidSequence = CryptoUtils.toByte("300d06092a864886f70d0101010500");

        byte[] encKey = CryptoUtils.toByte("30");
        int i = oidSequence.length + 2 + bitstringEncLength + pure_DER_rsa_pub_key.length;
        encKey = CryptoUtils.concat(encKey,ASNEncodedLength(i));

        encKey = CryptoUtils.concat(encKey,oidSequence);

        encKey = CryptoUtils.concat(encKey,CryptoUtils.toByte("03"));
        encKey = CryptoUtils.concat(encKey,ASNEncodedLength(pure_DER_rsa_pub_key.length + 1));
        encKey = CryptoUtils.concat(encKey,CryptoUtils.toByte("00"));
        encKey = CryptoUtils.concat(encKey,pure_DER_rsa_pub_key);

        return encKey;
    }


    public static byte[] ASNEncodedLength(int length) {
        // System.out.println("ASNEncodedLength(" + length+")");
        byte[] buf;
        if (length < 128)
        {
            buf = new byte[1];
            buf[0] = (byte)length;
            return buf;
        }

        int i = (length / 256) + 1;
        buf = new byte[i+1];

        // System.out.println("ASNEncodedLength i = " + i);

        buf[0] = (byte)(i + 0x80);
        for (int j = 0 ; j < i; ++j)
        {
            // System.out.println("ASNEncodedLength j = " + j + " i-j="+(i-j));
            buf[i - j] = (byte)(length & 0xFF);
            length = length >> 8;
        }

        return buf;
    }

    public static byte[] unwrapRSA1024_X509_deprecated(byte[] X509_rsa_pub_key)
            throws InvalidKeyException {
        if (X509_rsa_pub_key.length > 150 + 21) {
            throw new InvalidKeyException("Key too long");
        }
        return CryptoUtils.skip(X509_rsa_pub_key, 22);
    }

    public static byte[] unwrapRSA_X509(byte[] X509_rsa_pub_key) throws InvalidKeyException
    {
        //System.out.println("unwrapRSA_X509:" + toHex(X509_rsa_pub_key));

        // Skip ASN.1 public key header
        int len = X509_rsa_pub_key.length;
        if (len == 0) {
            throw new InvalidKeyException("Empty key");
        }

        int  idx    = 0;

        if (X509_rsa_pub_key[idx++] != 0x30) {
            throw new InvalidKeyException("Bad byte at 0");
        }

        // System.out.println("unwrapRSA_X509: compare " + (X509_rsa_pub_key[idx]&0xff) + " > " + (int)0x80);

        if ((X509_rsa_pub_key[idx]&0xff) > 0x80) {
            idx += (X509_rsa_pub_key[idx]&0xff) - 0x80 + 1;
            // System.out.println("unwrapRSA_X509: calculated idx" + idx);
        }
        else {
            idx++;
            // System.out.println("unwrapRSA_X509: incremented idx" + idx);
        }
        // PKCS #1 rsaEncryption szOID_RSA_RSA
        byte[] seqiod = CryptoUtils.toByte("300d06092a864886f70d0101010500");

        for (int i = 0; i < 15;++i) {
            if (X509_rsa_pub_key[idx+i] != seqiod[i]) {
                // System.out.println("X509_rsa_pub_key["+(idx+i)+"]=" + toHex(X509_rsa_pub_key[idx+i])+", seqiod["+i+"]="+toHex(seqiod[i]));
                throw new InvalidKeyException("Bad byte in header");
            }
        }
        idx += 15;

        if (X509_rsa_pub_key[idx++] != 0x03) {
            throw new InvalidKeyException("Bad byte after header");
        }

        if ((X509_rsa_pub_key[idx]&0xff) > 0x80) {
            idx += (X509_rsa_pub_key[idx]&0xff) - 0x80 + 1;
        }
        else {
            idx++;
        }

        if (X509_rsa_pub_key[idx++] != '\0') {
            throw new InvalidKeyException("Bad byte at start of key");
        }
        return CryptoUtils.skip(X509_rsa_pub_key, idx);
    }

    public static byte[] wrapRSA1024_PKCS8_deprecated(byte[] pure_DER_rsa_priv_key)
            throws InvalidKeyException {
        if (pure_DER_rsa_priv_key.length > 650) {
            throw new InvalidKeyException("Key too long, not RSA1024");
        }
        byte[] header = CryptoUtils.toByte("30820278020100300d06092a864886f70d010101050004820262");
        byte seq1LenIndex = 2;
        byte seq2LenIndex = 24;

        int seq2Len = pure_DER_rsa_priv_key.length - 2;
        int seq1Len = seq2Len + 22;

        header[seq1LenIndex] = (byte) (seq1Len / 255);
        header[seq1LenIndex + 1] = (byte) (seq1Len % 255);
        header[seq2LenIndex] = (byte) (seq2Len / 255);
        header[seq2LenIndex + 1] = (byte) (seq2Len % 255);

        return CryptoUtils.concat(header, pure_DER_rsa_priv_key);
    }

    public static byte[] wrapRSA_PKCS8(byte[] pure_DER_rsa_priv_key)
            throws InvalidKeyException {
        if (pure_DER_rsa_priv_key.length < 100) {
            throw new InvalidKeyException("key too short");
        }

        return pure_DER_rsa_priv_key;
    }

    public static byte[] unwrapRSA1024_PKCS8_deprecated(byte[] PKCS8_rsa_priv_key)
            throws InvalidKeyException {
        if (PKCS8_rsa_priv_key.length > 650 + 25) {
            throw new InvalidKeyException("Key too long");
        }
        return CryptoUtils.skip(PKCS8_rsa_priv_key, 26);
    }

    public static byte[] unwrapRSA_PKCS8(byte[] PKCS8_rsa_priv_key)
            throws InvalidKeyException {
        if (PKCS8_rsa_priv_key.length == 0) {
            throw new InvalidKeyException("Key too long");
        }
        return PKCS8_rsa_priv_key;
    }

    public static PublicKey makePublicRSA1024Key_deprecated(byte[] pure_DER_rsa_pub_key)
            throws NoSuchAlgorithmException, InvalidKeyException,
            InvalidKeySpecException {
        KeyFactory kf = KeyFactory.getInstance("RSA");

        X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(
                wrapRSA1024_X509_deprecated(pure_DER_rsa_pub_key));
        PublicKey myPublicKey = kf.generatePublic(pubSpec);
        return myPublicKey;
    }

    public static PublicKey makePublicRSAKey(byte[] pure_DER_rsa_pub_key)
            throws NoSuchAlgorithmException, InvalidKeyException,
            InvalidKeySpecException {
        KeyFactory kf = KeyFactory.getInstance("RSA");

        X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(
                wrapRSA_X509(pure_DER_rsa_pub_key));
        PublicKey myPublicKey = kf.generatePublic(pubSpec);
        return myPublicKey;
    }

    public static PrivateKey makePrivateRSA1024Key_deprecated(byte[] pure_DER_rsa_priv_key)
            throws NoSuchAlgorithmException, InvalidKeyException,
            InvalidKeySpecException {
        KeyFactory kf = KeyFactory.getInstance("RSA");

        PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(
                wrapRSA1024_PKCS8_deprecated(pure_DER_rsa_priv_key));
        PrivateKey myPrivateKey = kf.generatePrivate(privSpec);
        return myPrivateKey;
    }

    public static PrivateKey makePrivateRSAKey(byte[] pure_DER_rsa_priv_key)
            throws NoSuchAlgorithmException, InvalidKeyException,
            InvalidKeySpecException {
        KeyFactory kf = KeyFactory.getInstance("RSA");

        PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(
                wrapRSA_PKCS8(pure_DER_rsa_priv_key));
        PrivateKey myPrivateKey = kf.generatePrivate(privSpec);
        return myPrivateKey;
    }



}