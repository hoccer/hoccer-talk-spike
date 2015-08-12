package com.hoccer.talk.server;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.hoccer.scm.GitInfo;
import com.hoccer.talk.server.cryptoutils.P12CertificateChecker;
import com.hoccer.talk.server.database.JongoDatabase;
import com.hoccer.talk.server.database.migrations.DatabaseMigrationManager;
import com.hoccer.talk.server.push.ApnsConfiguration;
import com.hoccer.talk.server.push.PushAgent;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.jetty.jmx.ConnectorServer;
import org.eclipse.jetty.server.Server;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Properties;
import org.eclipse.jetty.jmx.MBeanContainer;

import javax.management.remote.JMXServiceURL;

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
        webServer.setHandler(new TalkServerHandler(talkServer));

        final Server managementServer = new Server(new InetSocketAddress(config.getManagementListenAddress(), config.getManagementListenPort()));
        managementServer.setStopAtShutdown(true);
        managementServer.setHandler(new TalkServerManagementHandler(talkServer));

        MBeanContainer mbContainer = new MBeanContainer(
        ManagementFactory.getPlatformMBeanServer());
        webServer.addBean(mbContainer);

        try {
            ConnectorServer s = new ConnectorServer(new JMXServiceURL("rmi", null, 1099, "/jndi/rmi://localhost:1099/jmxrmi"),
                    null, "org.eclipse.jetty.jmx:name=rmiconnectorserver");
            s.start();

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
}
