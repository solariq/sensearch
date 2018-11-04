package com.expleague.sensearch.web;

import com.expleague.sensearch.AppModule;
import com.expleague.sensearch.Config;
import java.nio.file.Paths;
import java.util.EnumSet;
import javax.servlet.DispatcherType;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.util.resource.Resource;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

public class SearchServer {


  public static void main(String[] args) throws Exception {
    Injector injector = Guice.createInjector(new AppModule());
    Builder builder = injector.getInstance(Builder.class);
    Config config = builder.build();

    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/api");

    // Set up cors to be able to make request from frontend
    FilterHolder cors = context.addFilter(CrossOriginFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
    cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
    cors.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
    cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,POST,HEAD");
    cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "X-Requested-With,Content-Type,Accept,Origin");

    ResourceConfig resourceConfig = new ResourceConfig();
    resourceConfig.packages("com.expleague.sensearch.web");
    resourceConfig.register(new HK2ToGuiceModule(injector));

    Server server = new Server(8081);

    ResourceHandler resHandler = new ResourceHandler();
    resHandler.setBaseResource(Resource.newResource(Paths.get(config.getWebRoot()).toFile()));

    HandlerCollection handlerCollection = new HandlerCollection();
    handlerCollection.setHandlers(new Handler[] {context, resHandler});
    server.setHandler(handlerCollection);

    ServletHolder jerseyServlet = new ServletHolder(new ServletContainer(resourceConfig));
    jerseyServlet.setInitOrder(0);
    jerseyServlet.setInitParameter(
        "jersey.config.server.provider.classnames", SearchEndpoint.class.getCanonicalName());
    context.addServlet(jerseyServlet, "/*");

    server.start();
    server.join();
  }
}
