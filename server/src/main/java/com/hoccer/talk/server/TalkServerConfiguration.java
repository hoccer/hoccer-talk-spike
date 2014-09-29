package com.hoccer.talk.server;

import com.google.android.gcm.server.Message;
import com.hoccer.scm.GitInfo;
import com.hoccer.talk.server.push.ApnsConfiguration;
import com.hoccer.talk.server.push.PushAgent;
import org.apache.log4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.*;

/**
 * Encapsulation of server configuration
 * <p/>
 * This gets initialized with defaults and can then
 * be overloaded from a property file.
 */
public class TalkServerConfiguration {

    private static final Logger LOG = Logger.getLogger(TalkServerConfiguration.class);

    // Note: A ScheduledThreadPoolExecutor does not spawn more threads than defined as core pool size
    // see http://stackoverflow.com/questions/11678021/why-doesnt-scheduledexecutorservice-spawn-threads-as-needed

    public final static int GCM_WAKE_TTL = 1 * 7 * 24 * 3600; // 1 week

    // top level property prefix for all talk related properties, e.g. 'talk.foo.bar'
    private static final String PROPERTY_PREFIX = "talk";

    private enum PropertyTypes {STRING, BOOLEAN, INTEGER}

    private enum ConfigurableProperties {
        // WEB-SERVER
        LISTEN_ADDRESS(PROPERTY_PREFIX + ".listen.address",
                PropertyTypes.STRING,
                "localhost"),
        LISTEN_PORT(PROPERTY_PREFIX + ".listen.port",
                PropertyTypes.INTEGER,
                8080),

        // DATABASE
        DATABASE_BACKEND(PROPERTY_PREFIX + ".db.backend",
                PropertyTypes.STRING,
                "jongo"),
        JONGO_HOST(PROPERTY_PREFIX + ".jongo.host",
                PropertyTypes.STRING,
                "localhost"),
        JONGO_DATABASE(PROPERTY_PREFIX + ".jongo.db",
                PropertyTypes.STRING,
                "talk"),
        JONGO_CONNECTIONS_PER_HOST(PROPERTY_PREFIX + ".jongo.connectionsPerHost",
                PropertyTypes.INTEGER,
                10),
        JONGO_MAX_WAIT_TIME(PROPERTY_PREFIX + ".jongo.maxWaitTime",
                PropertyTypes.INTEGER,
                5 * 1000), // in milliseconds (5 seconds)

        // PUSH AGENT GENERIC
        PUSH_RATE_LIMIT(PROPERTY_PREFIX + ".push.rateLimit",
                PropertyTypes.INTEGER,
                15000),
        PUSH_THREAD_POOL_SIZE(PROPERTY_PREFIX + ".push.threadPoolSize",
                PropertyTypes.INTEGER,
                1), // ScheduledThreadPoolExecutor, number is also maximum Number of threads used

        // APNS
        APNS_ENABLED(PROPERTY_PREFIX + ".apns.enabled",
                PropertyTypes.BOOLEAN,
                false),
        APNS_INVALIDATE_DELAY(PROPERTY_PREFIX + ".apns.invalidate.delay",
                PropertyTypes.INTEGER,
                30), // in seconds
        APNS_INVALIDATE_INTERVAL(PROPERTY_PREFIX + ".apns.invalidate.interval",
                PropertyTypes.INTEGER,
                3600), // in seconds

        APNS_CLIENT_NAME_1(PROPERTY_PREFIX + ".apns.1.client.name",
                PropertyTypes.STRING,
                "Hoccer XO"),
        APNS_PRODUCTION_CERTIFICATE_PATH_1(PROPERTY_PREFIX + ".apns.1.cert.production.path",
                PropertyTypes.STRING,
                "apns_production.p12"),
        APNS_PRODUCTION_CERTIFICATE_PASSWORD_1(PROPERTY_PREFIX + ".apns.1.cert.production.password",
                PropertyTypes.STRING,
                "password"),
        APNS_SANDBOX_CERTIFICATE_PATH_1(PROPERTY_PREFIX + ".apns.1.cert.sandbox.path",
                PropertyTypes.STRING,
                "apns_sandbox.p12"),
        APNS_SANDBOX_CERTIFICATE_PASSWORD_1(PROPERTY_PREFIX + ".apns.1.cert.sandbox.password",
                PropertyTypes.STRING,
                "password"),

