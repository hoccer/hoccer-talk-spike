package com.hoccer.talk.servlets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.floreysoft.jmte.DefaultModelAdaptor;
import com.floreysoft.jmte.Engine;
import com.hoccer.talk.model.TalkClient;
import com.hoccer.talk.model.TalkClientHostInfo;
import com.hoccer.talk.model.TalkPresence;
import com.hoccer.talk.server.ITalkServerDatabase;
import com.hoccer.talk.server.TalkServer;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.jongo.MongoCollection;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class UserAdminServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(UserAdminServlet.class);

    private final Engine mEngine = new Engine();
    private final String mSearchboxTemplate = loadTemplate("/admin/searchbox.html");
    private final String mClientTemplate = loadTemplate("/admin/client.html");
    private final String mConfirmTemplate = loadTemplate("/admin/confirmdelete.html");
    private final String mStyleSheet = loadTemplate("/admin/admin.css");

    private static String loadTemplate(String path) {
        InputStream stream = UserAdminServlet.class.getResourceAsStream(path);

        try {
            return IOUtils.toString(stream);
        } catch (IOException e) {
            LOG.error("Error loading user admin template", e);
            return "";
        }
    }
    private TalkServer server;
    private ITalkServerDatabase db;

    private MongoCollection getCollection(String name) {
        return (MongoCollection)db.getRawCollection(name);
    }

    Hashtable validUsers = new Hashtable();

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        // ie this user has no password

        validUsers.put("admin:hcrSrv$23Admin;","authorized");
        mEngine.setModelAdaptor(new ResourceBundleModelAdapter());
    }

    // This method checks the user information sent in the Authorization
    // header against the database of users maintained in the users Hashtable.
    protected boolean allowUser(String auth) throws IOException {

        if (auth == null) {
            return false;  // no auth
        }
        if (!auth.toUpperCase().startsWith("BASIC ")) {
            return false;  // we only do BASIC
        }
        // Get encoded user and password, comes after "BASIC "
        String userpassEncoded = auth.substring(6);
        // Decode it, using any base 64 decoder
        sun.misc.BASE64Decoder dec = new sun.misc.BASE64Decoder();
        String userpassDecoded = new String(dec.decodeBuffer(userpassEncoded));

        // Check our user list to see if that user and password are "allowed"
        if ("authorized".equals(validUsers.get(userpassDecoded))) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void init() throws ServletException {
    }

    String showClientInfo(String clientId) throws JsonProcessingException {
        if (clientId != null) {
            TalkClient client = db.findClientById(clientId);
            TalkClient deletedClient = db.findDeletedClientById(clientId);
            boolean deleted = false;
            boolean recoverable = false;
            if (client == null && deletedClient != null) {
                deleted = true;
                client = deletedClient;
                if (client.getSrpSavedVerifier() != null) {
                    recoverable = true;
                }
            }
            if (client != null) {
                TalkClientHostInfo clientHostInfo = db.findClientHostInfoForClient(clientId);
                TalkPresence clientPresence = db.findPresenceForClient(clientId);
                HashMap<String, Object> model = new HashMap<String, Object>();
                ObjectMapper mapper = server.getJsonMapper();
                mapper.enable(SerializationFeature.INDENT_OUTPUT);

                model.put("stylesheet", mStyleSheet);
                model.put("clientId", clientId);
                model.put("clientInfo", mapper.writeValueAsString(client));
                model.put("clientHostInfo", mapper.writeValueAsString(clientHostInfo));
                model.put("clientPresence", mapper.writeValueAsString(clientPresence));
                model.put("search_action_url", "/admin/cidbyid");

                boolean suspended = client.isSuspended(new Date());

                if (deleted) {
                    model.put("delete_disabled", "disabled");
                    if (recoverable) {
                        model.put("undelete_disabled", "");
                    } else {
                        model.put("undelete_disabled", "disabled");
                    }
                    model.put("suspend_disabled", "disabled");
                    model.put("unspend_disabled", "disabled");
                } else {
                    model.put("delete_disabled", "");
                    model.put("undelete_disabled", "disabled");
                    if (!suspended) {
                        model.put("suspend_disabled", "");
                        model.put("unspend_disabled", "disabled");

                    } else {
                        model.put("suspend_disabled", "disabled");
                        model.put("unspend_disabled", "");
                    }
                }

                model.put("deleteLink", "/admin/delete/"+clientId);
                model.put("undeleteLink", "/admin/undelete/"+clientId);
                model.put("suspendLink", "/admin/suspend/"+clientId);
                model.put("unsuspendLink", "/admin/unsuspend/"+clientId);

                return mEngine.transform(mClientTemplate, model);
            }
        }
        HashMap<String, Object> model = new HashMap<String, Object>();
        model.put("search_action_url", "/admin/cidbyid");
        model.put("stylesheet", mStyleSheet);
        model.put("ErrorMessage", "Error: Client with this id not found");

        return mEngine.transform(mSearchboxTemplate, model);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        LOG.info("POST:");
        LOG.info("servletPath:"+request.getServletPath());
        LOG.info("pathInfo:"+request.getPathInfo());
        LOG.info("contextPath:"+request.getContextPath());
        LOG.info("requestURI:"+request.getRequestURI());
        LOG.info("parameterMap:"+request.getParameterMap());

        String auth = request.getHeader("Authorization");
        // Do we allow that user?
        if (!allowUser(auth)) {
            // Not allowed, so report he's unauthorized
            response.setHeader("WWW-Authenticate", "BASIC realm=\"Hoccer Server Admin\"");
            response.sendError(response.SC_UNAUTHORIZED);
            return;
        }

        server = (TalkServer)getServletContext().getAttribute("server");
        db = server.getDatabase();

        String pathInfo = request.getPathInfo();
        String body = "Error";

        if (pathInfo != null && pathInfo.startsWith("/suspend/")) {
            LOG.info("-> suspend");
            String[] components = pathInfo.split("/");
            String clientId = components[2];
            String durationString = request.getParameter("time");
            long duration = Long.parseLong(durationString) * 1000;
            LOG.info("suspend "+clientId+", duration="+duration);

            TalkClient client = db.findClientById(clientId);
            if (client != null) {
                db.suspendClient(client, new Date(), duration);
            }
            body = showClientInfo(clientId);
        } else if (pathInfo != null && pathInfo.startsWith("/unsuspend/")) {
            LOG.info("-> unsuspend");
            String[] components = pathInfo.split("/");
            String clientId = components[2];

            TalkClient client = db.findClientById(clientId);
            if (client != null) {
                db.unsuspendClient(client);
            }
            body = showClientInfo(clientId);
        } else if (pathInfo != null && pathInfo.startsWith("/delete/")) {
            LOG.info("-> delete");
            String[] components = pathInfo.split("/");
            String clientId = components[2];

            TalkClient client = db.findClientById(clientId);
            if (client != null) {
                String confirmation = request.getParameter("confirm");
                if ("yes".equals(confirmation)) {
                    LOG.info("!!! deleting client:" + clientId);
                    db.markClientDeleted(client, "Admin action");
                    server.getUpdateAgent().requestAccountDeletion(clientId);
                    body = showClientInfo(clientId);
                } else {
                    HashMap<String, Object> model = new HashMap<String, Object>();
                    model.put("stylesheet", mStyleSheet);
                    model.put("deleteLink", "/admin/delete/"+clientId);
                    model.put("cancelLink", "/admin/idsearchbox");
                    model.put("clientId", clientId);
                    LOG.info("Map:"+model);
                    body = mEngine.transform(mConfirmTemplate, model);
                }
            }
        } else {
            LOG.info("-> notfound");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().println("Not found");
            return;
        }
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/html; charset=UTF-8");
        response.getWriter().println(body);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        LOG.info("servletPath:"+request.getServletPath());
        LOG.info("pathInfo:"+request.getPathInfo());
        LOG.info("contextPath:"+request.getContextPath());
        LOG.info("requestURI:"+request.getRequestURI());
        LOG.info("parameterMap:"+request.getParameterMap());

        String auth = request.getHeader("Authorization");
        // Do we allow that user?
        if (!allowUser(auth)) {
            // Not allowed, so report he's unauthorized
            response.setHeader("WWW-Authenticate", "BASIC realm=\"Hoccer Server Admin\"");
            response.sendError(response.SC_UNAUTHORIZED);
            return;
        }

        server = (TalkServer)getServletContext().getAttribute("server");
        db = server.getDatabase();

        String pathInfo = request.getPathInfo();
        String body = "Error";
        if ("/idsearchbox".equals(pathInfo)) {
            LOG.info("-> idsearchbox");
            HashMap<String, Object> model = new HashMap<String, Object>();
            model.put("search_action_url", "cidbyid");
            model.put("stylesheet", mStyleSheet);
            body = mEngine.transform(mSearchboxTemplate, model);
        } else if (pathInfo != null && pathInfo.startsWith("/cidbyid")) {
            LOG.info("-> cidbyid");
            String clientId = request.getParameter("clientId");
            body = showClientInfo(clientId);
        } else {
            LOG.info("-> notfound");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().println("Not found");
            return;
        }
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/html; charset=UTF-8");
        response.getWriter().println(body);

        //UrlParameters parameters = new UrlParameters(request.getPathInfo());
        //Label label = LABELS.get(parameters.scheme);
/*
        if (label == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
*/
        //ResourceBundle resourceBundle = getResourceBundle(label, request.getLocale());
        //String userAgent = request.getHeader("User-Agent");
        //Platform platform = determinePlatform(userAgent);

        /*
        model.put("label", label.toString().toLowerCase());
        model.put("messages", resourceBundle);
        model.put("downloadLink", resourceBundle.getString("downloadLink" + platform));
        model.put("inviteLink", parameters.scheme + "://" + parameters.token);
        */

    }


    /*

     private ResourceBundle getResourceBundle(Label label, Locale locale) {
        return ResourceBundle.getBundle("invite/" + label.toString().toLowerCase() + "/messages", locale);
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
    */


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
