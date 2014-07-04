package com.hoccer.talk.server.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoccer.talk.model.TalkClientHostInfo;
import org.apache.log4j.Logger;

import java.util.Locale;

public class StaticSystemMessage {

    private static final Logger LOG = Logger.getLogger(StaticSystemMessage.class);

    // TODO: maybe use something like "new Locale(code)" directly
    public static enum Language {
        GERMAN(Locale.GERMAN),
        ENGLISH(Locale.ENGLISH);

        public Locale locale;

        Language(Locale pLocale) {
            this.locale = pLocale;
        }

        public static Language fromString(String theISO639_1Code) {
            for (Language language : values()) {
                if (language.locale.getLanguage().equalsIgnoreCase(theISO639_1Code)) {
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
