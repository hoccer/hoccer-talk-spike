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
import java.util.HashMap;
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
        jsonGenerator.useDefaultPrettyPrinter();

        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectFieldStart("apns");

        writeApnsConfigurations(jsonGenerator, config.getApnsConfigurations());

        jsonGenerator.writeEndObject();
        jsonGenerator.writeEndObject();

        jsonGenerator.close();
    }

    private void writeApnsConfigurations(JsonGenerator jsonGenerator, HashMap<String, ApnsConfiguration> configurations) throws IOException {
        for (Map.Entry<String, ApnsConfiguration> entry : configurations.entrySet()) {
            String clientName = entry.getKey();

            jsonGenerator.writeObjectFieldStart(clientName);

            ApnsConfiguration apnsConfiguration = entry.getValue();
            writeApnsConfiguration(jsonGenerator, apnsConfiguration);

            jsonGenerator.writeEndObject();
        }
    }

    private void writeApnsConfiguration(JsonGenerator jsonGenerator, ApnsConfiguration apnsConfiguration) throws IOException {
        for (PushAgent.APNS_SERVICE_TYPE type : PushAgent.APNS_SERVICE_TYPE.values()) {
            jsonGenerator.writeObjectFieldStart(type.name().toLowerCase());

            ApnsConfiguration.Certificate cert = apnsConfiguration.getCertificate(type);
            writeApnsCertificate(jsonGenerator, cert);

            jsonGenerator.writeEndObject();
        }
    }

    private void writeApnsCertificate(JsonGenerator jsonGenerator, ApnsConfiguration.Certificate certificate) throws IOException {
        try {
            P12CertificateChecker checker = new P12CertificateChecker(certificate.getPath(), certificate.getPassword());

            jsonGenerator.writeBooleanField("exists", true);
            jsonGenerator.writeStringField("expirationDate", checker.getCertificateExpiryDate().toString());
            jsonGenerator.writeBooleanField("isExpired", checker.isExpired());
        } catch (IllegalArgumentException e) {
            jsonGenerator.writeBooleanField("exists", false);
        }
    }
}
