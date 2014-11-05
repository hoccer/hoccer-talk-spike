/**
 * Created by pavel on 15.03.14.
 */

package com.hoccer.talk.crypto.test;

import com.hoccer.talk.crypto.CryptoJSON;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Test;

import java.security.Security;

public class CryptoJSONTest {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    public void CryptoJSONTest() throws Exception {
        /*Ü
        byte[] testsalt = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
                17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32};
        byte[] testkey = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
                16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};
        */
        System.out.println("Starting CryptoJSONTest");

        //CryptoUtils.ListAlgorithms.listSecurityProviders();
        //CryptoUtils.listFeatures();

        String myClearText = "This is a new Text for testing222";
        byte[] myClearTextData = myClearText.getBytes("UTF-8");

        byte[] encryptedContainer = CryptoJSON.encrypt(myClearTextData, "myPassword", "test");
        System.out.println("createEncryptedContainer=" + new String(encryptedContainer, "UTF-8"));
        byte[] decryptedData = CryptoJSON.decrypt(encryptedContainer, "myPassword", "test");
        String decryptedString = new String(decryptedData, "UTF-8");
        System.out.println("decryptedString=" + decryptedString);

        if (!myClearText.equals(decryptedString)) {
            new RuntimeException("ERROR: encryption/decryption failed, myDecipheredText=" + decryptedString);
        }

        System.out.println("done CryptoJSONTest");
    }

}