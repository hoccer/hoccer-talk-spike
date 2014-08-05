package com.hoccer.talk.server.database;

import com.hoccer.talk.server.TalkServerConfiguration;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;
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
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class JongoDatabaseTest {

    private static final Logger LOG = Logger.getLogger(JongoDatabaseTest.class);
    private static MongodStarter mongodStarter;
    private static IMongodConfig mongodConfig;

    private JongoDatabase database;
    private MongodProcess mongod;
    private MongodExecutable mongodExecutable;

    static {
        configureLogging();
    }

    private static void configureLogging() {
        org.apache.log4j.Logger.getLogger(JongoDatabase.class).setLevel(Level.DEBUG);
    }


    @BeforeClass
    public static void setupClass() throws IOException {
        IRuntimeConfig runtimeConfig = new RuntimeConfigBuilder()
                .defaults(Command.MongoD)
                .processOutput(ProcessOutput.getDefaultInstanceSilent())
                .build();
        mongodStarter = MongodStarter.getInstance(runtimeConfig);
        mongodConfig = new MongodConfigBuilder()
                .version(Version.Main.PRODUCTION)
                .build();
    }

    @AfterClass
    public static void teardownClass() {
    }

    @Before
    public void setUp() throws Exception {
        mongodExecutable = mongodStarter.prepare(mongodConfig);
        mongod = mongodExecutable.start();

        TalkServerConfiguration config = new TalkServerConfiguration();
        LOG.info("----" + mongodConfig.net().getServerAddress() + " " + mongodConfig.net().getPort());
        MongoClient mongo = new MongoClient(new ServerAddress(mongodConfig.net().getServerAddress(), mongodConfig.net().getPort()));
        database = new JongoDatabase(config, mongo);
    }

    @After
    public void tearDown() throws Exception {
        mongod.stop();
        mongodExecutable.stop();
    }

    @Test
    public void testPingDbOnline() throws Exception {
        assertTrue(database.ping());
    }

    @Test(expected = MongoException.class)
    public void testPingDbOffline() throws Exception {
        mongod.stop();
        mongodExecutable.stop();
        database.ping();
    }

}
