package com.hoccer.talk.client;

public class XoClientSslConfiguration {

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

    public static final String TLS_PROTOCOLS[] = {
            "TLSv1.2",
            "TLSv1.1",
            "TLSv1"
    };
}
