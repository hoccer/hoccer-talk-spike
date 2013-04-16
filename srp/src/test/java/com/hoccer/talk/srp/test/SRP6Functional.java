package com.hoccer.talk.srp.test;

import com.hoccer.talk.srp.SRP6Parameters;
import com.hoccer.talk.srp.SRP6VerifyingClient;
import com.hoccer.talk.srp.SRP6VerifyingServer;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.agreement.srp.SRP6VerifierGenerator;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.digests.SHA224Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * SRP6 functional test
 *
 * This test verifies that all three SRP6 components (generator, client, server)
 * can work together successfully using various user/password, digest
 * and Diffie-Hellman parameter combinations.
 *
 */
public class SRP6Functional {

    // shared RNG
    SecureRandom random = new SecureRandom();

    // username and password used in non-randomized test
    byte[] aUser = "alice".getBytes();
    byte[] aPass = "password123".getBytes();

    // prove that we can deal with varying data
    @Test
    public void testRandomUserAndPassword() throws Exception {
        Digest digest = new SHA1Digest();
        byte[] user = new byte[16];
        byte[] pass = new byte[16];

        // do it once
        random.nextBytes(user);
        random.nextBytes(pass);
        test(digest, SRP6Parameters.CONSTANTS_1024, user, pass);

        // do it again
        random.nextBytes(user);
        random.nextBytes(pass);
        test(digest, SRP6Parameters.CONSTANTS_1024, user, pass);
    }

    // run test for all standard parameters using SHA-1
    @Test
    public void testSha1() throws Exception {
        Digest digest = new SHA1Digest();
        test(digest, SRP6Parameters.CONSTANTS_1024, aUser, aPass);
        test(digest, SRP6Parameters.CONSTANTS_2048, aUser, aPass);
        test(digest, SRP6Parameters.CONSTANTS_4096, aUser, aPass);
        test(digest, SRP6Parameters.CONSTANTS_8192, aUser, aPass);
    }

    // run test for common parameters using SHA-224
    @Test
    public void testSha224() throws Exception {
        Digest digest = new SHA224Digest();
        test(digest, SRP6Parameters.CONSTANTS_1024, aUser, aPass);
        test(digest, SRP6Parameters.CONSTANTS_2048, aUser, aPass);
    }

    // run test for common parameters using SHA-256
    @Test
    public void testSha256() throws Exception {
        Digest digest = new SHA256Digest();
        test(digest, SRP6Parameters.CONSTANTS_1024, aUser, aPass);
        test(digest, SRP6Parameters.CONSTANTS_2048, aUser, aPass);
    }

    // shared test routing
    private void test(Digest digest, SRP6Parameters params, byte[] user, byte[] pass) throws Exception {
        // generate salt, use digest size as advisable
        byte[] salt = new byte[digest.getDigestSize()];
        random.nextBytes(salt);

        // generate a verifier to authenticate with
        SRP6VerifierGenerator verifierGenerator = new SRP6VerifierGenerator();
        verifierGenerator.init(params.N, params.g, digest);
        BigInteger verifier = verifierGenerator.generateVerifier(salt, user, pass);

        // create both sides
        SRP6VerifyingClient client = new SRP6VerifyingClient();
        SRP6VerifyingServer server = new SRP6VerifyingServer();

        // initialize the client
        client.init(params.N, params.g, digest, random);

        // initialize the server
        server.initVerifiable(params.N, params.g, verifier, user, salt, digest, random);

        // CLIENT generates credentials
        BigInteger A = client.generateClientCredentials(salt, user, pass);

        // SERVER generates credentials
        BigInteger B = server.generateServerCredentials();

        // SERVER computes secret, verifying client credentials
        BigInteger serverS = server.calculateSecret(A);

        // CLIENT computes secret, verifying server credentials
        BigInteger clientS = client.calculateSecret(B);

        // verify that credentials match
        Assert.assertEquals("clientSecret != serverSecret", serverS, clientS);

        // CLIENT computes initial verifier
        byte[] M1 = client.calculateVerifier();

        // SERVER checks verifier, makes its own
        byte[] M2 = server.verifyClient(M1);
        Assert.assertNotNull("client could not prove possession of key", M2);

        // CLIENT verifies server verifier
        boolean verified = client.verifyServer(M2);
        Assert.assertTrue("server could not prove possession of key", verified);
    }

}
