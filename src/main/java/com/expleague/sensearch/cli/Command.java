package com.expleague.sensearch.cli;

public interface Command {
  void run(String... args) throws Exception;
  void printHelp();
  String commandName();
}
