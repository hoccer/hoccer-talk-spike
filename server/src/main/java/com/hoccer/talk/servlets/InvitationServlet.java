package com.hoccer.talk.servlets;

import com.floreysoft.jmte.Engine;
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

    private enum Platform {
        IOS,
        ANDROID,
        OTHER
    }

    private HashMap<Platform, String> mDownloadLinks = new HashMap<Platform, String>();
    private Engine mEngine = new Engine();
    private String mTemplate = "";

    @Override
    public void init() throws ServletException {
        // set download links
        mDownloadLinks.put(Platform.IOS, "https://itunes.apple.com/app/hoccer-xo/id641387450");
        mDownloadLinks.put(Platform.ANDROID, "https://play.google.com/store/apps/details?id=com.hoccer.xo.release");
        mDownloadLinks.put(Platform.OTHER, "http://hoccer.com");

        // load template
        InputStream stream = InvitationServlet.class.getResourceAsStream("/templates/inviteTemplate.html");
        try {
            mTemplate = IOUtils.toString(stream);
        } catch (IOException e) {
            LOG.error("Error loading invitation template", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String userAgent = request.getHeader("User-Agent");
        Platform platform = determinePlatform(userAgent);

        HashMap<String, Object> model = new HashMap<String, Object>();
        model.put("downloadLink", mDownloadLinks.get(platform));
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
}
