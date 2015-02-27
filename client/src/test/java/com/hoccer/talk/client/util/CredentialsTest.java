package com.hoccer.talk.client.util;

import com.hoccer.talk.util.Credentials;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.print.attribute.standard.DateTimeAtCompleted;
import java.sql.SQLException;
import java.util.Date;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;

public class CredentialsTest {

    @Before
    public void testSetup() throws Exception {}

    @After
    public void testCleanup() throws SQLException {}

    @Test
    public void testCredentialConversion() {
        String clientId = "clientId";
        String clientName = "clientName";
        String password = "password";
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
    public void testCredentialConversionWithoutClientName() {
        String clientId = "clientId";
        String password = "password";
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
        String password = "password";
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
        assertEquals(clientName, credentials.getClientName());
        assertNull(newCredentials.getTimeStamp());
    }

    @Test
    public void testEncryptedCredentialConversion() {
        String clientId = "clientId";
        String clientName = "clientName";
        String password = "password";
        String salt = "salt";

        Credentials credentials = new Credentials(clientId, password, salt, clientName, new Date().getTime());

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
