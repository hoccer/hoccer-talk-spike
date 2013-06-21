/**
 * Created with IntelliJ IDEA.
 * User: pavel
 * Date: 21.06.13
 * Time: 15:10
 * (C) 2013 Hoccer GmbH
 */

package com.hoccer.talk.crypto;

import org.apache.commons.codec.binary.Base64;

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

    public static void testRSA() {
        try {

            KeyPair kp = generateRSAKeyPair(1024);
            // LOG.finest("RSA" + toString(getPrivateKeySpec(kp)));
            // LOG.finest("RSA" + toString(getPublicKeySpec(kp)));
            String encr = encryptRSA(kp.getPublic(), "blafasel12345678");
            byte[] encrIOS = encryptRSA(kp.getPublic(),
                    "blafasel12345678".getBytes("UTF-8"));
            System.out.println("RSA-encrypted-iOS:" + Base64.encodeBase64String(encrIOS));

            // LOG.finest("RSA-encrypted:" + encr);
            String decr = decryptRSA(kp.getPrivate(), encr);
            System.out.println("RSA-decrypted:" + decr);

            byte[] pubenc = kp.getPublic().getEncoded();
            byte[] privenc = kp.getPrivate().getEncoded();
            System.out.println("RSA-pub-ts[" + pubenc.length + "]:"
                    + Base64.encodeBase64String(pubenc));
            // LOG.finest("RSA-pub-ts[" + pubenc.length + "]:" + toHex(pubenc));
            System.out.println("RSA-priv-ts[" + privenc.length + "]:"
                    + Base64.encodeBase64String(privenc));
            // LOG.finest("RSA-priv-ts[" + privenc.length + "]:" +
            // toHex(privenc));

            byte[] pubencIOS = unwrapRSA1024_X509(pubenc);
            System.out.println("RSA-pub-IOS[" + pubencIOS.length + ":"
                    + Base64.encodeBase64String(pubencIOS));
            byte[] privencIOS = unwrapRSA1024_PKCS8(privenc);
            System.out.println("RSA-priv-IOS[" + privencIOS.length + "]:"
                    + Base64.encodeBase64String(privencIOS));

            byte[] pubWrapped = wrapRSA1024_X509(pubencIOS);
            byte[] privWrapped = wrapRSA1024_PKCS8(privencIOS);
            System.out.println("RSA-pubWrapped-ts[" + pubWrapped.length + "]:"
                    + Base64.encodeBase64String(pubWrapped));
            // LOG.finest("RSA-pub-ts[" + pubWrapped + "]:" +
            // toHex(pubWrapped));
            System.out.println("RSA-privWrapped-ts[" + privWrapped.length + "]:"
                    + Base64.encodeBase64String(privWrapped));
            // LOG.finest("RSA-priv-ts[" + privWrapped + "]:" +
            // toHex(privWrapped));
            boolean pubOK = Arrays.equals(pubWrapped, pubenc);
            boolean privOK = Arrays.equals(privWrapped, privenc);
            System.out.println("RSA-wrapper-OK priv:[" + privOK + "]:" + " pub:["
                    + pubOK + "]:");

            String myPubKey2 = "MIGJAoGBAMSvNQyQdJtrsg54RIgb5P6eo8w/VZZoq2QsQbjo/ayqGUp03EDJk31C8aFuq2PLz2FLueC5e+1/IquJKlEXJE7iMN2vORLVna8Mck3C8PN0vEaYKqxM5bq9wyYkzQAg6MJ8jn8Xw7natgE9dtEvpZMcHtzbqiDnLKFAVRAUDgxbAgMBAAE=";
            String myPrivKey2 = "MIICWwIBAAKBgGxE9LQ//NX+i7IDQOD1OZd73R8fk1bIEEvIuGtYuoGMw4u+0eVJYZt4z+njOpJ/H1JMRUQO6PgBIlAsgxhD6SIkjo8vowr9u/R59D+F0t7UzHISZsuxiF6Vh4791tXtqER+9AgsAwaU496OfhmZdnNMinJz5SN7bz0s6XqAV4EhAgMBAAECgYAVzHBkVjnGsCBaL/OBF36H9GVZ3dahc1hsmbYfztaGPNwmJ75E5thjIBjkY16onjWlMTwE7ueS/090SvH+EbY/XG8/7cmZu6GvB4d54Sl07s+IV69wqGKPKl/1SSu1utPD0KlQzdWkaejByz+JoCFJw7m+zYWjSZxgm9G3y1DWQQJBATQOXHBVP3pHk8gM9uukiWNVUhDHqavaM+C2IYVPiyoT4pjXFE4OR/w+9060QDeX+8kSfQF37EKFs7l+HUO8T4kCQFn5Rk2VKYcTFjqJc3d8JoaVlQLjDNbhVjSNwzPeDnlIUSC9wsdiOJ6ixmIfUHRXOPap3jfsVi6T7yYKbGhr5tkCQQDBXTZW6Ju0tJMlokWnuhrm+BpQIBP3pDqmFYzK8hgHbH3ytCaxrDMxOZDgnTIl80d/ehRvRIhPZT9f8rKJ3v0JAkAVQkXvPOhUBxmAeUu0FryPnjZYOUemWhXhUwGldrlaxNCOeOfV7opMSU+wjY+X/afy+E4OTqRKWx/tkBbvUVd5AkEAuNaeJaJ0zq6mwngEhVIfjyntFF31rL4no5FKa7ePQ4TdcqWzlYHq6vdxmAZRhlQuN+48L3gjd6XHxkx+g78qkA==";

            String myPubKey = "MIGJAoGBALr4jEVZIw+hWUWXcCYw1aXqeSuJnda7YupliMaIdRvVVyrvTE7bHCHpgG6Q961hvFa5a38Jn5lnb/lerLj6n6NRtGhdqNXIsgZ+tQKkBIW4PaHxn5Gni7jw8ZfKbN/D437K2wjNPxS0E6Av1wWgeOmUkqUBjeBHb/rexRnqO3eVAgMBAAE=";
            String myPrivKey = "MIICWwIBAAKBgQC6+IxFWSMPoVlFl3AmMNWl6nkriZ3Wu2LqZYjGiHUb1Vcq70xO2xwh6YBukPetYbxWuWt/CZ+ZZ2/5Xqy4+p+jUbRoXajVyLIGfrUCpASFuD2h8Z+Rp4u48PGXymzfw+N+ytsIzT8UtBOgL9cFoHjplJKlAY3gR2/63sUZ6jt3lQIDAQABAoGAYHTtWLF9pwikV4Si9PDop6npTQ64ARm3FBnBkDrBv9Q2Hg5KHbxoLQ6blW7wd+AeG9eYn3dFgQyd9dZj4SJazAKJ5G0eWjga6jwDDI6+0SIQnlHmsCYPoI2ZTBHWQEyBbGiGenGcqKbVyvL9StbrJ9HFENj+PE3GHJ08qxeXExkCQQGg0CKWkiYz223Vtd3GjmHQFqWIrWmw0Zn1RDuXJAmRkNlrVikmsv/T/kFgItelmXbXoOH/CJdALTOmdPeW0+c/AkBy1aberRxac6GeOE5j1YO+ONJxii5JvsAR4O0VkhWkUydpCZ0f7cE6DB6NQKbK9YGxWlpWf32bOW6HnAuzFGArAkAO8ierWoZAKcggd6sCKazcN1OsOPunOXzZzJ6OZt5o99a0AJztJFIEGgPiHJ269GvMg5pW+MnjpTtK5rrSD7slAkBUgzjUGMMNLpx7PSU0BCd5D4iRVwjJ7UCd59OUVHbpAOm4PAMPRIM4nUK+4h3esOBKDhz+G8XtP09BLm7N1OkRAkEA/FHUOFYfhEczDCI4tuh3lbA34HzGlmFPpXkQqTKutQeVcZ7ER/M1f8uSDrYym5LrjM1GEOU6j02gyMBOCO0pvg==";
            String mySecretNoPadding = "m4n8SOqu8Z6amloq8tg9hS/fhguhyNOrikBhyJIwQxHJLF00yfw7mSYapYkT71edyrZWZBWzVUUCjYhC40To7u8YFuqSQkdSDF1ALWtXtGYlBOtZPFRxSqVDgo/jb7mZXgyxjtbqIi5W7TQoqBFLts5o5wXXk2BY=";
            String mySecretPCKS1 = "I5dIaAiB9OScIs2zqvGCVh2J26gX6fE/ggT5qEizhS4gfrmG3y/M1lLMR0Y8H1nyGFkerjRLgiHPAbS0OawEJbWaA2qtSzTa2Jlo6yuOx3ZAjwr4ojlPZDmkwn6sy1As2il+9twNtPyQmN0fk7c7j3Ni1plY1y5mf8lMJooHfSk=";

            byte[] pubBytes = Base64.decodeBase64(myPubKey);
            pubBytes = wrapRSA1024_X509(pubBytes);

            byte[] privBytes = Base64.decodeBase64(myPrivKey);
            privBytes = wrapRSA1024_PKCS8(privBytes);

            System.out.println("RSA-pub-ts-mod[" + pubenc.length + "]:"
                    + Base64.encodeBase64String(pubBytes));
            System.out.println("RSA-priv-ts-mod[" + privenc.length + "]:"
                    + Base64.encodeBase64String(privBytes));
            System.out.println("RSA-pub-ts-mod[" + pubenc.length + "]:"
                    + toHex(pubBytes));
            System.out.println("RSA-priv-ts-mod[" + privenc.length + "]:"
                    + toHex(privBytes));

            KeyFactory kf = KeyFactory.getInstance("RSA");

            X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(pubBytes);
            PublicKey publicKey = kf.generatePublic(pubSpec);

            PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(privBytes);
            PrivateKey privateKey = kf.generatePrivate(privSpec);

            System.out.println("RSA" + toString(getPrivateKeySpec(privateKey)));
            System.out.println("RSA" + toString(getPublicKeySpec(publicKey)));
            String encr2 = encryptRSA(publicKey, "blafasel12345678");
            System.out.println("RSA-encrypted:" + encr2);
            String decr2 = decryptRSA(privateKey, encr2);
            System.out.println("RSA-decrypted:" + decr2);
            System.out.println("RSA-pub-ts:"
                    + Base64.encodeBase64String(publicKey.getEncoded()));
            System.out.println("RSA-priv-ts:"
                    + Base64.encodeBase64String(privateKey.getEncoded()));

            byte[] iosBytes = Base64.decodeBase64(mySecretPCKS1);
            byte[] decr3 = decryptRSA(privateKey, iosBytes);
            System.out.println("RSA-decrypted-sec:" + new String(decr3));

        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (BadPaddingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            // } catch (InvalidAlgorithmParameterException e) {
            // // TODO Auto-generated catch block
            // e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}