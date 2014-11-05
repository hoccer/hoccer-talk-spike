package com.hoccer.talk.client.util;

import com.hoccer.talk.util.Credentials;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;

import static junit.framework.TestCase.assertEquals;

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
        final String clientId = "clientId";
        final String clientName = "clientName";
        final String password = "password";
        final String salt = "salt";

        final Credentials credentials = new Credentials(clientId, clientName, password, salt);
        assertEquals(clientId, credentials.getClientId());
        assertEquals(clientName, credentials.getClientName());
        assertEquals(password, credentials.getPassword());
        assertEquals(salt, credentials.getSalt());

        // convert to string
        final String jsonCredentialsString = credentials.toString();

        // create new credential instance
        final Credentials newCredentials = Credentials.fromJsonString(jsonCredentialsString);
        assertEquals(clientId, newCredentials.getClientId());
        assertEquals(clientName, newCredentials.getClientName());
        assertEquals(password, newCredentials.getPassword());
        assertEquals(salt, newCredentials.getSalt());
    }

    @Test
    public void testEncryptedCredentialConversion() {
        final String clientId = "clientId";
        final String clientName = "clientName";
        final String password = "password";
        final String salt = "salt";

        final Credentials credentials = new Credentials(clientId, clientName, password, salt);
        assertEquals(clientId, credentials.getClientId());
        assertEquals(clientName, credentials.getClientName());
        assertEquals(password, credentials.getPassword());
        assertEquals(salt, credentials.getSalt());

        final String encryptionPassword = "encryptionPassword";

        // convert to encrypted bytes
        final byte[] encryptedCredentials = credentials.toEncryptedBytes(encryptionPassword);

        // create new credential instance
        final Credentials newCredentials = Credentials.fromEncryptedBytes(encryptedCredentials, encryptionPassword);
        assertEquals(clientId, newCredentials.getClientId());
        assertEquals(clientName, newCredentials.getClientName());
        assertEquals(password, newCredentials.getPassword());
        assertEquals(salt, newCredentials.getSalt());
    }
}
