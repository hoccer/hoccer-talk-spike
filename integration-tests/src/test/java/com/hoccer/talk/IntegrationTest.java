package com.hoccer.talk;

import com.hoccer.talk.client.XoClient;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.tests.MongodForTestsFactory;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.IOException;


public class IntegrationTest {

    private static MongodForTestsFactory factory;


    @BeforeClass
    public static void setUpClass() throws IOException {
        factory = MongodForTestsFactory.with(Version.Main.PRODUCTION);
    }

    @AfterClass
    public static void tearDownClass() {
        if (factory != null) {
            factory.shutdown();
        }

    }

    public TestServer createTalkServer() throws Exception {
        return new TestServer(factory);
    }

    public XoClient createTalkClient(TestServer server) throws Exception {
        return new XoClient(new TestClientHost(server));
    }
}
