package com.hoccer.talk.srp.test;

import com.hoccer.talk.srp.SRP6Parameters;
import com.hoccer.talk.srp.SRP6VerifyingClient;
import com.hoccer.talk.srp.SRP6VerifyingServer;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.agreement.srp.SRP6VerifierGenerator;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.junit.Test;
import org.junit.Assert;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * SRP reference vector test
 *
 * This test verifies that the SRP implementation conforms
 * to the reference vector given in RFC5054.
 *
 * Verification includes generator, server and client.
 *
 * Only externally visible values are tested.
 *
 * Note that the secret S as well as client verification
 * values are not provided in the RFC and have therefore
 * been generated using this test.
 *
 */
public class SRP6Rfc5054Test {

    // pass null in case something tries to use random,
    // which we don't want for this vector test
    static final SecureRandom random = null;

    // standardized parameters
    static final Digest digest = new SHA1Digest();
    static final SRP6Parameters params = SRP6Parameters.CONSTANTS_1024;

    // standardized test vectors
    static final String user_RAW = "alice";
    static final String pass_RAW = "password123";
    static final String salt_HEX =
        "BEB25379D1A8581EB5A727673A2441EE";
    static final String verifier_HEX =
        "7E273DE8696FFC4F4E337D05B4B375BEB0DDE1569E8FA00A9886D812"+
        "9BADA1F1822223CA1A605B530E379BA4729FDC59F105B4787E5186F5"+
        "C671085A1447B52A48CF1970B4FB6F8400BBF4CEBFBB168152E08AB5"+
        "EA53D15C1AFF87B2B9DA6E04E058AD51CC72BFC9033B564E26480D78"+
        "E955A5E29E7AB245DB2BE315E2099AFB";
    static final String a_HEX =
        "60975527035CF2AD1989806F0407210BC81EDC04E2762A56AFD529DDDA2D4393";
    static final String A_HEX =
        "61D5E490F6F1B79547B0704C436F523DD0E560F0C64115BB72557EC4"+
        "4352E8903211C04692272D8B2D1A5358A2CF1B6E0BFCF99F921530EC"+
        "8E39356179EAE45E42BA92AEACED825171E1E8B9AF6D9C03E1327F44"+
        "BE087EF06530E69F66615261EEF54073CA11CF5858F0EDFDFE15EFEA"+
        "B349EF5D76988A3672FAC47B0769447B";
    static final String b_HEX =
        "E487CB59D31AC550471E81F00F6928E01DDA08E974A004F49E61F5D105284D20";
    static final String B_HEX =
        "BD0C61512C692C0CB6D041FA01BB152D4916A1E77AF46AE105393011"+
        "BAF38964DC46A0670DD125B95A981652236F99D9B681CBF87837EC99"+
        "6C6DA04453728610D0C6DDB58B318885D7D82C7F8DEB75CE7BD4FBAA"+
        "37089E6F9C6059F388838E7A00030B331EB76840910440B1B27AAEAE"+
        "EB4012B7D7665238A8E3FB004B117B58";

    // our own vectors generated using this test
    static final String S_HEX =
        "00b0dc82babcf30674ae450c0287745e7990a3381f63b387aaf271a1"+
        "0d233861e359b48220f7c4693c9ae12b0a6f67809f0876e2d013800d"+
        "6c41bb59b6d5979b5c00a172b4a2a5903a0bdcaf8a709585eb2afafa"+
        "8f3499b200210dcc1f10eb33943cd67fc88a2f39a4be5bec4ec0a321"+
        "2dc346d7e474b29ede8a469ffeca686e5a";
    static final String M1_HEX =
        "3f3bc67169ea71302599cf1b0f5d408b7b65d347";
    static final String M2_HEX =
        "9cab3c575a11de37d3ac1421a9f009236a48eb55";

    @Test
    public void testVector() throws CryptoException {
        byte[] user = user_RAW.getBytes();
        byte[] pass = pass_RAW.getBytes();

        // generate salt, use digest size as advisable
        byte[] salt = Utils.fromHexString(salt_HEX);

        // generate a verifier to authenticate with
        SRP6VerifierGenerator verifierGenerator = new SRP6VerifierGenerator();
        verifierGenerator.init(params.N, params.g, digest);
        BigInteger verifier = verifierGenerator.generateVerifier(salt, user, pass);

        // check the verifier
        BigInteger refVerifier = new BigInteger(verifier_HEX, 16);
        Assert.assertEquals("Incorrect verifier", refVerifier, verifier);

        // create both sides
        SRP6VerifyingClient client = new MockClient();
        SRP6VerifyingServer server = new MockServer();

        // initialize the client
        client.init(params.N, params.g, digest, random);

        // initialize the server
        server.initVerifiable(params.N, params.g, verifier, user, salt, digest, random);

        // CLIENT generates credentials
        BigInteger A = client.generateClientCredentials(salt, user, pass);

        // check client credentials
        BigInteger refA = new BigInteger(A_HEX, 16);
        Assert.assertEquals("Incorrect client credentials", refA, A);

        // SERVER generates credentials
        BigInteger B = server.generateServerCredentials();
        
        // check server credentials
        BigInteger refB = new BigInteger(B_HEX, 16);
        Assert.assertEquals("Incorrect server credentials", refB, B);

        // check secret on both sides
        BigInteger refS = new BigInteger(S_HEX, 16);
        // SERVER computes secret, verifying client credentials
        BigInteger serverS = server.calculateSecret(A);
        Assert.assertEquals("Server has computed incorrect secret", refS, serverS);
        // CLIENT computes secret, verifying server credentials
        BigInteger clientS = client.calculateSecret(B);
        Assert.assertEquals("Client has computed incorrect secret", refS, clientS);
        // verify that secrets match
        Assert.assertEquals("clientSecret != serverSecret", serverS, serverS);

        // CLIENT generates M1
        byte[] refM1 = Utils.fromHexString(M1_HEX);
        byte[] clientM1 = client.calculateVerifier();
        Assert.assertArrayEquals("Incorrect client M1", clientM1, refM1);

        // SERVER generates M2
        byte[] refM2 = Utils.fromHexString(M2_HEX);
        byte[] serverM2 = server.verifyClient(clientM1);
        Assert.assertArrayEquals("Incorrect server M2", serverM2, refM2);

        // CLIENT verifies M2
        client.verifyServer(serverM2);
    }

    /** Mock client overriding private value selection */
    private class MockClient extends SRP6VerifyingClient {
        @Override
        protected BigInteger selectPrivateValue() {
            return new BigInteger(a_HEX, 16);
        }
    }

    /** Mock server overriding private value selection */
    private class MockServer extends SRP6VerifyingServer {
        @Override
        protected BigInteger selectPrivateValue() {
            return new BigInteger(b_HEX, 16);
        }
    }

}