        APNS_CLIENT_NAME_2(PROPERTY_PREFIX + ".apns.2.client.name",
                PropertyTypes.STRING,
                "Hoccer"),
        APNS_PRODUCTION_CERTIFICATE_PATH_2(PROPERTY_PREFIX + ".apns.2.cert.production.path",
                PropertyTypes.STRING,
                "apns_production.p12"),
        APNS_PRODUCTION_CERTIFICATE_PASSWORD_2(PROPERTY_PREFIX + ".apns.2.cert.production.password",
                PropertyTypes.STRING,
                "password"),
        APNS_SANDBOX_CERTIFICATE_PATH_2(PROPERTY_PREFIX + ".apns.2.cert.sandbox.path",
                PropertyTypes.STRING,
                "apns_sandbox.p12"),
        APNS_SANDBOX_CERTIFICATE_PASSWORD_2(PROPERTY_PREFIX + ".apns.2.cert.sandbox.password",
                PropertyTypes.STRING,
                "password"),

        // GCM
        GCM_ENABLED(PROPERTY_PREFIX + ".gcm.enabled",
                PropertyTypes.BOOLEAN,
                false),
        GCM_API_KEY(PROPERTY_PREFIX + ".gcm.apikey",
                PropertyTypes.STRING,
                "AIzaSyA25wabV4kSQTaF73LTgTkjmw0yZ8inVr8"), // TODO: Do we really need this api key in code here?

        // CLEANING AGENT
        CLEANUP_ALL_CLIENTS_DELAY(PROPERTY_PREFIX + ".cleanup.allClientsDelay",
                PropertyTypes.INTEGER,
                7200), // in seconds (2 hours)
        CLEANUP_ALL_CLIENTS_INTERVAL(PROPERTY_PREFIX + ".cleanup.allClientsInterval",
                PropertyTypes.INTEGER,
                60 * 60 * 24), // in seconds (once a day)
        CLEANUP_ALL_DEVLIVERIES_DELAY(PROPERTY_PREFIX + ".cleanup.allDeliveriesDelay",
                PropertyTypes.INTEGER,
                3600), // in second (1 hour)
        CLEANUP_ALL_DELIVERIES_INTERVAL(PROPERTY_PREFIX + ".cleanup.allDeliveriesInterval",
                PropertyTypes.INTEGER,
                60 * 60 * 6), // in seconds (every 6 hours)
        CLEANUP_THREAD_POOL_SIZE(PROPERTY_PREFIX + ".cleanup.threadPoolSize",
                PropertyTypes.INTEGER,
                4), // ScheduledThreadPoolExecutor, number is also maximum Number of threads used

        // UPDATE AGENT
        UPDATE_THREAD_POOL_SIZE(PROPERTY_PREFIX + ".update.threadPoolSize",
                PropertyTypes.INTEGER,
                250), // ScheduledThreadPoolExecutor, number is also maximum Number of threads used

        // DELIVERY AGENT
        DELIVERY_THREAD_POOL_SIZE(PROPERTY_PREFIX + ".delivery.threadPoolSize",
                PropertyTypes.INTEGER,
                50), // ScheduledThreadPoolExecutor, number is also maximum Number of threads used

        // PING AGENT
        PING_THREAD_POOL_SIZE(PROPERTY_PREFIX + ".ping.threadPoolSize",
                PropertyTypes.INTEGER,
                10), // ScheduledThreadPoolExecutor, number is also maximum Number of threads used
        PING_PERFORM_AT_INTERVAL(PROPERTY_PREFIX + ".ping.performAtInterval",
                PropertyTypes.BOOLEAN,
                false),
        PING_INTERVAL(PROPERTY_PREFIX + ".ping.interval",
                PropertyTypes.INTEGER,
                300), // in seconds (every 5 minutes)

        // FILECACHE
        FILECACHE_CONTROL_URL(PROPERTY_PREFIX + ".filecache.controlUrl",
                PropertyTypes.STRING,
                "http://localhost:8081/control"),
        FILECACHE_UPLOAD_BASE(PROPERTY_PREFIX + ".filecache.uploadBase",
                PropertyTypes.STRING,
                "http://localhost:8081/upload/"),
        FILECACHE_DOWNLOAD_BASE(PROPERTY_PREFIX + ".filecache.downloadBase",
                PropertyTypes.STRING,
                "http://localhost:8081/download/"),

