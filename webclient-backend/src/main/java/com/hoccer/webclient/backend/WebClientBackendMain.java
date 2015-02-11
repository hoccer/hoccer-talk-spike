package com.hoccer.webclient.backend;

import com.beust.jcommander.JCommander;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.webclient.backend.factories.XoClientDatabaseFactory;
import com.hoccer.webclient.backend.servlet.DownloadsResource;
import com.hoccer.webclient.backend.servlet.WebSocketConnectionServlet;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

public class WebClientBackendMain {

    public static void main(String[] args) throws Exception {
        CommandLineParameters params = new CommandLineParameters();
        new JCommander(params, args);

        final Configuration configuration = new Configuration(params.getConfigFile());
        configuration.report();

        final WebClientBackend backend = new WebClientBackend(configuration);

        // Configure REST API handler
        ServletContextHandler apiHandler = new ServletContextHandler();
        ResourceConfig resourceConfig = new ResourceConfig(DownloadsResource.class);
        resourceConfig.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bindFactory(new XoClientDatabaseFactory(backend.getXoClient())).to(XoClientDatabase.class);
            }
        });

        FilterHolder crossOriginFilter = new FilterHolder(CrossOriginFilter.class);
        crossOriginFilter.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,POST,PUT,PATCH,HEAD,OPTIONS");
        apiHandler.addFilter(crossOriginFilter, "/*", null);
        apiHandler.addServlet(new ServletHolder(new ServletContainer(resourceConfig)), "/api/*");

        // Configure WebSocket handler
        ServletContextHandler webSocketHandler = new ServletContextHandler();
        XoClientDatabase database = backend.getXoClient().getDatabase();
        webSocketHandler.addServlet(new ServletHolder(new WebSocketConnectionServlet(database)), "/updates/*");

        // Configure static file download handler
        ContextHandler staticHandler = new ContextHandler("/" + configuration.getDecAttachmentDir());
        ResourceHandler staticResourceHandler = new ResourceHandler();
        staticResourceHandler.setResourceBase(configuration.getDecAttachmentDir());
        staticHandler.setHandler(staticResourceHandler);

        // Combine both handlers
        ContextHandlerCollection handlerCollection = new ContextHandlerCollection();
        handlerCollection.setHandlers(new Handler[]{apiHandler, webSocketHandler, staticHandler});


        // Start embedded server
        Server server = new Server(configuration.getBackendPort());
        server.setHandler(handlerCollection);
        server.start();
        server.join();
    }

}
