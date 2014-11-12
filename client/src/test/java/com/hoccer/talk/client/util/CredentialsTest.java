package com.hoccer.talk.client.util;

import com.hoccer.talk.util.Credentials;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;

public class CredentialsTest {

    @Before
    public void testSetup() throws Exception {
        // nothing to do
    }

    @After
    public void testCleanup() throws SQLException {
        // nothing to do
    }

    @Test
    public void testCredentialConversion() {
        String clientId = "clientId";
        String clientName = "clientName";
        String password = "password";
        String salt = "salt";

        Credentials credentials = new Credentials(clientId, password, salt, clientName);

        assertEquals(clientId, credentials.getClientId());
        assertEquals(clientName, credentials.getClientName());
        assertEquals(password, credentials.getPassword());
        assertEquals(salt, credentials.getSalt());

        // convert to string
        String jsonCredentialsString = credentials.toString();

        // create new credential instance
        Credentials newCredentials = Credentials.fromJsonString(jsonCredentialsString);

        assertEquals(clientId, newCredentials.getClientId());
        assertEquals(clientName, newCredentials.getClientName());
        assertEquals(password, newCredentials.getPassword());
        assertEquals(salt, newCredentials.getSalt());
    }

    @Test
    public void testCredentialConversionWithoutClientName() {
        String clientId = "clientId";
        String password = "password";
        String salt = "salt";

        Credentials credentials = new Credentials(clientId, password, salt);

        assertEquals(clientId, credentials.getClientId());
        assertNull(credentials.getClientName());
        assertEquals(password, credentials.getPassword());
        assertEquals(salt, credentials.getSalt());

        // convert to string
        String jsonCredentialsString = credentials.toString();

        // create new credential instance
        Credentials newCredentials = Credentials.fromJsonString(jsonCredentialsString);

        assertEquals(clientId, newCredentials.getClientId());
        assertNull(newCredentials.getClientName());
        assertEquals(password, newCredentials.getPassword());
        assertEquals(salt, newCredentials.getSalt());
    }

    @Test
    public void testEncryptedCredentialConversion() {
        String clientId = "clientId";
        String clientName = "clientName";
        String password = "password";
        String salt = "salt";

        Credentials credentials = new Credentials(clientId, password, salt, clientName);

        assertEquals(clientId, credentials.getClientId());
        assertEquals(clientName, credentials.getClientName());
        assertEquals(password, credentials.getPassword());
        assertEquals(salt, credentials.getSalt());

        String encryptionPassword = "encryptionPassword";

        // convert to encrypted bytes
        byte[] encryptedCredentials = credentials.toEncryptedBytes(encryptionPassword);

        // create new credential instance
        Credentials newCredentials = Credentials.fromEncryptedBytes(encryptedCredentials, encryptionPassword);

        assertEquals(clientId, newCredentials.getClientId());
        assertEquals(clientName, newCredentials.getClientName());
        assertEquals(password, newCredentials.getPassword());
        assertEquals(salt, newCredentials.getSalt());
    }
}
