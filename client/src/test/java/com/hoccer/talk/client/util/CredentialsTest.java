package com.hoccer.talk.client.util;

import com.hoccer.talk.util.Credentials;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Date;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;

public class CredentialsTest {

    @Before
    public void testSetup() throws Exception {}

    @After
    public void testCleanup() throws SQLException {}

    @Test
    public void credentialConversion() {
        String clientId = "clientId";
        String clientName = "clientName";
        String password = createSRPHexString();
        String salt = "salt";
        Long timestamp = new Date().getTime();

        Credentials credentials = new Credentials(clientId, password, salt, clientName, timestamp);

        assertEquals(clientId, credentials.getClientId());
        assertEquals(password, credentials.getPassword());
        assertEquals(salt, credentials.getSalt());
        assertEquals(clientName, credentials.getClientName());
        assertEquals(timestamp, credentials.getTimeStamp());

        // convert to string
        String jsonCredentialsString = credentials.toString();

        // create new credential instance
        Credentials newCredentials = Credentials.fromJsonString(jsonCredentialsString);

        assertEquals(clientId, newCredentials.getClientId());
        assertEquals(password, newCredentials.getPassword());
        assertEquals(salt, newCredentials.getSalt());
        assertEquals(clientName, newCredentials.getClientName());
        assertEquals(timestamp, newCredentials.getTimeStamp());
    }

    @Test
    public void credentialConversionWithoutClientName() {
        String clientId = "clientId";
        String password = createSRPHexString();
        String salt = "salt";
        Long timestamp = new Date().getTime();

        Credentials credentials = new Credentials(clientId, password, salt, null, timestamp);

        assertEquals(clientId, credentials.getClientId());
        assertEquals(password, credentials.getPassword());
        assertEquals(salt, credentials.getSalt());
        assertNull(credentials.getClientName());
        assertEquals(timestamp, credentials.getTimeStamp());

        // convert to string
        String jsonCredentialsString = credentials.toString();

        // create new credential instance
        Credentials newCredentials = Credentials.fromJsonString(jsonCredentialsString);

        assertEquals(clientId, newCredentials.getClientId());
        assertEquals(password, newCredentials.getPassword());
        assertEquals(salt, newCredentials.getSalt());
        assertNull(newCredentials.getClientName());
        assertEquals(timestamp, newCredentials.getTimeStamp());
    }

    @Test
    public void testCredentialConversionWithoutTimestamp() {
        String clientId = "clientId";
        String clientName = "clientName";
        String password = createSRPHexString();
        String salt = "salt";

        Credentials credentials = new Credentials(clientId, password, salt, clientName, null);

        assertEquals(clientId, credentials.getClientId());
        assertEquals(password, credentials.getPassword());
        assertEquals(salt, credentials.getSalt());
        assertEquals(clientName, credentials.getClientName());
        assertNull(credentials.getTimeStamp());

        // convert to string
        String jsonCredentialsString = credentials.toString();

        // create new credential instance
        Credentials newCredentials = Credentials.fromJsonString(jsonCredentialsString);

        assertEquals(clientId, newCredentials.getClientId());
        assertEquals(password, newCredentials.getPassword());
        assertEquals(salt, newCredentials.getSalt());
        assertEquals(clientName, newCredentials.getClientName());
        assertNull(newCredentials.getTimeStamp());
    }

    @Test
    public void encryptedCredentialConversion() {
        String clientId = "clientId";
        String clientName = "clientName";
        String password = createSRPHexString();
        String salt = "salt";
        Long timestamp = new Date().getTime();

        Credentials credentials = new Credentials(clientId, password, salt, clientName, timestamp);

        String encryptionPassword = "encryptionPassword";

        // convert to encrypted bytes
        byte[] encryptedCredentials = credentials.toEncryptedBytes(encryptionPassword);

        // create new credential instance
        Credentials newCredentials = Credentials.fromEncryptedBytes(encryptedCredentials, encryptionPassword);

        assertEquals(clientId, newCredentials.getClientId());
        assertEquals(password, newCredentials.getPassword());
        assertEquals(salt, newCredentials.getSalt());
        assertEquals(clientName, newCredentials.getClientName());
        assertEquals(timestamp, newCredentials.getTimeStamp());
    }

