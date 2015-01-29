package com.hoccer.talk.server;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.servlets.HealthCheckServlet;
import com.codahale.metrics.servlets.MetricsServlet;
import com.hoccer.scm.GitInfo;
import com.hoccer.talk.server.cryptoutils.P12CertificateChecker;
import com.hoccer.talk.server.database.JongoDatabase;
import com.hoccer.talk.server.database.migrations.DatabaseMigrationManager;
import com.hoccer.talk.server.push.ApnsConfiguration;
import com.hoccer.talk.server.push.PushAgent;
import com.hoccer.talk.server.rpc.TalkRpcConnectionHandler;
import com.hoccer.talk.servlets.CertificateInfoServlet;
import com.hoccer.talk.servlets.InvitationServlet;
import com.hoccer.talk.servlets.PushMessageServlet;
import com.hoccer.talk.servlets.ServerInfoServlet;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.WebSocketHandler;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Entrypoint to the Talk server
 */
public class TalkServerMain {

    private static final Logger LOG = Logger.getLogger(TalkServerMain.class);

    @Parameter(names = {"-c", "-config"},
            description = "Configuration file to use")
    private final String config = null;

    private void run() {
        // load configuration
        TalkServerConfiguration config = initializeConfiguration();
        config.report();

        checkApnsCertificateExpirationStatus(config);

        // instantiate database backend
        ITalkServerDatabase db = new JongoDatabase(config);
        // ensure that the db is actually online and working
        db.reportPing();

        migrateDatabase(db);

        LOG.info("Initializing talk server");
        TalkServer talkServer = new TalkServer(config, db);

        LOG.info("Initializing jetty");
        final Server webServer = new Server(new InetSocketAddress(config.getListenAddress(), config.getListenPort()));
        webServer.setStopAtShutdown(true);
        setupServerHandlers(webServer, talkServer);

        final Server managementServer = new Server(new InetSocketAddress(config.getManagementListenAddress(), config.getManagementListenPort()));
        managementServer.setStopAtShutdown(true);
        setupManagementServerHandlers(managementServer, talkServer);

        try {
            LOG.info("Starting server");
            webServer.start();
            managementServer.start();
            webServer.join();
            managementServer.join();
            LOG.info("Server has quit");
        } catch (Exception e) {
            LOG.error("Exception in server", e);
            LOG.error("Server has quit abnormally");
            System.exit(1);
        }
    }