        // INVITATIONS
        INVITATION_ALLOWED_URI_SCHEMES(PROPERTY_PREFIX + ".invitation.allowed.uri.schemes",
                PropertyTypes.STRING,
                "hxo, hxod"),

        // MISC
        SUPPORT_TAG(PROPERTY_PREFIX + ".support.tag",
                PropertyTypes.STRING,
                "Oos8guceich2yoox"),
        LOG_ALL_CALLS(PROPERTY_PREFIX + ".debug.logallcalls",
                PropertyTypes.BOOLEAN,
                false);

        public final String key;
        public final PropertyTypes type;
        public Object value;

        private ConfigurableProperties(String name, PropertyTypes type, Object defaultValue) {
            this.key = name;
            this.type = type;
            this.value = defaultValue;
        }

        static public void loadFromProperties(Properties properties) {
            for (ConfigurableProperties property : ConfigurableProperties.values()) {
                LOG.debug("Loading configurable property " + property.name() + " from key: '" + property.key + "'");
                String rawValue = properties.getProperty(property.key);
                if (rawValue != null) {
                    property.setValue(rawValue);
                } else {
                    LOG.info("Property " + property.name() + " (type: " + property.type.name() + ") is unconfigured (key: '" + property.key + "'): default value is '" + property.value + "'");
                }
            }
        }

        public void setValue(String rawValue) {
            LOG.debug("   - setValue: " + rawValue + "  (" + this.type.name() + ")");
            if (PropertyTypes.STRING.equals(this.type)) {
                this.value = rawValue;
            } else if (PropertyTypes.INTEGER.equals(this.type)) {
                this.value = Integer.valueOf(rawValue);
            } else if (PropertyTypes.BOOLEAN.equals(this.type)) {
                this.value = Boolean.valueOf(rawValue);
            }
        }
    }

    static {
        verifyConfigurableProperties();
    }

    private static void verifyConfigurableProperties() {
        final Set<String> keys = new HashSet<String>();
        for (ConfigurableProperties configurableProperty : ConfigurableProperties.values()) {
            // check key uniqueness
            if (keys.contains(configurableProperty.key)) {
                throw new RuntimeException("Key: '" + configurableProperty.key + "' is doubly present. This is an error in the configuration setup! Keys have to be unique!");
            } else {
                keys.add(configurableProperty.key);
            }
        }
    }

    private String mVersion = "<unknown>";
    private GitInfo gitInfo = new GitInfo();
    private HashMap<String, ApnsConfiguration> mApnsConfigurations = new HashMap<String, ApnsConfiguration>();

    public TalkServerConfiguration() {
    }

