package com.hoccer.talk.srp;

import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.agreement.srp.SRP6Server;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;

public class SRP6VerifyingServer extends SRP6Server {

    protected byte[] s;
    protected byte[] I;

    protected byte[] K;

    protected byte[] M1;
    protected byte[] M2;

    public void initVerifiable(
            BigInteger N, BigInteger g,
            BigInteger v, byte[] identifier, byte[] salt,
            Digest digest, SecureRandom random) {
        init(N, g, v, digest, random);
        s = salt;
        I = identifier;
    }

    @Override
    public BigInteger calculateSecret(BigInteger clientA) throws CryptoException {
        BigInteger secret = super.calculateSecret(clientA);
        K = SRP6Verification.calculateHash(digest, secret);
        return secret;
    }

    public byte[] verifyClient(byte[] M1c) {
        M1 = SRP6Verification.calculateM1(digest, N, g, I, s, A, B, K);

        M2 = SRP6Verification.calculateM2(digest, A, M1, K);

        if(!Arrays.equals(M1, M1c)) {
            return null;
        } else {
            return M2;
        }
    }

}
