package com.hoccer.talk.crypto.test;

import com.hoccer.talk.crypto.CryptoUtils;
import com.hoccer.talk.crypto.RSACryptor;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Test;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

public class RSATest {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    public void testRSA() throws Exception {
        KeyPair kp = RSACryptor.generateRSAKeyPair(1024);
        // LOG.finest("RSA" + toString(getPrivateKeySpec(kp)));
        // LOG.finest("RSA" + toString(getPublicKeySpec(kp)));
        String encr = RSACryptor.encryptRSA(kp.getPublic(), "blafasel12345678");
        byte[] encrIOS = RSACryptor.encryptRSA(kp.getPublic(),
                "blafasel12345678".getBytes("UTF-8"));
        System.out.println("RSA-encrypted-iOS:" + Base64.encodeBase64String(encrIOS));

        // LOG.finest("RSA-encrypted:" + encr);
        String decr = RSACryptor.decryptRSA(kp.getPrivate(), encr);
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

        byte[] pubencIOS = RSACryptor.unwrapRSA1024_X509_deprecated(pubenc);
        System.out.println("RSA-pub-IOS[" + pubencIOS.length + ":"
                + Base64.encodeBase64String(pubencIOS));

        byte[] pubencIOS_X = RSACryptor.unwrapRSA_X509(pubenc);
        System.out.println("RSA-pub-IOS[" + pubencIOS.length + ":"
                + Base64.encodeBase64String(pubencIOS_X));

        if (Arrays.equals(pubencIOS, pubencIOS_X)) {
            System.out.println("X509-UnWrapper RSA 1024 ok");
        } else {
            System.out.println("X509-UnWrapper RSA 1024 failed ###########");
        }

        byte[] privencIOS = RSACryptor.unwrapRSA1024_PKCS8_deprecated(privenc);
        System.out.println("RSA-priv-IOS[" + privencIOS.length + "]:"
                + Base64.encodeBase64String(privencIOS));

        byte[] pubWrapped = RSACryptor.wrapRSA1024_X509_deprecated(pubencIOS);
        byte[] privWrapped = RSACryptor.wrapRSA1024_PKCS8_deprecated(privencIOS);
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

        byte[] pubWBytes = Base64.decodeBase64(myPubKey);
        byte[] pubBytes = RSACryptor.wrapRSA1024_X509_deprecated(pubWBytes);
        byte[] pubXBytes = RSACryptor.wrapRSA_X509(pubWBytes);
        if (Arrays.equals(pubBytes, pubXBytes)) {
            System.out.println("X509-Wrapper RSA 1024 ok");
        } else {
            System.out.println("X509-UnWrapper RSA 1024 failed ###########");
            System.out.println("pubBytes :" + CryptoUtils.toHex(pubBytes));
            System.out.println("pubXBytes:" + CryptoUtils.toHex(pubXBytes));
        }

        byte[] privBytes = Base64.decodeBase64(myPrivKey);
        privBytes = RSACryptor.wrapRSA1024_PKCS8_deprecated(privBytes);

        System.out.println("RSA-pub-ts-mod[" + pubenc.length + "]:"
                + Base64.encodeBase64String(pubBytes));
        System.out.println("RSA-priv-ts-mod[" + privenc.length + "]:"
                + Base64.encodeBase64String(privBytes));
        System.out.println("RSA-pub-ts-mod[" + pubenc.length + "]:"
//                + Hex.encodeHexString(pubBytes));
                + new String(Hex.encodeHex(pubBytes)));
        System.out.println("RSA-priv-ts-mod[" + privenc.length + "]:"
//                + Hex.encodeHexString(privBytes));
                + new String(Hex.encodeHex(privBytes)));

        KeyFactory kf = KeyFactory.getInstance("RSA");

        X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(pubBytes);
        PublicKey publicKey = kf.generatePublic(pubSpec);

        PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(privBytes);
        PrivateKey privateKey = kf.generatePrivate(privSpec);

        System.out.println("RSA" + RSACryptor.toString(RSACryptor.getPrivateKeySpec(privateKey)));
        System.out.println("RSA" + RSACryptor.toString(RSACryptor.getPublicKeySpec(publicKey)));
        String encr2 = RSACryptor.encryptRSA(publicKey, "blafasel12345678");
        System.out.println("RSA-encrypted:" + encr2);
        String decr2 = RSACryptor.decryptRSA(privateKey, encr2);
        System.out.println("RSA-decrypted:" + decr2);
        System.out.println("RSA-pub-ts:"
                + Base64.encodeBase64String(publicKey.getEncoded()));
        System.out.println("RSA-priv-ts:"
                + Base64.encodeBase64String(privateKey.getEncoded()));

        byte[] iosBytes = Base64.decodeBase64(mySecretPCKS1);
        byte[] decr3 = RSACryptor.decryptRSA(privateKey, iosBytes);
        System.out.println("RSA-decrypted-sec:" + new String(decr3));

        testRSA2048();
    }

    public void testRSA2048() throws Exception {
        System.out.println("******* Testing RSA 2048 ***********");


        KeyPair kp = RSACryptor.generateRSAKeyPair(2048);
        // LOG.finest("RSA" + toString(getPrivateKeySpec(kp)));
        // LOG.finest("RSA" + toString(getPublicKeySpec(kp)));
        String encr = RSACryptor.encryptRSA(kp.getPublic(), "blafasel12345678");
        byte[] encrIOS = RSACryptor.encryptRSA(kp.getPublic(),
                "blafasel12345678".getBytes("UTF-8"));
        System.out.println("RSA-encrypted-iOS:" + Base64.encodeBase64String(encrIOS));

        // LOG.finest("RSA-encrypted:" + encr);
        String decr = RSACryptor.decryptRSA(kp.getPrivate(), encr);
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


        byte[] pubencIOS_X = RSACryptor.unwrapRSA_X509(pubenc);
        System.out.println("RSA-pub-IOS[" + pubencIOS_X.length + ":"
                + Base64.encodeBase64String(pubencIOS_X));


        // byte[] privencIOS = RSACryptor.unwrapRSA1024_PKCS8(privenc);
        // System.out.println("RSA-priv-IOS[" + privencIOS.length + "]:"
        //         + Base64.encodeBase64String(privencIOS));

        byte[] pubWrapped = RSACryptor.wrapRSA_X509(pubencIOS_X);
        // byte[] privWrapped = RSACryptor.wrapRSA1024_PKCS8(privencIOS);
        System.out.println("RSA-pubWrapped-ts[" + pubWrapped.length + "]:"
                + Base64.encodeBase64String(pubWrapped));
        // LOG.finest("RSA-pub-ts[" + pubWrapped + "]:" +
        // toHex(pubWrapped));
        //System.out.println("RSA-privWrapped-ts[" + privWrapped.length + "]:"
        //        + Base64.encodeBase64String(privWrapped));
        // LOG.finest("RSA-priv-ts[" + privWrapped + "]:" +
        // toHex(privWrapped));
        boolean pubOK = Arrays.equals(pubWrapped, pubenc);
        //boolean privOK = Arrays.equals(privWrapped, privenc);
        //System.out.println("RSA-wrapper-OK priv:[" + privOK + "]:" + " pub:["
        //        + pubOK + "]:");
        System.out.println("RSA-wrapper-OK pub:["
                + pubOK + "]:");

    }
}
