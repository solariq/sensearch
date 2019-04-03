package com.expleague.sensearch.web;

import com.expleague.sensearch.AppModule;
import com.expleague.sensearch.Config;
import com.expleague.sensearch.ConfigImpl;
import com.expleague.sensearch.donkey.IndexBuilder;
import com.expleague.sensearch.index.Index;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.Properties;
import javax.servlet.DispatcherType;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
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

  private static final Logger LOG = Logger.getLogger(SearchServer.class.getName());
  private final Index index;

  @Inject
  // Index is marked as a dependency so it will be loaded before server starts
  public SearchServer(Index index) {
    this.index = index;
  }

  public static void main(String[] args) throws Exception {
    System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog");
    System.setProperty("org.eclipse.jetty.LEVEL", "OFF");

    Properties logProperties = new Properties();
    logProperties.load(Files.newInputStream(Paths.get("log4j.properties")));
    PropertyConfigurator.configure(logProperties);

    Config config =
        new ObjectMapper().readValue(Paths.get("./config.json").toFile(), ConfigImpl.class);
    Injector injector = Guice.createInjector(new AppModule(config));

    if (config.getBuildIndexFlag()) {
      IndexBuilder indexBuilder = injector.getInstance(IndexBuilder.class);
      if (config.getTrainEmbeddingFlag()) {
        indexBuilder.buildIndexAndEmbedding();
      } else {
        indexBuilder.buildIndex();
      }
    }

    injector.getInstance(SearchServer.class).start(injector);
  }

  // TODO: strange method signature, should move this injector logic out of it
  public void start(Injector injector) throws Exception {
    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/api");

    // Set up cors to be able to make request from frontend
    FilterHolder cors =
        context.addFilter(CrossOriginFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
    cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
    cors.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
    cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,POST,HEAD");
    cors.setInitParameter(
        CrossOriginFilter.ALLOWED_HEADERS_PARAM, "X-Requested-With,Content-Type,Accept,Origin");

    ResourceConfig resourceConfig = new ResourceConfig();
    resourceConfig.packages("com.expleague.sensearch.web");
    resourceConfig.register(new HK2ToGuiceModule(injector));
    resourceConfig.register(new DebugExceptionMapper());

    Server server = new Server(8081);

    ResourceHandler resHandler = new ResourceHandler();
    resHandler.setBaseResource(Resource.newResource(Paths.get("webapp/dist/webapp").toFile()));

    HandlerCollection handlerCollection = new HandlerCollection();
    handlerCollection.setHandlers(new Handler[]{context, resHandler});
    server.setHandler(handlerCollection);

    ServletHolder jerseyServlet = new ServletHolder(new ServletContainer(resourceConfig));
    jerseyServlet.setInitOrder(0);
    jerseyServlet.setInitParameter(
        "jersey.config.server.provider.classnames", SearchEndpoint.class.getCanonicalName());
    context.addServlet(jerseyServlet, "/*");

    server.start();

    LOG.info("Server started!");

    server.join();
  }
}