    public void report() {
        StringBuilder builder = new StringBuilder();

        builder.append(                     "\n - General:");
        builder.append(MessageFormat.format("\n   * version:                              ''{0}''", mVersion));
        builder.append(MessageFormat.format("\n   * git.commit.id:                        ''{0}''", gitInfo.commitId));
        builder.append(MessageFormat.format("\n   * git.commit.id.abbrev:                 ''{0}''", gitInfo.commitIdAbbrev));
        builder.append(MessageFormat.format("\n   * git.branch:                           ''{0}''", gitInfo.branch));
        builder.append(MessageFormat.format("\n   * git.commit.time:                      ''{0}''", gitInfo.commitTime));
        builder.append(MessageFormat.format("\n   * git.build.time:                       ''{0}''", gitInfo.buildTime));
        builder.append(                     "\n - WebServer Configuration:");
        builder.append(MessageFormat.format("\n   * listen address:                       ''{0}''", this.getListenAddress()));
        builder.append(MessageFormat.format("\n   * listen port:                          {0}", Long.toString(getListenPort())));
        builder.append(                     "\n - Database Configuration:");
        builder.append(MessageFormat.format("\n   * database backend:                     ''{0}''", this.getDatabaseBackend()));
        builder.append(MessageFormat.format("\n   * jongo host:                           ''{0}''", this.getJongoHost()));
        builder.append(MessageFormat.format("\n   * jongo database:                       ''{0}''", this.getJongoDb()));
        builder.append(MessageFormat.format("\n   * jongo connections/host:               {0}", this.getJongoConnectionsPerHost()));
        builder.append(MessageFormat.format("\n   * jongo max wait time (in ms):          {0}", this.getJongoMaxWaitTime()));
        builder.append(                     "\n - Push Configuration:");
        builder.append(MessageFormat.format("\n   * push rate limit (in milli-seconds):   {0}", Long.toString(this.getPushRateLimit())));
        builder.append(                     "\n   - APNS:");
        builder.append(MessageFormat.format("\n     * enabled:                            {0}", this.isApnsEnabled()));
        builder.append(MessageFormat.format("\n     * apns invalidate delay (in s):       {0}", Long.toString(this.getApnsInvalidateDelay())));
        builder.append(MessageFormat.format("\n     * apns invalidate interval (in s):    {0}", Long.toString(this.getApnsInvalidateInterval())));
        builder.append(MessageFormat.format("\n     * default client name:                ''{0}''", this.getApnsDefaultClientName()));

        for (Map.Entry<String, ApnsConfiguration> entry : getApnsConfigurations().entrySet()) {
            String clientName = entry.getKey();
            ApnsConfiguration apnsConfiguration = entry.getValue();
            ApnsConfiguration.Certificate productionCertificate = apnsConfiguration.getCertificate(PushAgent.APNS_SERVICE_TYPE.PRODUCTION);
            ApnsConfiguration.Certificate sandboxCertificate = apnsConfiguration.getCertificate(PushAgent.APNS_SERVICE_TYPE.SANDBOX);

            builder.append(MessageFormat.format("\n     * configuration for client name:      ''{0}''", clientName));
            builder.append(MessageFormat.format("\n       * production cert path :            ''{0}''", productionCertificate.getPath()));
            builder.append(MessageFormat.format("\n       * production cert password (length):''{0}''", productionCertificate.getPassword().length())); // here we don't really print the password literal to stdout of course
            builder.append(MessageFormat.format("\n       * sandbox cert path :               ''{0}''", sandboxCertificate.getPath()));
            builder.append(MessageFormat.format("\n       * sandbox cert password (length):   ''{0}''", sandboxCertificate.getPassword().length())); // here we don't really print the password literal to stdout of course
        }

        builder.append(                     "\n   - GCM:");
        builder.append(MessageFormat.format("\n     * enabled:                            {0}", this.isGcmEnabled()));
        builder.append(MessageFormat.format("\n     * api key (length):                   ''{0}''", getGcmApiKey().length()));
        builder.append(MessageFormat.format("\n     * wake ttl (in s)                     ''{0}''", Long.toString(GCM_WAKE_TTL)));
        builder.append(                     "\n - Cleaning Agent Configuration:");
        builder.append(MessageFormat.format("\n   * clients cleanup delay (in s):         {0}", Long.toString(this.getApnsInvalidateDelay())));
        builder.append(MessageFormat.format("\n   * clients cleanup interval (in s):      {0}", Long.toString(this.getCleanupAllClientsInterval())));
        builder.append(MessageFormat.format("\n   * deliveries cleanup delay (in s):      {0}", Long.toString(this.getCleanupAllDeliveriesDelay())));
        builder.append(MessageFormat.format("\n   * deliveries cleanup interval (in s):   {0}", Long.toString(this.getCleanupAllDeliveriesInterval())));
        builder.append(                     "\n - Filecache Configuration:");
        builder.append(MessageFormat.format("\n   * filecache control url:                ''{0}''", this.getFilecacheControlUrl()));
        builder.append(MessageFormat.format("\n   * filecache upload base url:            ''{0}''", this.getFilecacheUploadBase()));
        builder.append(MessageFormat.format("\n   * filecache download base url:          ''{0}''", this.getFilecacheDownloadBase()));
        builder.append(                      "\n - Invitation Configuration:");
        builder.append(MessageFormat.format("\n   * allowed invitation URI schemes:     {0}", this.getAllowedInviteUriSchemes()));
        builder.append(                     "\n - Other:");
        builder.append(MessageFormat.format("\n   * support tag:                          ''{0}''", this.getSupportTag()));
        builder.append(                     "\n - Threads:");
        builder.append(MessageFormat.format("\n   * DeliveryAgent Threads Poolsize:       {0}", this.getDeliveryAgentThreadPoolSize()));
        builder.append(MessageFormat.format("\n   * CleanupAgent  Threads Poolsize:       {0}", this.getCleaningAgentThreadPoolSize()));
        builder.append(MessageFormat.format("\n   * PushAgent     Threads Poolsize:       {0}", this.getPushAgentThreadPoolSize()));
        builder.append(MessageFormat.format("\n   * PingAgent     Threads Poolsize:       {0}", this.getPingAgentThreadPoolSize()));
        builder.append(MessageFormat.format("\n   * UpdateAgent   Threads Poolsize:       {0}", this.getUpdateAgentThreadPoolSize()));
        builder.append(                     "\n - Ping:");
        builder.append(MessageFormat.format("\n   * Ping interval (in s):                 {0}", this.getPingInterval()));
        builder.append(MessageFormat.format("\n   * perform ping at intervals:            {0}", this.getPerformPingAtInterval()));
        builder.append(                     "\n - Debugging:");
        builder.append(MessageFormat.format("\n   * LogAllCalls:                          {0}", this.getLogAllCalls()));

        LOG.info("Current configuration:" + builder.toString());
    }

