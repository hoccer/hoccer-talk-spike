package com.hoccer.talk.srp.test;

import com.hoccer.talk.srp.SRP6Parameters;
import com.hoccer.talk.srp.SRP6VerifyingServer;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.junit.Test;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * SRP6 server safeguard test
 *
 * This verifies that the SRP server will reject
 * credentials that satisfy (A % N == 0), which
 * are considered unsafe.
 *
 */
public class SRP6SafeguardsServer {

    static Digest digest;
    static SecureRandom random;
    static SRP6Parameters params;
    static BigInteger verifier;

    static {
        digest = new SHA1Digest();
        random = new SecureRandom();
        params = SRP6Parameters.CONSTANTS_1024;
        verifier = new BigInteger("2342");
    }

    @Test(expected = CryptoException.class)
    public void testInvalidCredentials0() throws CryptoException {
        SRP6VerifyingServer s = new SRP6VerifyingServer();
        s.init(params.N, params.g, verifier, digest, random);
        s.calculateSecret(new BigInteger("0"));
    }

    @Test(expected = CryptoException.class)
    public void testInvalidCredentials2N() throws CryptoException {
        SRP6VerifyingServer s = new SRP6VerifyingServer();
        s.init(params.N, params.g, verifier, digest, random);
        s.calculateSecret(params.N.multiply(new BigInteger("2")));
    }

}
