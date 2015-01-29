package com.hoccer.talk.server;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.servlets.HealthCheckServlet;
import com.codahale.metrics.servlets.MetricsServlet;
import com.hoccer.talk.server.rpc.TalkRpcConnectionHandler;
import com.hoccer.talk.servlets.CertificateInfoServlet;
import com.hoccer.talk.servlets.InvitationServlet;
import com.hoccer.talk.servlets.ServerInfoServlet;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;

import java.io.IOException;
import java.util.List;

public class TalkServerHandler extends HandlerCollection {

    public TalkServerHandler(TalkServer talkServer) {
        addHandler(createMetricsHandler(talkServer));
        addHandler(createServerStatusHandler(talkServer));
        addHandler(createInvitationHandler());
        addHandler(createStaticResourceHandler());
        addHandler(new TalkRpcConnectionHandler(talkServer));
    }

    private ServletContextHandler createMetricsHandler(TalkServer talkServer) {
        ServletContextHandler metricsContextHandler = new ServletContextHandler();

        metricsContextHandler.setContextPath("/metrics");
        metricsContextHandler.setInitParameter("show-jvm-metrics", "true");

        metricsContextHandler.addEventListener(new MyMetricsServletContextListener(talkServer.getMetrics()));
        metricsContextHandler.addServlet(MetricsServlet.class, "/registry");

        metricsContextHandler.addEventListener(new MyHealthCheckServletContextListener(talkServer.getHealthCheckRegistry()));
        metricsContextHandler.addServlet(HealthCheckServlet.class, "/health");

        return metricsContextHandler;
    }

    private ServletContextHandler createServerStatusHandler(TalkServer talkServer) {
        ServletContextHandler serverInfoContextHandler = new ServletContextHandler();

        serverInfoContextHandler.setContextPath("/server");
        serverInfoContextHandler.setAttribute("server", talkServer);
        serverInfoContextHandler.addServlet(ServerInfoServlet.class, "/info");
        serverInfoContextHandler.addServlet(CertificateInfoServlet.class, "/certificates");

        return serverInfoContextHandler;
    }

    private ServletContextHandler createInvitationHandler() {
        ServletContextHandler invitationContextHandler = new ServletContextHandler();

        invitationContextHandler.setContextPath("/invite");
        invitationContextHandler.addServlet(InvitationServlet.class, "/*");

        return invitationContextHandler;
    }

    private HandlerCollection createStaticResourceHandler() {
        HandlerCollection handlerCollection = new HandlerCollection();

        try {
            List<String> directories = IOUtils.readLines(getClass().getResourceAsStream("/invite/"), Charsets.UTF_8);

            for (String directory : directories) {
                handlerCollection.addHandler(createStaticResourceHandler(directory));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load static resource paths", e);
        }

        return handlerCollection;
    }

    private ContextHandler createStaticResourceHandler(String directory) {
        ContextHandler staticHandler = new ContextHandler("/static/" + directory);

        ResourceHandler staticResourceHandler = new ResourceHandler();
        String resourceBase = getClass().getResource("/invite/" + directory + "/static").toExternalForm();
        staticResourceHandler.setResourceBase(resourceBase);
        staticHandler.setHandler(staticResourceHandler);

        return staticHandler;
    }

    private static class MyMetricsServletContextListener extends MetricsServlet.ContextListener {
        private final MetricRegistry _metricRegistry;

        public MyMetricsServletContextListener(MetricRegistry metricRegistry) {
            _metricRegistry = metricRegistry;
        }

        @Override
        protected MetricRegistry getMetricRegistry() {
            return _metricRegistry;
        }
    }

    private static class MyHealthCheckServletContextListener extends HealthCheckServlet.ContextListener {
        private final HealthCheckRegistry _healthCheckRegistry;

        public MyHealthCheckServletContextListener(HealthCheckRegistry healthCheckRegistry) {
            _healthCheckRegistry = healthCheckRegistry;
        }

        @Override
        protected HealthCheckRegistry getHealthCheckRegistry() {
            return _healthCheckRegistry;
        }
    }
}