    private void checkApnsCertificateExpirationStatus(TalkServerConfiguration config) {
        // report APNS expiry
        if (config.isApnsEnabled()) {
            for (Map.Entry<String, ApnsConfiguration> entry : config.getApnsConfigurations().entrySet()) {
                String clientName = entry.getKey();
                ApnsConfiguration apnsConfiguration = entry.getValue();

                for (PushAgent.APNS_SERVICE_TYPE type : PushAgent.APNS_SERVICE_TYPE.values()) {
                    ApnsConfiguration.Certificate cert = apnsConfiguration.getCertificate(type);
                    final P12CertificateChecker checker = new P12CertificateChecker(cert.getPath(), cert.getPassword());

                    try {
                        LOG.info("APNS " + type + " cert expiryDate is: " + checker.getCertificateExpiryDate());
                        LOG.info("APNS " + type + " cert expiration status: " + checker.isExpired());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void setupServerHandlers(Server server, TalkServer talkServer) {
        HandlerCollection handlerCollection = new HandlerCollection();

        addMetricsServlets(handlerCollection, talkServer);
        addServerStatusServlets(handlerCollection, talkServer);
        addInvitationServlet(handlerCollection);
        addStaticResourceHandlers(handlerCollection);
        addTalkRpcConnectionHandler(handlerCollection, talkServer);

        server.setHandler(handlerCollection);
    }

    private void addMetricsServlets(HandlerCollection handlerCollection, TalkServer talkServer) {
        ServletContextHandler metricsContextHandler = new ServletContextHandler();
        handlerCollection.addHandler(metricsContextHandler);

        metricsContextHandler.setContextPath("/metrics");
        metricsContextHandler.setInitParameter("show-jvm-metrics", "true");

        metricsContextHandler.addEventListener(new MyMetricsServletContextListener(talkServer.getMetrics()));
        metricsContextHandler.addServlet(MetricsServlet.class, "/registry");

        metricsContextHandler.addEventListener(new MyHealtchecksServletContextListener(talkServer.getHealthCheckRegistry()));
        metricsContextHandler.addServlet(HealthCheckServlet.class, "/health");
    }

    private void addServerStatusServlets(HandlerCollection handlerCollection, TalkServer talkServer) {
        ServletContextHandler serverInfoContextHandler = new ServletContextHandler();
        handlerCollection.addHandler(serverInfoContextHandler);

        serverInfoContextHandler.setContextPath("/server");
        serverInfoContextHandler.setAttribute("server", talkServer);
        serverInfoContextHandler.addServlet(ServerInfoServlet.class, "/info");
        serverInfoContextHandler.addServlet(CertificateInfoServlet.class, "/certificates");
    }

    private void addInvitationServlet(HandlerCollection handlerCollection) {
        ServletContextHandler invitationContextHandler = new ServletContextHandler();
        handlerCollection.addHandler(invitationContextHandler);

        invitationContextHandler.setContextPath("/invite");
        invitationContextHandler.addServlet(InvitationServlet.class, "/*");
    }

    private void addStaticResourceHandlers(HandlerCollection handlerCollection) {
        try {
            List<String> directories = IOUtils.readLines(getClass().getResourceAsStream("/invite/"), Charsets.UTF_8);

            for (String directory : directories) {
                addStaticResourceHandler(handlerCollection, directory);
            }
        } catch (IOException e) {
            LOG.fatal("Failed to load static resources", e);
            System.exit(1);
        }
    }

    private void addStaticResourceHandler(HandlerCollection handlerCollection, String directory) {
        ContextHandler staticHandler = new ContextHandler("/static/" + directory);
        handlerCollection.addHandler(staticHandler);

        ResourceHandler staticResourceHandler = new ResourceHandler();
        String resourceBase = getClass().getResource("/invite/" + directory + "/static").toExternalForm();
        staticResourceHandler.setResourceBase(resourceBase);
        staticHandler.setHandler(staticResourceHandler);
    }

    private void addTalkRpcConnectionHandler(HandlerCollection handlerCollection, TalkServer talkServer) {
        WebSocketHandler clientHandler = new TalkRpcConnectionHandler(talkServer);
        handlerCollection.addHandler(clientHandler);
    }

    private void setupManagementServerHandlers(Server managementServer, TalkServer talkServer) {
        HandlerCollection handlerCollection = new HandlerCollection();
        addPushMessageServlet(handlerCollection, talkServer);
        managementServer.setHandler(handlerCollection);
    }

    private void addPushMessageServlet(HandlerCollection handlerCollection, TalkServer talkServer) {
        ServletContextHandler pushMessageHandler = new ServletContextHandler();
        handlerCollection.addHandler(pushMessageHandler);

        pushMessageHandler.setContextPath("/push");
        pushMessageHandler.setAttribute("server", talkServer);
        pushMessageHandler.addServlet(PushMessageServlet.class, "/*");
    }

    private void migrateDatabase(ITalkServerDatabase database) {
        LOG.info("applying database migrations");
        DatabaseMigrationManager migrationManager = new DatabaseMigrationManager(database);
        migrationManager.executeAllMigrations();
    }

    private TalkServerConfiguration initializeConfiguration() {
        LOG.info("Determining configuration");
        TalkServerConfiguration configuration = new TalkServerConfiguration();

        // configure from file
        if (config != null) {
            Properties properties = null;
            LOG.info("Loading configuration from property file: '" + config + "'");
            try {
                FileInputStream configIn = new FileInputStream(config);
                properties = new Properties();
                properties.load(configIn);
            } catch (FileNotFoundException e) {
                LOG.error("Could not load configuration", e);
            } catch (IOException e) {
                LOG.error("Could not load configuration", e);
            }
            if (properties != null) {
                configuration.configureFromProperties(properties);
            }
        }

        // also read additional bundled property files
        LOG.info("Loading bundled properties (server.properties)...");
        Properties bundled_properties = new Properties();
        try {
            InputStream bundledConfigIs = TalkServerConfiguration.class.getResourceAsStream("/server.properties");
            bundled_properties.load(bundledConfigIs);
            configuration.setVersion(bundled_properties.getProperty("version"));
        } catch (IOException e) {
            LOG.error("Unable to load bundled configuration", e);
        }

        LOG.info("Loading GIT properties (git.properties)...");
        Properties git_properties = new Properties();
        try {
            InputStream gitConfigIs = TalkServerConfiguration.class.getResourceAsStream("/git.properties");
            if (gitConfigIs != null) {
                git_properties.load(gitConfigIs);
                configuration.setGitInfo(GitInfo.initializeFromProperties(git_properties));
            }
        } catch (IOException e) {
            LOG.error("Unable to load bundled configuration", e);
        }

        return configuration;
    }

    public static void main(String[] args) {
        BasicConfigurator.configure();
        TalkServerMain main = new TalkServerMain();
        new JCommander(main, args);
        // hand the property file over to log4j mechanism as well
        PropertyConfigurator.configure(main.config);
        main.run();
    }

    private static class MyMetricsServletContextListener extends MetricsServlet.ContextListener {
        private final MetricRegistry _metricRegistry;

        public MyMetricsServletContextListener(MetricRegistry metricRegistry) {
            _metricRegistry = metricRegistry;
        }

        @Override
        protected MetricRegistry getMetricRegistry() {
            return _metricRegistry;
        }
    }

    private static class MyHealtchecksServletContextListener extends HealthCheckServlet.ContextListener {
        private final HealthCheckRegistry _healthCheckRegistry;

        public MyHealtchecksServletContextListener(HealthCheckRegistry healthCheckRegistry) {
            _healthCheckRegistry = healthCheckRegistry;
        }

        @Override
        protected HealthCheckRegistry getHealthCheckRegistry() {
            return _healthCheckRegistry;
        }
    }
}
