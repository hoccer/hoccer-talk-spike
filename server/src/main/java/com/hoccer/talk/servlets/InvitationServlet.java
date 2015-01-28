package com.hoccer.talk.servlets;

import com.floreysoft.jmte.DefaultModelAdaptor;
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
import java.util.ResourceBundle;

public class InvitationServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(InvitationServlet.class);

    private enum Platform {
        IOS,
        ANDROID,
        OTHER
    }

    private final HashMap<Platform, String> mDownloadLinks = new HashMap<Platform, String>();
    private final Engine mEngine = new Engine();

    private TalkServerConfiguration mConfig;
    private String mTemplate;

    @Override
    public void init() throws ServletException {
        mDownloadLinks.put(Platform.IOS, "https://itunes.apple.com/app/hoccer/id340180776");
        mDownloadLinks.put(Platform.ANDROID, "https://play.google.com/store/apps/details?id=com.artcom.hoccer");
        mDownloadLinks.put(Platform.OTHER, "http://hoccer.com");

        TalkServer server = (TalkServer) getServletContext().getAttribute("server");
        mConfig = server.getConfiguration();

        mTemplate = loadTemplate("inviteTemplate.html");
        mEngine.setModelAdaptor(new ResourceBundleModelAdapter());
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

        HashMap<String, Object> model = new HashMap<String, Object>();
        model.put("messages", ResourceBundle.getBundle("messages/messages", request.getLocale()));
        model.put("downloadLink", mDownloadLinks.get(platform));
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

    private static Platform determinePlatform(String userAgent) {
        Platform platform = Platform.OTHER;

        if (userAgent.contains("iPhone") || userAgent.contains("iPad") || userAgent.contains("iPod")) {
            platform = Platform.IOS;
        } else if (userAgent.contains("Android")) {
            platform = Platform.ANDROID;
        }

        return platform;
    }

    private class ResourceBundleModelAdapter extends DefaultModelAdaptor {
        @Override
        protected Object getPropertyValue(Object o, String propertyName) {
            if (o instanceof ResourceBundle) {
                return ((ResourceBundle) o).getString(propertyName);
            }

            return super.getPropertyValue(o, propertyName);
        }
    }
}