    @Test
    public void readAndroidCredentials() throws UnsupportedEncodingException {
        // credentials.json created with Hoccer Android (344)
        String encryptedJsonCredentialsWithPasswordAndSalt = "{\"container\":\"AESPBKDF2\",\"contentType\":\"credentials\",\"salt\":\"WTrDSUWKyE6RuU79DYJvOh+kKbY7QUErDnKRKRF0N3Y=\",\"ciphered\":\"4i92c80wtvWB7jZhyPmwli1d8jIcOaPvAVkFPCyTv6bgCvo/fkrZmaEeLVdB3Bik1KFyQhpZugVpoOizl8JG+zq0/RsEHS5ecA/2omz5RrT1xlseh919kVA2dQAfUegCRlmeCGcAcrFM8xAA7Hfqr2JsIvWQ2gJDbFVI8ZOfqOj/Ie3fhfKyfTznJ1C2LYVGwv0dYSposAzUmE1l7onePsOKquB4NO7Q0bgSQrlwWrhJm6PfxGvuxkrZ8YVngBfz+w8HofkW44Oi3Dwdp3W/c6AZUEC6/GEDKK9Z+7kOBAA=\"}";

        String encryptionPassword = "hoccer";
        String clientId = "042ab613-c4fb-4039-bc16-115f6c0adff3";
        String password = "d171c732ff9fb3ad7a0fc03898cde6ab47c436439495c19d450559925fd18f30";
        String salt = "be3f6bb44ab831640324f9f0cb941b367d5ff95fe203a9e843697d51a40f45a8";
        String clientName = "test";
        Credentials credentials = Credentials.fromEncryptedBytes(encryptedJsonCredentialsWithPasswordAndSalt.getBytes("UTF-8"), encryptionPassword);

        assertNotNull(credentials);
        assertEquals(clientId, credentials.getClientId());
        assertEquals(password, credentials.getPassword());
        assertEquals(salt, credentials.getSalt());
        assertEquals(clientName, credentials.getClientName());
        assertNull(credentials.getTimeStamp());
    }

    @Test
    public void readiOSLegacyCredentials() throws UnsupportedEncodingException {
        // credentials.json created with Hoccer iOS 3.2.10 (16091)
        String encryptedJsonCredentialsWithPasswordAndSalt = "{\"container\":\"AESPBKDF2\",\"contentType\":\"credentials\",\"salt\":\"5ott2bBwTLdvRcPzLFlxlPn81D2uZcW\\/9XBP9q0NCSM=\",\"ciphered\":\"kWhaIL+S64BetEf0FZ\\/9yKHCkcv768bDArptOyRvSnIHKFDNrhqHVfm8pgm\\/mdbQcsfNJuvlVL+axw2nat4fn170ULJo1onBjktYKMO0F1LmjI6nfbM93XEsW7xI1Lh51BH9lkc8PzjbwuS2KK81AopPSvDJos0JPzvSwiyRiMcHLZhN6EmvQhOdYHFug9x2VxcLtYR2vPpRjehHm1YCXXBhuvYUvVmkAGXw\\/W6yQqRTK7yc\\/Y7BAJmiqj8ZqgXtjp2ALvAZ7H\\/JuQEChVfzVg==\"}";

        String encryptionPassword = "hoccer";
        String clientId = "5221f703-ee18-429b-985d-1fae515edca9";
        String password = "2158666e566a4c6275473e2d5a5e4a466821612131487a";
        String salt = "33157ea87f122835a29012207e1e83d018470387e683147125deff34642b733f";
        Long timestamp = 1424264426652L;
        Credentials credentials = Credentials.fromEncryptedBytes(encryptedJsonCredentialsWithPasswordAndSalt.getBytes("UTF-8"), encryptionPassword);

        assertNotNull(credentials);
        assertEquals(clientId, credentials.getClientId());
        assertEquals(password, credentials.getPassword());
        assertEquals(salt, credentials.getSalt());
        assertNull(credentials.getClientName());
        assertEquals(timestamp, credentials.getTimeStamp());
    }

    private static String createSRPHexString() {
        final byte[] bytes = new byte[new SHA256Digest().getDigestSize()];
        new SecureRandom().nextBytes(bytes);
        return new String(Hex.encodeHex(bytes));
    }
}
