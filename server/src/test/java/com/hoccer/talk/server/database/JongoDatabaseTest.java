package com.hoccer.talk.server.database;

import com.hoccer.talk.model.TalkClient;
import com.hoccer.talk.model.TalkMessage;
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
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class JongoDatabaseTest {

    private static final Logger LOG = Logger.getLogger(JongoDatabaseTest.class);
    private static MongodStarter mongodStarter;
    private static IMongodConfig mongodConfig;

    private JongoDatabase database;
    private MongodProcess mongod;
    private MongodExecutable mongodExecutable;

    // TODO: provide a convenient way to load fixtures into the database as part of a test.
    // Alternatively we have to setup the state of the database via code, which may be faulty in itself? depends on diligence.

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
        LOG.debug(mongodConfig.net().getServerAddress() + " " + mongodConfig.net().getPort());
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

    @Test
    public void testLoadAndSaveClient() throws Exception {
        final TalkClient transientClient = new TalkClient();
        transientClient.setClientId("foo");

        database.saveClient(transientClient);

        List<TalkClient> clients;

        clients = database.findAllClients();
        assertEquals(1, clients.size());
        TalkClient persistedClient = clients.get(0);
        assertEquals("foo", persistedClient.getClientId());

        final TalkClient anotherTransientClient = new TalkClient();
        transientClient.setClientId("bar");

        database.saveClient(anotherTransientClient);
        clients = database.findAllClients();
        assertEquals(2, clients.size());
    }

    @Test
    public void testFindClientById() throws Exception {
        assertNull(database.findClientById("doesnotexist"));

        final TalkClient transientClient1 = new TalkClient();
        transientClient1.setClientId("foo");
        transientClient1.setApnsToken("1");
        database.saveClient(transientClient1);

        final TalkClient transientClient2 = new TalkClient();
        transientClient2.setClientId("foo");
        transientClient1.setApnsToken("2");
        // TODO: This is actually something that should not be possible
        database.saveClient(transientClient2);

        TalkClient client = database.findClientById(transientClient1.getClientId());
        assertNotNull(client);
        assertEquals("foo", client.getClientId());
        // We expect to get only the first entity...
        assertEquals("1", client.getApnsToken());

        // ...although two are in the database
        List<TalkClient> clients = database.findAllClients();
        assertEquals(2, clients.size());
    }

    @Test
    public void testFindClientByApnsToken() {
        assertNull(database.findClientByApnsToken("doesnotexist"));

        final TalkClient transientClient1 = new TalkClient();
        transientClient1.setClientId("1");
        transientClient1.setApnsToken("foo");
        database.saveClient(transientClient1);

        final TalkClient transientClient2 = new TalkClient();
        transientClient2.setClientId("2");
        transientClient2.setApnsToken("foo");
        database.saveClient(transientClient2);

        TalkClient client = database.findClientByApnsToken(transientClient1.getApnsToken());
        assertNotNull(client);
        assertEquals("foo", client.getApnsToken());
        // We expect to get only the first entity...
        assertEquals("1", client.getClientId());

        // ...although two are in the database
        List<TalkClient> clients = database.findAllClients();
        assertEquals(2, clients.size());
    }

    @Test
    public void testFindMessageById() {
        assertNull(database.findMessageById("doesnotexist"));

        final TalkMessage transientMessage1 = new TalkMessage();
        transientMessage1.setMessageId("foo");
        transientMessage1.setMessageTag("1");
        database.saveMessage(transientMessage1);

        final TalkMessage transientMessage2 = new TalkMessage();
        transientMessage2.setMessageId("foo");
        transientMessage2.setMessageTag("2");
        database.saveMessage(transientMessage2);

        TalkMessage message = database.findMessageById("foo");
        assertNotNull(message);
        assertEquals("foo", message.getMessageId());
        // We expect to get only the first entity...
        assertEquals("1", message.getMessageTag());

        // TODO: also test if there are actually 2 messages in db? No API for that currently
    }
}
