package com.hoccer.talk.srp.test;

import com.hoccer.talk.srp.SRP6Parameters;
import com.hoccer.talk.srp.SRP6VerifyingClient;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.junit.Test;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * SRP6 client safeguard test
 *
 * This verifies that the SRP client will reject
 * credentials that satisfy (B % N == 0), which
 * are considered unsafe.
 *
 */
public class SRP6ClientSafeguardsTest {

    static Digest digest;
    static SecureRandom random;
    static SRP6Parameters params;

    static byte[] user;
    static byte[] pass;
    static byte[] salt;

    static {
        digest = new SHA1Digest();
        random = new SecureRandom();
        params = SRP6Parameters.CONSTANTS_1024;
        user = new byte[digest.getDigestSize()];
        random.nextBytes(user);
        pass = new byte[digest.getDigestSize()];
        random.nextBytes(pass);
        salt = new byte[digest.getDigestSize()];
        random.nextBytes(salt);
    }

    @Test(expected = CryptoException.class)
    public void testInvalidCredentials0() throws CryptoException {
        SRP6VerifyingClient c = new SRP6VerifyingClient();
        c.init(params.N, params.g, digest, random);
        c.generateClientCredentials(salt, user, pass);
        c.calculateSecret(new BigInteger("0"));
    }

    @Test(expected = CryptoException.class)
    public void testInvalidCredentials2N() throws CryptoException {
        SRP6VerifyingClient c = new SRP6VerifyingClient();
        c.init(params.N, params.g, digest, random);
        c.generateClientCredentials(salt, user, pass);
        c.calculateSecret(params.N.multiply(new BigInteger("2")));
    }

}
