package com.expleague.sensearch.cli.commands;

import static com.expleague.sensearch.cli.utils.CommandLineTools.checkOptions;

import com.expleague.sensearch.AppModule;
import com.expleague.sensearch.ConfigImpl;
import com.expleague.sensearch.cli.Command;
import com.expleague.sensearch.cli.utils.SingleArgOptions.IntOption;
import com.expleague.sensearch.cli.utils.SingleArgOptions.PathOption;
import com.expleague.sensearch.cli.utils.SingleArgPredicates.ExistingPath;
import com.expleague.sensearch.cli.utils.SingleArgPredicates.NegativeInteger;
import com.expleague.sensearch.cli.utils.SingleArgPredicates.PositiveInteger;
import com.expleague.sensearch.miner.pool.builders.FilterPoolBuilder;
import com.google.inject.Guice;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuildFilterPoolCmd implements Command {

  private static final String COMMAND_NAME = "build-fpool";

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
  private static final PathOption FILTER_MODEL_PATH = PathOption.builder()
      .shortOption("m")
      .longOption("filter-model")
      .defaultValue("Specify path to the filter model")
      .predicates(ExistingPath.get())
      .build();
  private static final IntOption POOL_ITERATIONS = IntOption.builder()
      .longOption("pool-iters")
      .description("Specify iterations count")
      .predicates(NegativeInteger.negated())
      .build();
  private static final PathOption POOL_PATH = PathOption.builder()
      .shortOption("p")
      .longOption("pool-path")
      .description("Specify pool path")
      .build();

  private static final Option HELP = Option.builder("h")
      .longOpt("help")
      .required(false)
      .numberOfArgs(0)
      .build();

  private static final Logger LOG = LoggerFactory.getLogger(BuildFilterPoolCmd.class);
  private static final CommandLineParser CLI_PARSER = new DefaultParser();
  private static final Options OPTIONS = new Options();
  private static final BuildFilterPoolCmd INSTANCE = new BuildFilterPoolCmd();

  static {
    INDEX_PATH.addToOptions(OPTIONS);
    MAX_FILTER_ITEMS.addToOptions(OPTIONS);
    FILTER_MODEL_PATH.addToOptions(OPTIONS);
    POOL_ITERATIONS.addToOptions(OPTIONS);
    POOL_PATH.addToOptions(OPTIONS);

    OPTIONS.addOption(HELP);
  }

  private BuildFilterPoolCmd() {
  }

  public static BuildFilterPoolCmd instance() {
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

    checkOptions(commandLine, INDEX_PATH, MAX_FILTER_ITEMS, POOL_ITERATIONS, POOL_PATH);

    ConfigImpl config = new ConfigImpl();
    config.setTemporaryIndex(INDEX_PATH.value(commandLine).toString());
    config.setMaxFilterItems(MAX_FILTER_ITEMS.value(commandLine));
    if (FILTER_MODEL_PATH.hasOption(commandLine)) {
      config.setModelFilterPath(FILTER_MODEL_PATH.value(commandLine).toString());
      LOG.info(String.format("Received filter model by given path [ %s ]",
          FILTER_MODEL_PATH.value(commandLine).toString()));
    }

    Guice.createInjector(new AppModule(config))
        .getInstance(FilterPoolBuilder.class)
        .build(POOL_PATH.value(commandLine), POOL_ITERATIONS.value(commandLine));

  }

  @Override
  public void printHelp() {
    HelpFormatter helpFormatter = new HelpFormatter();
    helpFormatter.printHelp(COMMAND_NAME, OPTIONS);
  }

  public static String commandName() {
    return COMMAND_NAME;
  }
}
