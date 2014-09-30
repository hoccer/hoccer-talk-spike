package com.hoccer.talk.server.push;

import java.util.HashMap;

public class ApnsConfiguration {
    private final HashMap<PushAgent.APNS_SERVICE_TYPE, Certificate> mCertificates = new HashMap<PushAgent.APNS_SERVICE_TYPE, Certificate>();

    public ApnsConfiguration(String productionCertPath, String productionCertPassword, String sandboxCertPath, String sandboxCertPassword) {
        mCertificates.put(PushAgent.APNS_SERVICE_TYPE.PRODUCTION, new Certificate(productionCertPath, productionCertPassword));
        mCertificates.put(PushAgent.APNS_SERVICE_TYPE.SANDBOX, new Certificate(sandboxCertPath, sandboxCertPassword));
    }

    public Certificate getCertificate(PushAgent.APNS_SERVICE_TYPE type) {
        return mCertificates.get(type);
    }

    public class Certificate {
        private final String mPath;
        private final String mPassword;

        public Certificate(String path, String password) {
            mPath = path;
            mPassword = password;
        }

        public String getPath() {
            return mPath;
        }

        public String getPassword() {
            return mPassword;
        }
    }
}
