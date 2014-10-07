package com.hoccer.talk.crypto.test;

import com.hoccer.talk.crypto.AESCryptor;
import com.hoccer.talk.crypto.CryptoUtils;
import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Test;

import javax.crypto.Cipher;
import java.security.Security;

public class AESTest {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    public void testAES() throws Exception {

        byte[] testsalt = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
                17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32};
        byte[] testkey = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
                16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};

        byte[] nulliv = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

        System.out.println("AES encryption/decryption Testing:");
        System.out.println("salt=" + Base64.encodeBase64String(testsalt));
        System.out.println("salt=" + CryptoUtils.toHex(testsalt));

        Cipher c = AESCryptor.makeCipher(testkey, testsalt, nulliv, Cipher.ENCRYPT_MODE, "AES");
        byte[] encrypted = AESCryptor.crypt(c, new String("testdiufhduishiufhdiufduihfuidhufdhiufhiudhf").getBytes("UTF-8"));

        System.out.println("AES-encrypted=" + Base64.encodeBase64String(encrypted));
        System.out.println("AES-encrypted=" + CryptoUtils.toHex(encrypted));

        Cipher d = AESCryptor.makeCipher(testkey, testsalt, nulliv, Cipher.DECRYPT_MODE, "AES");
        byte[] decrypted = AESCryptor.crypt(d, encrypted);
        System.out.println("AES-decrypted=" + new String(decrypted));


        String myClearText = "This is a new Text for testing";
        String myCipherText = AESCryptor.encryptString(testkey, testsalt, myClearText);
        System.out.println("AES-b64-ciphertext: " + myCipherText);
        String myDecipheredText = AESCryptor.decryptString(testkey, testsalt, myCipherText);

        if (!myClearText.equals(myDecipheredText)) {
            new RuntimeException("ERROR: encryption/decryption failed, myDecipheredText=" + myDecipheredText);
        }

        // test key derivation
        byte[] derivedKey = AESCryptor.make256BitKeyFromPassword_PBKDF2WithHmacSHA256("myPassword", testsalt);
        System.out.println("PBKDF2WithHmacSHA256 derived key=" + CryptoUtils.toHex(derivedKey));
        String derivedTestKey = "55b8d68993154cad3642a15a64445db04ebd7a2eec5362f9294ecbc1d57a02db";
        System.out.println("test key                        =" + derivedTestKey);
        if (!derivedTestKey.equals(CryptoUtils.toHex(derivedKey))) {
            throw new Exception("PBKDF2WithHmacSHA256 unexpected result, key differs");
        }

        System.out.println("done test");
    }

}
