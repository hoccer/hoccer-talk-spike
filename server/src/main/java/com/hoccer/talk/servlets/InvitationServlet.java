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

public class InvitationServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(InvitationServlet.class);
    private TalkServerConfiguration mConfig;

    private enum Platform {
        IOS,
        ANDROID,
        OTHER
    }

    private static final HashMap<Platform, String> DOWNLOAD_LINKS = new HashMap<Platform, String>();
    static {
        DOWNLOAD_LINKS.put(Platform.IOS, "https://itunes.apple.com/app/hoccer-xo/id641387450");
        DOWNLOAD_LINKS.put(Platform.ANDROID, "https://play.google.com/store/apps/details?id=com.hoccer.xo.release");
        DOWNLOAD_LINKS.put(Platform.OTHER, "http://hoccer.com");
    }

    private Engine mEngine = new Engine();
    private String mInviteTemplate;
    private String mErrorTemplate;

    @Override
    public void init() throws ServletException {
        mInviteTemplate = loadTemplate("inviteTemplate.html");
        mErrorTemplate = loadTemplate("inviteErrorTemplate.html");

        TalkServer server = (TalkServer)getServletContext().getAttribute("server");
        mConfig = server.getConfiguration();
    }

    private String loadTemplate(String name) {
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

        String[] components = request.getPathInfo().split("/");
        String scheme = components.length > 1 ? components[1] : null;
        String token = components.length > 2 ? components[2] : null;

        String body;
        if (scheme != null && token != null && mConfig.getInviteUriSchemes().contains(scheme)) {
            body = renderInviteTemplate(platform, scheme, token);
        } else {
            body = renderErrorTemplate(platform);
        }

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/html; charset=UTF-8");
        response.getWriter().println(body);
    }

    private String renderInviteTemplate(Platform platform, String scheme, String token) throws IOException {
        HashMap<String, Object> model = new HashMap<String, Object>();
        model.put("downloadLink", DOWNLOAD_LINKS.get(platform));
        model.put("inviteLink", scheme + "://" + token);
        return mEngine.transform(mInviteTemplate, model);
    }

    private String renderErrorTemplate(Platform platform) {
        HashMap<String, Object> model = new HashMap<String, Object>();
        model.put("downloadLink", DOWNLOAD_LINKS.get(platform));
        return mEngine.transform(mErrorTemplate, model);
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
