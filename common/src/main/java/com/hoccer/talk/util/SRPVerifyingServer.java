package com.hoccer.talk.util;

import org.bouncycastle.crypto.agreement.srp.SRP6Server;

import java.math.BigInteger;

public class SRPVerifyingServer extends SRP6Server {

    protected byte[] s;
    protected byte[] I;

    protected byte[] K;

    protected byte[] M1;
    protected byte[] M2;

    private byte[] calculateH_N() {
        byte[] bN = N.toByteArray();

        byte[] output = new byte[digest.getDigestSize()];
        digest.update(bN, 0, bN.length);
        digest.doFinal(output, 0);

        return output;
    }

    private byte[] calculateH_g() {
        byte[] bg = g.toByteArray();

        byte[] output = new byte[digest.getDigestSize()];
        digest.update(bg, 0, bg.length);
        digest.doFinal(output, 0);

        return output;
    }

    private byte[] calculateH_Ng() {
        byte[] HN = calculateH_N();
        byte[] Hg = calculateH_g();

        byte[] output = new byte[digest.getDigestSize()];
        for(int i = 0; i < output.length; i++) {
            output[i] = (byte)(HN[i] ^ Hg[i]);
        }

        return output;
    }

    private byte[] calculateH_I() {
        byte[] output = new byte[digest.getDigestSize()];
        digest.update(I, 0, I.length);
        digest.doFinal(output, 0);

        return output;
    }

    private byte[] calculateK() {
        byte[] bS = S.toByteArray();

        byte[] output = new byte[digest.getDigestSize()];
        digest.update(bS, 0, bS.length);
        digest.doFinal(output, 0);

        return output;
    }

    private byte[] calculateM1() {
        byte[] H_Ng = calculateH_Ng();
        byte[] H_I  = calculateH_I();

        byte[] bA = A.toByteArray();
        byte[] bB = B.toByteArray();

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

    private byte[] calculateM2() {
        byte[] bA  = A.toByteArray();

        byte[] output = new byte[digest.getDigestSize()];

        digest.update(bA, 0, bA.length);
        digest.update(M1, 0, M1.length);

        digest.doFinal(output, 0);

        return output;
    }

}