    public void configureFromProperties(Properties properties) {
        LOG.info("Loading from properties...");
        ConfigurableProperties.loadFromProperties(properties);
        initApnsConfigurations();
    }

    private void initApnsConfigurations() {
        mApnsConfigurations.put((String) ConfigurableProperties.APNS_CLIENT_NAME_1.value, new ApnsConfiguration(
                (String) ConfigurableProperties.APNS_PRODUCTION_CERTIFICATE_PATH_1.value,
                (String) ConfigurableProperties.APNS_PRODUCTION_CERTIFICATE_PASSWORD_1.value,
                (String) ConfigurableProperties.APNS_SANDBOX_CERTIFICATE_PATH_1.value,
                (String) ConfigurableProperties.APNS_SANDBOX_CERTIFICATE_PASSWORD_1.value));

        mApnsConfigurations.put((String) ConfigurableProperties.APNS_CLIENT_NAME_2.value, new ApnsConfiguration(
                (String) ConfigurableProperties.APNS_PRODUCTION_CERTIFICATE_PATH_2.value,
                (String) ConfigurableProperties.APNS_PRODUCTION_CERTIFICATE_PASSWORD_2.value,
                (String) ConfigurableProperties.APNS_SANDBOX_CERTIFICATE_PATH_2.value,
                (String) ConfigurableProperties.APNS_SANDBOX_CERTIFICATE_PASSWORD_2.value));
    }

    public String getListenAddress() {
        return (String) ConfigurableProperties.LISTEN_ADDRESS.value;
    }

    public int getListenPort() {
        return (Integer) ConfigurableProperties.LISTEN_PORT.value;
    }

    public String getDatabaseBackend() {
        return (String) ConfigurableProperties.DATABASE_BACKEND.value;
    }

    public String getJongoDb() {
        return (String) ConfigurableProperties.JONGO_DATABASE.value;
    }

    public int getJongoConnectionsPerHost() {
        return (Integer) ConfigurableProperties.JONGO_CONNECTIONS_PER_HOST.value;
    }

    public int getJongoMaxWaitTime() {
        return (Integer) ConfigurableProperties.JONGO_MAX_WAIT_TIME.value;
    }

    public String getJongoHost() {
        return (String) ConfigurableProperties.JONGO_HOST.value;
    }

    public int getPushRateLimit() {
        return (Integer) ConfigurableProperties.PUSH_RATE_LIMIT.value;
    }

    public boolean isGcmEnabled() {
        return (Boolean) ConfigurableProperties.GCM_ENABLED.value;
    }

    public String getGcmApiKey() {
        return (String) ConfigurableProperties.GCM_API_KEY.value;
    }

    public boolean isApnsEnabled() {
        return (Boolean) ConfigurableProperties.APNS_ENABLED.value;
    }

    public HashMap<String, ApnsConfiguration> getApnsConfigurations() {
        return mApnsConfigurations;
    }

    public ApnsConfiguration getApnsConfiguration(String clientName) {
        return mApnsConfigurations.get(clientName);
    }

