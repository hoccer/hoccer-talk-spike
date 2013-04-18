package com.hoccer.talk.srp;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.util.BigIntegers;

import java.math.BigInteger;

public class SRP6Verification {

    public static byte[] calculateHash(Digest digest, byte[] data) {
        byte[] output = new byte[digest.getDigestSize()];
        digest.update(data, 0, data.length);
        digest.doFinal(output, 0);
        return output;
    }

    public static byte[] calculateHash(Digest digest, BigInteger number) {
        byte[] bytes = BigIntegers.asUnsignedByteArray(number);
        return calculateHash(digest, bytes);
    }

    public static byte[] calculateH_Ng(Digest digest, BigInteger N, BigInteger g) {
        // calculate hashes using common digest
        byte[] HN = calculateHash(digest, N);
        byte[] Hg = calculateHash(digest, g);
        // XOR the two
        byte[] output = new byte[digest.getDigestSize()];
        for(int i = 0; i < output.length; i++) {
            output[i] = (byte)(HN[i] ^ Hg[i]);
        }
        // return result
        return output;
    }

    public static byte[] calculateM1(
            Digest digest,
            BigInteger N, BigInteger g,
            byte[] I,
            byte[] s,
            BigInteger A, BigInteger B,
            byte[] K
    ) {
        byte[] H_Ng = calculateH_Ng(digest, N, g);
        byte[] H_I  = calculateHash(digest, I);

        byte[] bA = BigIntegers.asUnsignedByteArray(A);
        byte[] bB = BigIntegers.asUnsignedByteArray(B);

        byte[] output = new byte[digest.getDigestSize()];

        digest.update(H_Ng, 0, H_Ng.length);
        digest.update(H_I, 0, H_I.length);
        digest.update(s, 0, s.length);
        digest.update(bA, 0, bA.length);
        digest.update(bB, 0, bB.length);
        digest.update(K, 0, K.length);

        digest.doFinal(output, 0);

        return output;
    }

    public static byte[] calculateM2(Digest digest, BigInteger A, byte[] M1, byte[] K) {
        byte[] bA  = BigIntegers.asUnsignedByteArray(A);

        byte[] output = new byte[digest.getDigestSize()];

        digest.update(bA, 0, bA.length);
        digest.update(M1, 0, M1.length);
        digest.update(K, 0, K.length);

        digest.doFinal(output, 0);

        return output;
    }

}
