package com.hoccer.talk.server.database;

import com.hoccer.talk.model.TalkClient;
import com.hoccer.talk.model.TalkDelivery;
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

    /*
    * TalkClient related methods
    * */
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
    public void testFindClientByApnsToken() throws Exception {
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

    /*
    * TalkMessage related methods
    * */
    @Test
    public void testFindMessageById() throws Exception {
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

    @Test
    public void testFindMessagesWithAttachmentFileId() throws Exception {
        List<TalkMessage> emptyMessageList = database.findMessagesWithAttachmentFileId("doesnotexist");
        assertNotNull(emptyMessageList);
        assertEquals(0, emptyMessageList.size());

        final TalkMessage transientMessage1 = new TalkMessage();
        transientMessage1.setMessageId("first");
        transientMessage1.setAttachmentFileId("fileId1");
        database.saveMessage(transientMessage1);

        final TalkMessage transientMessage2 = new TalkMessage();
        transientMessage1.setMessageId("second");
        transientMessage1.setAttachmentFileId("fileId2");
        database.saveMessage(transientMessage2);

        final List<TalkMessage> MessageListContainingOne = database.findMessagesWithAttachmentFileId("fileId1");
        assertNotNull(MessageListContainingOne);
        assertEquals(1, MessageListContainingOne.size());

        transientMessage2.setAttachmentFileId("fileId1");
        database.saveMessage(transientMessage2);

        final List<TalkMessage> messageListContainingTwo = database.findMessagesWithAttachmentFileId("fileId1");
        assertNotNull(messageListContainingTwo);
        assertEquals(2, messageListContainingTwo.size());

        emptyMessageList = database.findMessagesWithAttachmentFileId("doesnotexist");
        assertNotNull(emptyMessageList);
        assertEquals(0, emptyMessageList.size());
    }

    @Test
    public void testDeleteMessage() throws Exception {
        TalkMessage transientMessage = new TalkMessage();
        transientMessage.setMessageId("foo");
        database.saveMessage(transientMessage);

        final TalkMessage persistedMessage = database.findMessageById("foo");
        assertNotNull(persistedMessage);
        database.deleteMessage(persistedMessage);
        assertNull(database.findMessageById("foo"));

        LOG.info(" + " + database.findMessageById(null));
    }

    // just pointing out a small hole atm - easily fixed as soon as we agree on the approach...
    @Test(expected = NullPointerException.class)
    public void testDeleteNullMessage() throws Exception {
        database.deleteMessage(null);
    }



    /*
    * TalkDelivery related methods
    * */
    @Test
    public void testFindDelivery() throws Exception {
        assertNull(database.findDelivery("unknown_message_id", "unknown_recipient_id"));

        TalkDelivery transientDelivery1 = new TalkDelivery();
        transientDelivery1.setMessageId("foo");
        transientDelivery1.setReceiverId("bar");
        database.saveDelivery(transientDelivery1);

        TalkDelivery transientDelivery2 = new TalkDelivery();
        transientDelivery2.setMessageId("something_else");
        transientDelivery2.setReceiverId("bar");
        database.saveDelivery(transientDelivery2);

        TalkDelivery transientDelivery3 = new TalkDelivery();
        transientDelivery3.setMessageId("foo");
        transientDelivery3.setReceiverId("something_else");
        database.saveDelivery(transientDelivery3);

        final TalkDelivery persistedDelivery = database.findDelivery("foo", "bar");
        assertNotNull(persistedDelivery);
        assertEquals("foo", persistedDelivery.getMessageId());
        assertEquals("bar", persistedDelivery.getReceiverId());

        assertNull(database.findDelivery("foo", "unknown_recipient_id"));
        assertNull(database.findDelivery("unknown_message_id", "bar"));

        assertNotNull(database.findDelivery("something_else", "bar"));
        assertNotNull(database.findDelivery("foo", "something_else"));
    }

    private void createDeliveryInState(String pState) {
        TalkDelivery transientDelivery = new TalkDelivery(true);
        transientDelivery.setState(pState);
        database.saveDelivery(transientDelivery);
    }

    @Test
    public void testFindDeliveriesInState() throws Exception {
        final List<TalkDelivery> emptyResult = database.findDeliveriesInState("crocodile_hunting");
        assertNotNull(emptyResult);
        assertEquals(0, emptyResult.size());

        createDeliveryInState(TalkDelivery.STATE_DELIVERED_SEEN);
        createDeliveryInState(TalkDelivery.STATE_DELIVERED_SEEN);
        createDeliveryInState(TalkDelivery.STATE_ABORTED);
        createDeliveryInState(TalkDelivery.STATE_DELIVERED_SEEN_ACKNOWLEDGED);

        assertEquals(2, database.findDeliveriesInState(TalkDelivery.STATE_DELIVERED_SEEN).size());
        assertEquals(1, database.findDeliveriesInState(TalkDelivery.STATE_ABORTED).size());
        assertEquals(1, database.findDeliveriesInState(TalkDelivery.STATE_DELIVERED_SEEN_ACKNOWLEDGED).size());
    }


}
