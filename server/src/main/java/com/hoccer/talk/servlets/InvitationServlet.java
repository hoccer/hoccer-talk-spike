package com.hoccer.talk.servlets;

import com.floreysoft.jmte.Engine;
import com.hoccer.talk.server.TalkServer;
import com.hoccer.talk.server.TalkServerConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

public class InvitationServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(InvitationServlet.class);

    private enum Platform {
        IOS,
        ANDROID,
        OTHER
    }

    private TalkServerConfiguration mConfig;
    private final HashMap<String, Map<String, Object>> mLocalizedMessages = new HashMap<String, Map<String, Object>>();
    private final Engine mEngine = new Engine();
    private String mTemplate;

    private static final HashMap<Platform, String> DOWNLOAD_LINKS = new HashMap<Platform, String>();

    static {
        DOWNLOAD_LINKS.put(Platform.IOS, "https://itunes.apple.com/app/hoccer/id340180776");
        DOWNLOAD_LINKS.put(Platform.ANDROID, "https://play.google.com/store/apps/details?id=com.artcom.hoccer");
        DOWNLOAD_LINKS.put(Platform.OTHER, "http://hoccer.com");
    }


    private static Map<String, Object> loadMessages(String filename) {
        Properties properties = new Properties();

        try {
            InputStream inputStream = InvitationServlet.class.getResourceAsStream("/messages/" + filename);
            properties.load(inputStream);
        } catch (IOException e) {
            LOG.error("Error loading localized messages", e);
        }

        return (Map) properties;
    }

    @Override
    public void init() throws ServletException {
        mLocalizedMessages.put("en", loadMessages("messages-en.properties"));
        mLocalizedMessages.put("de", loadMessages("messages-de.properties"));
        mTemplate = loadTemplate("inviteTemplate.html");

        TalkServer server = (TalkServer) getServletContext().getAttribute("server");
        mConfig = server.getConfiguration();
    }

    private static String loadTemplate(String name) {
        InputStream stream = InvitationServlet.class.getResourceAsStream("/templates/" + name);

        try {
            return IOUtils.toString(stream);
        } catch (IOException e) {
            LOG.error("Error loading invitation template", e);
            return "";
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String userAgent = request.getHeader("User-Agent");
        Platform platform = determinePlatform(userAgent);
        String language = determineLanguage(request.getLocale(), "en");

        HashMap<String, Object> model = new HashMap<String, Object>();
        model.put("messages", mLocalizedMessages.get(language));
        model.put("downloadLink", DOWNLOAD_LINKS.get(platform));
        model.put("inviteLink", extractInviteLink(request.getPathInfo()));

        String body = mEngine.transform(mTemplate, model);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/html; charset=UTF-8");
        response.getWriter().println(body);
    }

    private String extractInviteLink(String pathInfo) {
        String[] components = pathInfo.split("/");

        if (components.length > 2) {
            // pathInfo is expected to look like this: "/<scheme>/<token>"
            // components[0] is an empty string
            String scheme = components[1];
            String token = components[2];

            if (mConfig.getAllowedInviteUriSchemes().contains(scheme)) {
                return scheme + "://" + token;
            }
        }

        return null;
    }

    private String determineLanguage(Locale locale, String defaultLanguage) {
        String language = locale.getLanguage();

        if (language.isEmpty() || !mLocalizedMessages.containsKey(language)) {
            language = defaultLanguage;
        }

        return language;
    }

    private static Platform determinePlatform(String userAgent) {
        Platform platform = Platform.OTHER;

        if (userAgent.contains("iPhone") || userAgent.contains("iPad") || userAgent.contains("iPod")) {
            platform = Platform.IOS;
        } else if (userAgent.contains("Android")) {
            platform = Platform.ANDROID;
        }

        return platform;
    }
}
