/**
 * Created by pavel on 15.03.14.
 */

package com.hoccer.talk.crypto;

import org.apache.commons.codec.binary.Base64;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.*;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class CryptoUtils {

    public static byte[] concat(byte[] first, byte[] second) {
        byte[] result = new byte[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    public static byte[] shorten(byte[] array, int length) {
        if (length == array.length) {
            return array;
        }
        byte[] result = new byte[length];
        System.arraycopy(array, 0, result, 0, length);
        return result;
    }

    public static byte[] skip(byte[] array, int skiplength) {
        byte[] result = new byte[array.length - skiplength];
        System.arraycopy(array, skiplength, result, 0, result.length);
        return result;
    }

    public static byte[] overwrite(byte[] array, byte[] into, int offset) {
        byte[] result = new byte[into.length];
        System.arraycopy(into, 0, result, 0, into.length);
        System.arraycopy(array, 0, result, offset, array.length);
        return result;
    }

    // String conversion
    public static String toHex(String txt) {
        return toHex(txt.getBytes());
    }

    public static String fromHex(String hex) {
        return new String(toByte(hex));
    }

    public static byte[] toByte(String hexString) {
        int len = hexString.length() / 2;
        byte[] result = new byte[len];
        for (int i = 0; i < len; i++)
            result[i] = Integer.valueOf(hexString.substring(2 * i, 2 * i + 2),
                    16).byteValue();
        return result;
    }

    public static String toHex(byte[] buf) {
        if (buf == null)
            return "";
        StringBuffer result = new StringBuffer(2 * buf.length);
        for (int i = 0; i < buf.length; i++) {
            appendHex(result, buf[i]);
        }
        return result.toString();
    }

    public static String toHex(byte theByte) {
        StringBuffer result = new StringBuffer(2);
        appendHex(result, theByte);
        return result.toString();
    }

    private final static String HEX = "0123456789abcdef";

    private static void appendHex(StringBuffer sb, byte b) {
        sb.append(HEX.charAt((b >> 4) & 0x0f)).append(HEX.charAt(b & 0x0f));
    }

    public static void printSet(String setName, Set algorithms) {
        System.out.println(setName + ":");
        if (algorithms.isEmpty()) {
            System.out.println("            None available.");
        } else {
            Iterator it = algorithms.iterator();
            while (it.hasNext()) {
                String name = (String) it.next();

                System.out.println("            " + name);
            }
        }
    }

    public static void listFeatures() {
        Provider[] providers = Security.getProviders();
        Set<String> ciphers = new HashSet<String>();
        Set<String> keyAgreements = new HashSet<String>();
        Set<String> macs = new HashSet<String>();
        Set<String> messageDigests = new HashSet<String>();
        Set<String> signatures = new HashSet<String>();
        Set<String> keyFactories = new HashSet<String>();

        for (int i = 0; i != providers.length; i++) {
            Iterator it = providers[i].keySet().iterator();

            while (it.hasNext()) {
                String entry = (String) it.next();

                if (entry.startsWith("Alg.Alias.")) {
                    entry = entry.substring("Alg.Alias.".length());
                }

                if (entry.startsWith("Cipher.")) {
                    ciphers.add(entry.substring("Cipher.".length()));
                } else if (entry.startsWith("KeyAgreement.")) {
                    keyAgreements.add(entry.substring("KeyAgreement.".length()));
                } else if (entry.startsWith("Mac.")) {
                    macs.add(entry.substring("Mac.".length()));
                } else if (entry.startsWith("MessageDigest.")) {
                    messageDigests.add(entry.substring("MessageDigest.".length()));
                } else if (entry.startsWith("Signature.")) {
                    signatures.add(entry.substring("Signature.".length()));
                } else if (entry.startsWith("SecretKeyFactory.")) {
                    keyFactories.add(entry.substring("SecretKeyFactory.".length()));
                }
            }
        }

        printSet("Ciphers", ciphers);
        printSet("KeyAgreeents", keyAgreements);
        printSet("Macs", macs);
        printSet("MessageDigests", messageDigests);
        printSet("Signatures", signatures);
        printSet("KeyFactories", keyFactories);
    }

    public static void listSecurityProviders() {
        try {
            Provider p[] = Security.getProviders();
            for (int i = 0; i < p.length; i++) {
                System.out.println(p[i]);
                for (Enumeration e = p[i].keys(); e.hasMoreElements();) {
                    System.out.println("\t" + e.nextElement());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String computeHmac(String contentDataUrl) throws NoSuchAlgorithmException, IOException {
        InputStream inputStream = new URL(contentDataUrl).openStream();
        return computeHmac(inputStream);
    }

    public static String computeHmac(byte[] data) throws NoSuchAlgorithmException, IOException {
        InputStream inputStream = new ByteArrayInputStream(data);
        return computeHmac(inputStream);
    }

    private static String computeHmac(InputStream inputStream) throws NoSuchAlgorithmException, IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA256");
            DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest);

            byte[] buffer = new byte[8192];
            int bytesRead;
            int totalBytesRead = 0;

            while ((bytesRead = digestInputStream.read(buffer)) != -1) {
                totalBytesRead += bytesRead;
            }
            System.out.println("[CryptoUtils::computeHmac] digested " + totalBytesRead + " bytes");

            return new String(Base64.encodeBase64(digest.digest()));
        } finally {
            inputStream.close();
        }
    }
}
