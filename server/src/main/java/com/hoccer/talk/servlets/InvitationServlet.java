package com.hoccer.talk.servlets;

import com.floreysoft.jmte.DefaultModelAdaptor;
import com.floreysoft.jmte.Engine;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;

public class InvitationServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(InvitationServlet.class);

    private enum Platform {
        IOS,
        ANDROID,
        OTHER
    }

    private enum Label {
        HOCCER,
        STROEER
    }

    private static final HashMap<String, Label> LABELS = new HashMap<String, Label>();

    static {
        LABELS.put("hcr", Label.HOCCER);
        LABELS.put("hcrd", Label.HOCCER);
        LABELS.put("strm", Label.STROEER);
        LABELS.put("strmd", Label.STROEER);
    }

    private final HashMap<Platform, String> mDownloadLinks = new HashMap<Platform, String>();
    private final Engine mEngine = new Engine();

    private String mTemplate;

    @Override
    public void init() throws ServletException {
        mDownloadLinks.put(Platform.IOS, "https://itunes.apple.com/app/hoccer/id340180776");
        mDownloadLinks.put(Platform.ANDROID, "https://play.google.com/store/apps/details?id=com.artcom.hoccer");
        mDownloadLinks.put(Platform.OTHER, "http://hoccer.com");

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
        UrlParameters parameters = new UrlParameters(request.getPathInfo());
        Label label = LABELS.get(parameters.scheme);

        if (label != null) {
            HashMap<String, Object> model = new HashMap<String, Object>();
            model.put("messages", getResourceBundle(label, request.getLocale()));
            model.put("downloadLink", mDownloadLinks.get(platform));
            model.put("inviteLink", parameters.scheme + "://" + parameters.token);

            String body = mEngine.transform(mTemplate, model);

            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("text/html; charset=UTF-8");
            response.getWriter().println(body);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
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

    private ResourceBundle getResourceBundle(Label label, Locale locale) {
        return ResourceBundle.getBundle("label/" + label.toString().toLowerCase() + "/messages", locale);
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

    private class UrlParameters {
        @Nullable public String scheme;
        @Nullable public String token;

        public UrlParameters(String pathInfo) {
            String[] components = pathInfo.split("/");

            if (components.length > 2) {
                // pathInfo is expected to look like this: "/<scheme>/<token>", components[0] is an empty string
                scheme = components[1];
                token = components[2];
            }
        }
    }
}
