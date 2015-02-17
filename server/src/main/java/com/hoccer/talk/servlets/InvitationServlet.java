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

    public enum Label {
        HOCCER,
        HOCCME,
        SIMSME,
        STROEER
    }

    private static final HashMap<String, Label> LABELS = new HashMap<String, Label>();

    static {
        LABELS.put("hcr", Label.HOCCER);
        LABELS.put("hcrd", Label.HOCCER);
        LABELS.put("hoccme", Label.HOCCME);
        LABELS.put("hoccmed", Label.HOCCME);
        LABELS.put("hcrsms", Label.SIMSME);
        LABELS.put("hcrsmsd", Label.SIMSME);
        LABELS.put("strm", Label.STROEER);
        LABELS.put("strmd", Label.STROEER);
    }

    private final Engine mEngine = new Engine();
    private final String mTemplate = loadTemplate("/invite/common/inviteTemplate.html");

    private static String loadTemplate(String path) {
        InputStream stream = InvitationServlet.class.getResourceAsStream(path);

        try {
            return IOUtils.toString(stream);
        } catch (IOException e) {
            LOG.error("Error loading invitation template", e);
            return "";
        }
    }

    @Override
    public void init() throws ServletException {
        mEngine.setModelAdaptor(new ResourceBundleModelAdapter());
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        UrlParameters parameters = new UrlParameters(request.getPathInfo());
        Label label = LABELS.get(parameters.scheme);

        if (label == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        ResourceBundle resourceBundle = getResourceBundle(label, request.getLocale());
        String userAgent = request.getHeader("User-Agent");
        Platform platform = determinePlatform(userAgent);

        HashMap<String, Object> model = new HashMap<String, Object>();
        model.put("label", label.toString().toLowerCase());
        model.put("messages", resourceBundle);
        model.put("downloadLink", resourceBundle.getString("downloadLink" + platform));
        model.put("inviteLink", parameters.scheme + "://" + parameters.token);

        String body = mEngine.transform(mTemplate, model);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/html; charset=UTF-8");
        response.getWriter().println(body);
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
        return ResourceBundle.getBundle("invite/" + label.toString().toLowerCase() + "/messages", locale);
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
        @Nullable public final String scheme;
        @Nullable public final String token;

        public UrlParameters(String pathInfo) {
            // pathInfo is expected to look like this: "/<scheme>/<token>"
            String[] components = pathInfo.split("/");

            if (components.length > 2) {
                // components[0] is an empty string
                scheme = components[1];
                token = components[2];
            } else {
                scheme = null;
                token = null;
            }
        }
    }
}
