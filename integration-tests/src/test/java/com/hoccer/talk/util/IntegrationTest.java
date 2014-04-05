package com.hoccer.talk.util;

import com.hoccer.talk.client.XoClient;
import com.hoccer.talk.filecache.CacheConfiguration;
import com.hoccer.talk.server.TalkServerConfiguration;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.tests.MongodForTestsFactory;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


public class IntegrationTest {

    private static MongodForTestsFactory factory;
    private Path tempDir;

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

    @Before
    public void createTempFolder() throws IOException {
        tempDir = Files.createTempDirectory("IT-");
    }

    @After
    public void deleteTempFolder() throws IOException {
        FileUtils.deleteDirectory(tempDir.toFile());
    }

    public TestTalkServer createTalkServer() throws Exception {
        return createTalkServer(null);
    }

    public TestTalkServer createTalkServer(TestFileCache fc) throws Exception {
        TalkServerConfiguration configuration = new TalkServerConfiguration();
        if (fc != null) {
            int port = fc.getServerConnector().getPort();
            configuration.setFilecacheControlUrl("ws://localhost:"+port+"/control");
            configuration.setFilecacheDownloadBase("http://localhost:" + port + "/download/");
            configuration.setFilecacheUploadBase("http://localhost:" + port + "/upload/");
        }
        return new TestTalkServer(configuration, factory);
    }

    public TestFileCache createFileCache() throws Exception {
        CacheConfiguration configuration = new CacheConfiguration();
        configuration.setOrmliteUrl("jdbc:h2:mem");
        configuration.setOrmliteInitDb(true);
        configuration.setDataDirectory(tempDir.toString());
        return new TestFileCache(configuration);
    }

    public XoClient createTalkClient(TestTalkServer server) throws Exception {
        return new XoClient(new TestClientHost(server));
    }
}
