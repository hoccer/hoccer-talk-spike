package com.hoccer.talk.util;

import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.filecache.CacheConfiguration;
import com.hoccer.talk.server.TalkServerConfiguration;
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
import de.flapdoodle.embed.mongo.runtime.Mongod;
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
    private static Logger mongoLogger = Logger.getLogger(Mongod.class);

    private MongodExecutable mongodExecutable = null;
    private MongodProcess mongod = null;
    private Path tempDir;


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
        // set up connection to fileCache
        if (fc != null) {
            int port = fc.getServerConnector().getPort();
            configuration.setFilecacheControlUrl("ws://localhost:"+port+"/control");
            configuration.setFilecacheDownloadBase("http://localhost:" + port + "/download/");
            configuration.setFilecacheUploadBase("http://localhost:" + port + "/upload/");
        }
        // set up mongo client
        MongoClient mongo = new MongoClient(new ServerAddress(mongodConfig.net().getServerAddress(), mongodConfig.net().getPort()));
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
}
