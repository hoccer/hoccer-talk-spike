package com.hoccer.talk.server;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.servlets.HealthCheckServlet;
import com.codahale.metrics.servlets.MetricsServlet;
import com.hoccer.scm.GitInfo;
import com.hoccer.talk.server.database.JongoDatabase;
import com.hoccer.talk.server.database.OrmliteDatabase;
import com.hoccer.talk.server.database.migrations.DatabaseMigrationManager;
import com.hoccer.talk.server.rpc.TalkRpcConnectionHandler;
import com.hoccer.talk.server.cryptoutils.*;
import com.hoccer.talk.servlets.CertificateInfoServlet;
import com.hoccer.talk.servlets.InvitationServlet;
import com.hoccer.talk.servlets.ServerInfoServlet;
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

        // select and instantiate database backend
        ITalkServerDatabase db = initializeDatabase(config);
        // ensure that the db is actually online and working
        db.reportPing();

        migrateDatabase(db);

        LOG.info("Initializing talk server");
        TalkServer talkServer = new TalkServer(config, db);

        LOG.info("Initializing jetty");
        Server webServer = new Server(new InetSocketAddress(config.getListenAddress(), config.getListenPort()));
        setupServerHandlers(webServer, talkServer);

        // TODO: take care of proper signal handling (?) here. We never see the "Server has quit" line, currently.
        // run and stop when interrupted
        try {
            LOG.info("Starting server");
            webServer.start();
            webServer.join();
            LOG.info("Server has quit");
        } catch (Exception e) {
            LOG.error("Exception in server", e);
        }
    }

    private void checkApnsCertificateExpirationStatus(TalkServerConfiguration config) {
        // report APNS expiry
        if (config.isApnsEnabled()) {
            final P12CertificateChecker p12ProductionVerifier = new P12CertificateChecker(
                    config.getApnsCertProductionPath(),
                    config.getApnsCertProductionPassword());
            final P12CertificateChecker p12SandboxVerifier = new P12CertificateChecker(
                    config.getApnsCertProductionPath(),
                    config.getApnsCertProductionPassword());
            try {
                LOG.info("APNS production cert expiryDate is: " + p12ProductionVerifier.getCertificateExpiryDate());
                LOG.info("APNS production cert expiration status: " + p12ProductionVerifier.isExpired());
                LOG.info("APNS sandbox cert expiryDate is: " + p12SandboxVerifier.getCertificateExpiryDate());
                LOG.info("APNS sandbox cert expiration status: " + p12SandboxVerifier.isExpired());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void setupServerHandlers(Server server, TalkServer talkServer) {
        // all metrics servlets are handled here
        ServletContextHandler metricsContextHandler = new ServletContextHandler();
        metricsContextHandler.setContextPath("/metrics");
        metricsContextHandler.setInitParameter("show-jvm-metrics", "true");

        metricsContextHandler.addEventListener(new MyMetricsServletContextListener(talkServer.getMetrics()));
        metricsContextHandler.addServlet(MetricsServlet.class, "/registry");

        metricsContextHandler.addEventListener(new MyHealtchecksServletContextListener(talkServer.getHealthCheckRegistry()));
        metricsContextHandler.addServlet(HealthCheckServlet.class, "/health");

        // handler for additional status information about the server
        ServletContextHandler serverInfoContextHandler = new ServletContextHandler();
        serverInfoContextHandler.setContextPath("/server");
        serverInfoContextHandler.setAttribute("server", talkServer);
        serverInfoContextHandler.addServlet(ServerInfoServlet.class, "/info");
        serverInfoContextHandler.addServlet(CertificateInfoServlet.class, "/certificates");

        // handler for invitation landing pages
        ServletContextHandler invitationContextHandler = new ServletContextHandler();
        invitationContextHandler.setContextPath("/invite");
        invitationContextHandler.setAttribute("server", talkServer);
        invitationContextHandler.addServlet(InvitationServlet.class, "/*");

        // handler for static files
        ContextHandler staticHandler = new ContextHandler("/static");
        ResourceHandler staticResourceHandler = new ResourceHandler();
        staticResourceHandler.setResourceBase(getClass().getResource("/static").toExternalForm());
        staticHandler.setHandler(staticResourceHandler);

        // handler for talk websocket connections
        WebSocketHandler clientHandler = new TalkRpcConnectionHandler(talkServer);

        // set server handlers
        HandlerCollection handlerCollection = new HandlerCollection();
        handlerCollection.addHandler(clientHandler);
        handlerCollection.addHandler(invitationContextHandler);
        handlerCollection.addHandler(staticHandler);
        handlerCollection.addHandler(serverInfoContextHandler);
        handlerCollection.addHandler(metricsContextHandler);
        server.setHandler(handlerCollection);
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

    private ITalkServerDatabase initializeDatabase(TalkServerConfiguration config) {
        LOG.info("Determining database");
        String backend = config.getDatabaseBackend();
        if ("jongo".equals(backend)) {
            return new JongoDatabase(config);
        } else if ("ormlite".equals(backend)) {
            return new OrmliteDatabase();
        }
        throw new RuntimeException("Unknown database backend: " + backend);
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
