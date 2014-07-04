package com.hoccer.talk.server.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoccer.talk.model.TalkClientHostInfo;
import org.apache.log4j.Logger;

public class StaticSystemMessage {

    private static final Logger LOG = Logger.getLogger(StaticSystemMessage.class);

    public static enum Language {
        GERMAN("de"),
        ENGLISH("en");

        public String iso639_1;
        Language(String pIso639value) {
            this.iso639_1 = pIso639value;
        }

        public static Language fromString(String theCode) {
            for (Language language : values()) {
                if (language.iso639_1.equalsIgnoreCase(theCode)) {
                    return language;
                }
            }
            return null;
        }
    }
    public static Language DEFAULT_LANGUAGE = Language.GERMAN;

    public static enum MESSAGES {
        UPDATE_NAGGING(
                "Bitte XO updaten und die aktuellste Version aus deinem App Store installieren! Ältere Versionen werden nicht mehr unterstützt!",
                "Please update XO and install the latest version from your App store! Outdated versions are no longer supported!");

        public String de;
        public String en;

        MESSAGES(String pDeu, String pEng) {
            this.de = pDeu;
            this.en = pEng;
        }

        public static String getMessageForLanguage(MESSAGES pMessage, Language pLanguage) {
            if (pLanguage == Language.GERMAN) {
                return pMessage.de;
            } else if (pLanguage == Language.ENGLISH) {
                return pMessage.en;
            } else {
                throw new RuntimeException("unknown language: '" + pLanguage.name() + "'");
            }
        }
    }

    public static String generateMessage(String clientId, TalkClientHostInfo hostInfo, MESSAGES message) {
        LOG.info("generateMessage -clientId: '" + clientId + "' -hostInfo: '" + hostInfo + "' -message: '" + message);
        final ObjectMapper mapper = new ObjectMapper();
        // for debugging
        try {
            LOG.info("hostInfo: " + mapper.writeValueAsString(hostInfo));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        Language clientLanguage = getLanguageFromClientHostInfo(hostInfo);
        String template = MESSAGES.getMessageForLanguage(message, clientLanguage);
        return template;
    }

    private static Language getLanguageFromClientHostInfo(TalkClientHostInfo pClientHostInfo) {
        if (pClientHostInfo == null) {
            return DEFAULT_LANGUAGE;
        }

        Language res = Language.fromString(pClientHostInfo.getClientLanguage());
        if (res == null) {
            res = DEFAULT_LANGUAGE;
        }
        return res;
    }
}