    public String getApnsDefaultClientName() {
        return (String) ConfigurableProperties.APNS_CLIENT_NAME_1.value;
    }

    public int getApnsInvalidateDelay() {
        return (Integer) ConfigurableProperties.APNS_INVALIDATE_DELAY.value;
    }

    public int getApnsInvalidateInterval() {
        return (Integer) ConfigurableProperties.APNS_INVALIDATE_INTERVAL.value;
    }

    public int getCleanupAllClientsDelay() {
        return (Integer) ConfigurableProperties.CLEANUP_ALL_CLIENTS_DELAY.value;
    }

    public int getCleanupAllClientsInterval() {
        return (Integer) ConfigurableProperties.CLEANUP_ALL_CLIENTS_INTERVAL.value;
    }

    public int getCleanupAllDeliveriesDelay() {
        return (Integer) ConfigurableProperties.CLEANUP_ALL_DEVLIVERIES_DELAY.value;
    }

    public int getCleanupAllDeliveriesInterval() {
        return (Integer) ConfigurableProperties.CLEANUP_ALL_CLIENTS_INTERVAL.value;
    }

    public URI getFilecacheControlUrl() {
        URI url = null;
        try {
            url = new URI((String) ConfigurableProperties.FILECACHE_CONTROL_URL.value);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return url;
    }

    public void setFilecacheControlUrl(String pFilecacheControlUrl) {
        ConfigurableProperties.FILECACHE_CONTROL_URL.setValue(pFilecacheControlUrl);
    }

    public String getFilecacheUploadBase() {
        return (String) ConfigurableProperties.FILECACHE_UPLOAD_BASE.value;
    }

    public void setFilecacheUploadBase(String pFilecacheUploadBase) {
        ConfigurableProperties.FILECACHE_UPLOAD_BASE.setValue(pFilecacheUploadBase);
    }

    public String getFilecacheDownloadBase() {
        return (String) ConfigurableProperties.FILECACHE_DOWNLOAD_BASE.value;
    }

    public void setFilecacheDownloadBase(String mFilecacheDownloadBase) {
        ConfigurableProperties.FILECACHE_DOWNLOAD_BASE.setValue(mFilecacheDownloadBase);
    }

    public List<String> getAllowedInviteUriSchemes() {
        String schemesString = (String) ConfigurableProperties.INVITATION_ALLOWED_URI_SCHEMES.value;
        return Arrays.asList(schemesString.replace(" ", "").split(","));
    }

    public String getSupportTag() {
        return (String) ConfigurableProperties.SUPPORT_TAG.value;
    }

    public boolean getLogAllCalls() {
        return (Boolean) ConfigurableProperties.LOG_ALL_CALLS.value;
    }

    public void setLogAllCalls(Boolean flag) {
        ConfigurableProperties.LOG_ALL_CALLS.setValue(flag.toString());
    }

    public String getVersion() {
        return mVersion;
    }

    public void setVersion(String version) {
        this.mVersion = version;
    }

    public void setGitInfo(GitInfo gitInfo) {
        this.gitInfo = gitInfo;
    }

    public GitInfo getGitInfo() {
        return gitInfo;
    }

    public int getCleaningAgentThreadPoolSize() {
        return (Integer) ConfigurableProperties.CLEANUP_THREAD_POOL_SIZE.value;
    }

    public int getUpdateAgentThreadPoolSize() {
        return (Integer) ConfigurableProperties.UPDATE_THREAD_POOL_SIZE.value;
    }

    public int getDeliveryAgentThreadPoolSize() {
        return (Integer) ConfigurableProperties.DELIVERY_THREAD_POOL_SIZE.value;
    }

    public int getPingAgentThreadPoolSize() {
        return (Integer) ConfigurableProperties.PING_THREAD_POOL_SIZE.value;
    }

    public int getPushAgentThreadPoolSize() {
        return (Integer) ConfigurableProperties.PUSH_THREAD_POOL_SIZE.value;
    }

    public boolean getPerformPingAtInterval() {
        return (Boolean) ConfigurableProperties.PING_PERFORM_AT_INTERVAL.value;
    }

    public int getPingInterval() {
        return (Integer) ConfigurableProperties.PING_INTERVAL.value;
    }
}
