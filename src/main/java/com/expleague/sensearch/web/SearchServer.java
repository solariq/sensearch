package com.expleague.sensearch.web;

import org.eclipse.jetty.server.Server;

public class SearchServer {


  public static void main(String[] args) throws Exception {
    Builder.build();

    Server server = new Server(8081);

    server.setHandler(Builder.getPageLoadHandler());

    server.start();
    server.join();
  }
}
