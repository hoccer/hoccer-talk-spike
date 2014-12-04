package com.hoccer.talk.server.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.floreysoft.jmte.Engine;
import com.hoccer.talk.model.TalkClient;
import com.hoccer.talk.model.TalkClientHostInfo;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class StaticSystemMessage {

    private static final Logger LOG = Logger.getLogger(StaticSystemMessage.class);

    public static enum Message {
        UPDATE_NAGGING(
                "Bitte XO updaten und die aktuellste Version aus deinem App Store installieren! Ältere Versionen werden nicht mehr unterstützt!", //${if storeLink} Besuche den Store: ${storeLink}${end}",
                "Please update XO and install the latest version from your App store! Outdated versions are no longer supported!" //${if storeLink} Visit the store: ${storeLink}${end}");
        ),
        UPDATE_SETTING_ENABLE_MP_MEDIA_ACCESS(
                "Zugriff auf die Musikbibliothek wurde aktiviert",
                "Music Library Access has been enabled"
        );
        public final String de;
        public final String en;

        Message(String pDeu, String pEng) {
            this.de = pDeu;
            this.en = pEng;
        }

        public static String getMessageForLanguage(Message pMessage, Language pLanguage) {
            if (pLanguage == Language.GERMAN) {
                return pMessage.de;
            } else if (pLanguage == Language.ENGLISH) {
                return pMessage.en;
            } else {
                throw new RuntimeException("unknown language: '" + pLanguage.name() + "'");
            }
        }
    }

    // TODO: maybe use something like "new Locale(code)" directly
    public static enum Language {
        GERMAN(Locale.GERMAN),
        ENGLISH(Locale.ENGLISH);

        public final Locale locale;

        Language(Locale pLocale) {
            this.locale = pLocale;
        }

        @Nullable
        public static Language fromString(String theISO639_1Code) {
            for (Language language : values()) {
                if (language.locale.getLanguage().equalsIgnoreCase(theISO639_1Code)) {
                    return language;
                }
            }
            return null;
        }
    }

    public final static Language DEFAULT_LANGUAGE = Language.GERMAN;

    private final Message mMessage;
    private final String mClientId;
    private final TalkClientHostInfo mClientHostInfo;

    public StaticSystemMessage(String pClientId, @Nullable TalkClientHostInfo pClientHostInfo, Message pMessage) {
        this.mClientId = pClientId;
        this.mClientHostInfo = pClientHostInfo;
        this.mMessage = pMessage;
    }

    private void dumpHostInfo() {
        // for debugging
        final ObjectMapper mapper = new ObjectMapper();
        try {
            LOG.info("hostInfo: " + mapper.writeValueAsString(this.mClientHostInfo));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public String generateMessage() {
        LOG.info("generateMessage -clientId: '" + this.mClientId + "' -hostInfo: '" + this.mClientHostInfo + "' -message: '" + this.mMessage);
        dumpHostInfo();

        String template = Message.getMessageForLanguage(this.mMessage, getLanguage());

        Engine engine = new Engine();
        Map<String, Object> model = new HashMap<String, Object>();

        model.put("language", getLanguage().locale.getLanguage());
        model.put("storeLink", getStoreLink());

        return engine.transform(template, model);
    }

    private Language getLanguage() {
        if (mClientHostInfo == null) {
            return DEFAULT_LANGUAGE;
        }

        Language res = Language.fromString(this.mClientHostInfo.getClientLanguage());
        if (res == null) {
            res = DEFAULT_LANGUAGE;
        }
        return res;
    }

    @Nullable
    private String getStoreLink() {
        // TODO this is just a quickshot to get things going. This is in fact more complex.
        /*
        * The Url in the respective store is also dependent on e.g. iOS bundleId (clientName)
        * */
        if (this.mClientHostInfo == null) {
            return null;
        }

        final String systemName = this.mClientHostInfo.getSystemName();
        if ("iPhone OS".equals(systemName)) {
            return "https://itunes.apple.com/de/app/hoccer-xo/id641387450?mt=8#";
        } else if ("Android".equals(systemName)) {
            return "https://play.google.com/store/apps/details?id=com.hoccer.xo.release";
        } else if ("TalkTool".equals(systemName)) {
            return "http://www.hoccer.com";
        } else {
            return null;
        }
    }
}
