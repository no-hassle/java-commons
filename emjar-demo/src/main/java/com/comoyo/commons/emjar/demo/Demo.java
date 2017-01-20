package com.comoyo.commons.emjar.demo;

import com.comoyo.commons.emjar.demo.rs.Endpoint;
import java.net.URI;
import java.util.logging.Logger;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

public class Demo {
    public static void main(final String[] args) throws Exception {
        final Logger log = Logger.getLogger(Demo.class.getName());
        final ResourceConfig app = new ResourceConfig();
        if (System.getProperty("emjar.demo.classpath-scanning") != null) {
            log.info("=== Running test (classpath scanning)");
            app.packages("com.comoyo.commons.emjar.demo.rs");
        } else {
            log.info("=== Running test (explicit registration)");
            app.register(Endpoint.class);
        }

        final ServletHolder servlet = new ServletHolder(new ServletContainer(app));
        final Server server = new Server(0);
        final ServletContextHandler context = new ServletContextHandler(server, "/");
        context.addServlet(servlet, "/*");
        server.start();
        final URI uri = server.getURI();
        final Client client = ClientBuilder.newClient();
        try {
            log.info("Html: "
                + client.target(uri).request(MediaType.TEXT_HTML_TYPE).get(String.class));
            log.info("Json: "
                + client.target(uri).request(MediaType.APPLICATION_JSON_TYPE).get(String.class));
            log.info("Xml: "
                + client.target(uri).request(MediaType.APPLICATION_XML_TYPE).get(String.class));
        } catch (final RuntimeException e) {
            e.printStackTrace(System.err);
        }
        server.stop();
    }
}
