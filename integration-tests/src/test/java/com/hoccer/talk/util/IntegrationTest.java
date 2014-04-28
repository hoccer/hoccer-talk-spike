package com.hoccer.talk.util;

import better.jsonrpc.client.JsonRpcClient;
import better.jsonrpc.core.JsonRpcConnection;
import better.jsonrpc.server.JsonRpcServer;
import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.filecache.CacheConfiguration;
import com.hoccer.talk.server.TalkServerConfiguration;
import com.hoccer.talk.server.database.JongoDatabase;
import com.hoccer.talk.server.rpc.TalkRpcHandler;
import com.j256.ormlite.logger.LoggerFactory;
import com.j256.ormlite.table.TableUtils;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.config.io.ProcessOutput;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Security;

public class IntegrationTest {
    private static MongodStarter mongodStarter = null;
    private static IMongodConfig mongodConfig = null;

    private MongodExecutable mongodExecutable = null;
    private MongodProcess mongod = null;
    private Path tempDir;


    static {
        configureLogging();
    }

    @BeforeClass
    public static void setUpClass() throws IOException {
        if (mongodStarter == null) {
            ProcessOutput processOutput = new ProcessOutput(
                    new MongoStreamLogger("mongod", Level.DEBUG),
                    new MongoStreamLogger("mongod", Level.ERROR),
                    new MongoStreamLogger("mongoc", Level.INFO));
            IRuntimeConfig runtimeConfig = new RuntimeConfigBuilder()
                    .defaults(Command.MongoD)
                    .processOutput(processOutput)
                    .build();
            mongodStarter = MongodStarter.getInstance(runtimeConfig);
        }
        if (mongodConfig == null) {
            mongodConfig = new MongodConfigBuilder()
                    .version(Version.Main.PRODUCTION)
                    .build();
        }
    }

    @AfterClass
    public static void tearDownClass() {

    }

    @Before
    public void createTempFolder() throws IOException {
        mongodExecutable = mongodStarter.prepare(mongodConfig);
        mongod = mongodExecutable.start();
        tempDir = Files.createTempDirectory("IT-");
    }

    @After
    public void deleteTempFolder() throws IOException {
        if (mongod != null) {
            mongod.stop();
        }
        if (mongodExecutable != null) {
            mongodExecutable.stop();
        }
        FileUtils.deleteDirectory(tempDir.toFile());
    }

    public TestTalkServer createTalkServer() throws Exception {
        return createTalkServer(null);
    }

    public TestTalkServer createTalkServer(TestFileCache fc) throws Exception {
        TalkServerConfiguration configuration = new TalkServerConfiguration();
        configuration.setLogAllCalls(true);

        // set up connection to fileCache
        if (fc != null) {
            int port = fc.getServerConnector().getPort();
            configuration.setFilecacheControlUrl("ws://localhost:" + port + "/control");
            configuration.setFilecacheDownloadBase("http://localhost:" + port + "/download/");
            configuration.setFilecacheUploadBase("http://localhost:" + port + "/upload/");
        }
        // set up mongo client
        MongoClient mongo = new MongoClient(new ServerAddress(mongodConfig.net().getServerAddress(), mongodConfig.net().getPort()));

        configuration.report();

        return new TestTalkServer(configuration, mongo);
    }

    public TestFileCache createFileCache() throws Exception {
        CacheConfiguration configuration = new CacheConfiguration();
        configuration.setOrmliteUrl("jdbc:h2:mem:");
        configuration.setOrmliteInitDb(true);
        configuration.setDataDirectory(tempDir.toString());
        return new TestFileCache(configuration);
    }

    public XoClient createTalkClient(TestTalkServer server) throws Exception {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        return new XoClient(new TestClientHost(server));
    }

    private static void configureLogging() {
        // This sets everything to DEBUG - use only when necessary
        //RootLogger.getRootLogger().setLevel(Level.DEBUG);

        // For TalkClient
        Logger.getLogger(XoClient.class).setLevel(Level.DEBUG);
        System.setProperty(LoggerFactory.LOG_TYPE_SYSTEM_PROPERTY, LoggerFactory.LogType.LOG4J.name());
        Logger.getLogger(TableUtils.class).setLevel(Level.DEBUG);

        // For Server
        // Setting the Loglevel of specific logger explicitely
        //org.apache.log4j.Logger.getLogger(UpdateAgent.class).setLevel(Level.DEBUG);
        org.apache.log4j.Logger.getLogger(JongoDatabase.class).setLevel(Level.DEBUG);
        org.apache.log4j.Logger.getLogger(TalkRpcHandler.class).setLevel(Level.DEBUG);

        // For both
        org.apache.log4j.Logger.getLogger(JsonRpcServer.class).setLevel(Level.DEBUG);
        org.apache.log4j.Logger.getLogger(JsonRpcClient.class).setLevel(Level.DEBUG);
        org.apache.log4j.Logger.getLogger(JsonRpcConnection.class).setLevel(Level.DEBUG);
    }
}
