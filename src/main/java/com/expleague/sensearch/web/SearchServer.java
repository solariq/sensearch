package com.expleague.sensearch.web;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;

public class SearchServer {


  public static void main(String[] args) throws Exception {
    Builder.build();

    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");

    Server server = new Server(8081);
    server.setHandler(context);

    ServletHolder jerseyServlet = context.addServlet(ServletContainer.class, "/*");
    jerseyServlet.setInitOrder(0);

    jerseyServlet.setInitParameter(
        "jersey.config.server.provider.classnames", SearchEndpoint.class.getCanonicalName());

    server.start();
    server.join();
  }
}
