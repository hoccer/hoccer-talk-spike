package com.hoccer.talk.servlets;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.hoccer.talk.server.TalkServer;
import com.hoccer.talk.server.TalkServerConfiguration;
import com.hoccer.talk.server.cryptoutils.P12CertificateChecker;
import com.hoccer.talk.server.push.ApnsConfiguration;
import com.hoccer.talk.server.push.PushAgent;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

public class CertificateInfoServlet extends HttpServlet {

    private JsonFactory mJsonFactory;

    @Override
    public void init() throws ServletException {
        mJsonFactory = new JsonFactory();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);

        TalkServer server = (TalkServer)getServletContext().getAttribute("server");
        TalkServerConfiguration config = server.getConfiguration();

        JsonGenerator jsonGenerator = mJsonFactory.createJsonGenerator(response.getWriter());

        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectFieldStart("apns");

        for (Map.Entry<String, ApnsConfiguration> entry : config.getApnsConfigurations().entrySet()) {
            String clientName = entry.getKey();
            jsonGenerator.writeObjectFieldStart(clientName);

            ApnsConfiguration apnsConfiguration = entry.getValue();

            for (PushAgent.APNS_SERVICE_TYPE type : PushAgent.APNS_SERVICE_TYPE.values()) {
                jsonGenerator.writeObjectFieldStart(type.name().toLowerCase());

                try {
                    ApnsConfiguration.Certificate cert = apnsConfiguration.getCertificate(type);
                    P12CertificateChecker checker = new P12CertificateChecker(cert.getPath(), cert.getPassword());

                    jsonGenerator.writeBooleanField("exists", true);
                    jsonGenerator.writeStringField("expirationDate", checker.getCertificateExpiryDate().toString());
                    jsonGenerator.writeBooleanField("isExpired", checker.isExpired());
                } catch (IllegalArgumentException e) {
                    jsonGenerator.writeBooleanField("exists", false);
                }

                jsonGenerator.writeEndObject();
            }

            jsonGenerator.writeEndObject();
        }

        jsonGenerator.writeEndObject();
        jsonGenerator.writeEndObject();
        jsonGenerator.close();
    }
}
