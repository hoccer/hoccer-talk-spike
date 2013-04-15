package com.hoccer.talk.srp;

import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.agreement.srp.SRP6Client;

import java.math.BigInteger;
import java.util.Arrays;

public class SRP6VerifyingClient extends SRP6Client {

    protected byte[] s;
    protected byte[] I;

    protected byte[] K;

    protected byte[] M1;
    protected byte[] M2;

    @Override
    public BigInteger generateClientCredentials(byte[] salt, byte[] identity, byte[] password) {
        BigInteger credentials = super.generateClientCredentials(salt, identity, password);
        s = salt;
        I = identity;
        return credentials;
    }

    @Override
    public BigInteger calculateSecret(BigInteger serverB) throws CryptoException {
        BigInteger secret = super.calculateSecret(serverB);
        K = SRP6Verification.calculateHash(digest, secret);
        return secret;
    }

    public byte[] calculateVerifier() {
        M1 = SRP6Verification.calculateM1(digest, N, g, I, s, A, B, K);
        return M1;
    }

    public boolean verifyServer(byte[] M2s) {
        M2 = SRP6Verification.calculateM2(digest, A, M1);
        return Arrays.equals(M2, M2s);
    }

}
