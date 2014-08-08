package com.hoccer.talk.client;

public class XoClientSslConfiguration {

    /** Enable the session cache to improve connection latency */
    public static final boolean TLS_SESSION_CACHE_ENABLED = true;

    /** Limit the session cache to a small size */
    public static final int TLS_SESSION_CACHE_SIZE = 3;

    /** Allow only AES-based cipher suites */
    public static final String TLS_CIPHERS[] = {
        //"TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
        //"TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
        "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
        "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
        "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
        "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
        "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
        "TLS_RSA_WITH_AES_256_CBC_SHA",
        "TLS_RSA_WITH_AES_128_CBC_SHA",
    };

    /** Allow only TLS 1.1 and up */
    public static final String TLS_PROTOCOLS[] = {
            "TLSv1.2",
            "TLSv1.1",
            "TLSv1"
    };
}
