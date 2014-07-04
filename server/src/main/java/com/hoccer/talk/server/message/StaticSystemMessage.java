package com.hoccer.talk.server.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoccer.talk.model.TalkClientHostInfo;
import org.apache.log4j.Logger;

public class StaticSystemMessage {

    private static final Logger LOG = Logger.getLogger(StaticSystemMessage.class);

    public static enum MESSAGES {

        UPDATE_NAGGING(
                "Bitte XO updaten und die aktuellste Version aus deinem App Store installieren! Ältere Versionen werden nicht mehr unterstützt!",
                "Please update XO and install the latest version from your App store! Outdated versions are no longer supported!");

        public String deu;
        public String eng;

        MESSAGES(String pDeu, String pEng) {
            this.deu = pDeu;
            this.eng = pEng;
        }
    }

    public static String generateMessage(String clientId, TalkClientHostInfo hostInfo, MESSAGES message) {
        LOG.info("generateMessage -clientId: '" + clientId + "' -hostInfo: '" + hostInfo + "' -message: '" + message);
        final ObjectMapper mapper = new ObjectMapper();
        try {
            LOG.info("hostInfo: " + mapper.writeValueAsString(hostInfo));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return message.deu;
    }
}
