package com.expleague.sensearch.cli;

import com.expleague.sensearch.AppModule;
import com.expleague.sensearch.Config;
import com.expleague.sensearch.ConfigImpl;
import com.expleague.sensearch.cli.utils.SingleArgOptions;
import com.expleague.sensearch.cli.utils.SingleArgOptions.IntOption;
import com.expleague.sensearch.cli.utils.SingleArgOptions.PathOption;
import com.expleague.sensearch.cli.utils.SingleArgPredicates;
import com.expleague.sensearch.web.SearchServer;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ServerCli {

  private static final Logger LOG = LoggerFactory.getLogger(ServerCli.class);

  private static final PathOption INDEX_PATH = new PathOption(Option.builder("i")
      .longOpt("index-path")
      .desc("Specify path to the index")
      .numberOfArgs(1)
      .build(), SingleArgPredicates.ExistingPath.get()
  );
  private static final PathOption RANKING_MODEL_PATH = new PathOption(
      Option.builder("r")
          .longOpt("ranking-model")
          .desc("Specify path to the ranking model")
          .numberOfArgs(1)
          .build(), SingleArgPredicates.ExistingPath.get()
  );
  private static final PathOption FILTER_MODEL_PATH = new PathOption(
      Option.builder("f")
          .longOpt("filter-model")
          .desc("Specify path to the filter model")
          .numberOfArgs(1)
          .build(), SingleArgPredicates.ExistingPath.get()
  );
  private static final IntOption FILTERED_ITEMS_COUNT = new IntOption(
      Option.builder()
          .longOpt("max-filter")
          .desc("Set maximum count of items to be filtered for ranking")
          .numberOfArgs(1)
          .build(), 1000, SingleArgPredicates.PositiveInteger.get()
  );
  private static final IntOption RESULT_PAGE_SIZE = new IntOption(
      Option.builder()
          .longOpt("serp-size")
          .desc("Set results count per page")
          .numberOfArgs(1)
          .build(), 10, SingleArgPredicates.PositiveInteger.get()
  );

  private static final Option COMMAND_LINE_SEARCH = Option.builder("c")
      .longOpt("cmd-line")
      .desc("Run search in command line. It is useful for debug purposes")
      .numberOfArgs(0)
      .build();
  private static final Option HELP = Option.builder("h")
      .longOpt("help")
      .desc("Print help message")
      .numberOfArgs(0)
      .build();


  private static final Options OPTIONS = new Options();

  static {
    INDEX_PATH.addToOptions(OPTIONS);
    RANKING_MODEL_PATH.addToOptions(OPTIONS);
    FILTER_MODEL_PATH.addToOptions(OPTIONS);
    RESULT_PAGE_SIZE.addToOptions(OPTIONS);
    FILTERED_ITEMS_COUNT.addToOptions(OPTIONS);

    OPTIONS.addOption(COMMAND_LINE_SEARCH);
    OPTIONS.addOption(HELP);
  }

  private static final CommandLineParser CLI_PARSER = new DefaultParser();

  public static void run(String[] args) throws Exception {
    CommandLine commandLine;
    try {
      commandLine = CLI_PARSER.parse(OPTIONS, args);
    } catch (Exception e) {
      printUsage();
      return;
    }

    if (commandLine.hasOption(HELP.getOpt())) {
      printUsage();
      return;
    }

    SingleArgOptions.checkOptions(commandLine, INDEX_PATH, RANKING_MODEL_PATH, FILTER_MODEL_PATH,
        RESULT_PAGE_SIZE, FILTERED_ITEMS_COUNT);


    ConfigImpl config = new ConfigImpl();
    config.setTemporaryIndex(INDEX_PATH.value(commandLine).toString());
    // TODO: configure LSH from command line, but why?
    config.setLshNearestFlag(true);
    config.setMaxFilterItems(FILTERED_ITEMS_COUNT.value(commandLine));
    config.setModelPath(RANKING_MODEL_PATH.value(commandLine).toString());
    config.setModelFilterPath(FILTER_MODEL_PATH.value(commandLine).toString());
    Injector injector = Guice.createInjector(new AppModule(config));
    injector.getInstance(SearchServer.class).start(injector);
  }

  static void printUsage() {
    HelpFormatter helpFormatter = new HelpFormatter();
    helpFormatter.printHelp("srch", OPTIONS);
  }
}
