package com.expleague.sensearch.cli.commands;

import com.expleague.sensearch.cli.Command;
import com.expleague.sensearch.cli.utils.SingleArgOptions.IntOption;
import com.expleague.sensearch.cli.utils.SingleArgOptions.PathOption;
import com.expleague.sensearch.cli.utils.SingleArgPredicates.ExistingPath;
import com.expleague.sensearch.cli.utils.SingleArgPredicates.PositiveInteger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class BuildRankingPoolCmd implements Command {

  private static final String COMMAND_NAME = "build-rkpool";

  private static final PathOption INDEX_PATH = PathOption.builder()
      .shortOption("i")
      .longOption("index")
      .description("Specify path to the index")
      .predicates(ExistingPath.get())
      .build();
  private static final IntOption MAX_FILTER_ITEMS = IntOption.builder()
      .longOption("max-items")
      .description("Specify maximum items to be filtered")
      .predicates(PositiveInteger.get())
      .build();
  private static final PathOption RANKING_MODEL_PATH = PathOption.builder()
      .shortOption("m")
      .longOption("ranking-model")
      .defaultValue("Specify path to the filter model")
      .predicates(ExistingPath.get())
      .build();
  private static final IntOption POOL_ITERATIONS = IntOption.builder()
      .longOption("pool-iters")
      .description("Specify iterations count")
      .predicates(PositiveInteger.get())
      .build();
  private static final PathOption POOL_PATH = PathOption.builder()
      .shortOption("p")
      .longOption("pool-path")
      .description("Specify pool path")
      .build();

  private static final Option HELP = Option.builder()
      .argName("h")
      .longOpt("help")
      .required(false)
      .numberOfArgs(0)
      .build();

  private static final CommandLineParser CLI_PARSER = new DefaultParser();
  private static final Options OPTIONS = new Options();
  private static final BuildRankingPoolCmd INSTANCE = new BuildRankingPoolCmd();

  static {
    INDEX_PATH.addToOptions(OPTIONS);
    MAX_FILTER_ITEMS.addToOptions(OPTIONS);
    RANKING_MODEL_PATH.addToOptions(OPTIONS);
    POOL_ITERATIONS.addToOptions(OPTIONS);
    POOL_PATH.addToOptions(OPTIONS);

    OPTIONS.addOption(HELP);
  }

  private BuildRankingPoolCmd() {
  }

  public static BuildRankingPoolCmd instance() {
    return INSTANCE;
  }

  @Override
  public void run(String... args) throws Exception {
    CommandLine commandLine;
    try {
      commandLine = CLI_PARSER.parse(OPTIONS, args);
    } catch (Exception e) {
      printHelp();
      return;
    }

    if (commandLine.hasOption(HELP.getOpt())) {
      printHelp();
      return;
    }
  }

  @Override
  public void printHelp() {
    HelpFormatter helpFormatter = new HelpFormatter();
    helpFormatter.printHelp(COMMAND_NAME, OPTIONS);
  }

  @Override
  public String commandName() {
    return COMMAND_NAME;
  }
}
