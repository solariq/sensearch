package com.expleague.sensearch.cli.commands;

import com.expleague.sensearch.cli.Command;
import com.expleague.sensearch.cli.utils.SingleArgOptions.PathOption;
import com.expleague.sensearch.cli.utils.SingleArgPredicates.ExistingPath;

public class BuildRankingPoolCmd implements Command {

  private static final String COMMAND_NAME = "build-rkpool";

  private static final PathOption INDEX_PATH = PathOption.builder()
      .shortOption("i")
      .longOption("index")
      .description("Specify path to the index")
      .predicates(ExistingPath.get())
      .build();
  @Override
  public void run(String... args) throws Exception {

  }

  @Override
  public void printUsage() {

  }

  @Override
  public String commandName() {
    return COMMAND_NAME;
  }
}
