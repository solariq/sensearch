package com.expleague.sensearch.cli.commands;

import com.expleague.sensearch.AppModule;
import com.expleague.sensearch.ConfigImpl;
import com.expleague.sensearch.SenSeArch;
import com.expleague.sensearch.SenSeArch.ResultPage;
import com.expleague.sensearch.cli.Command;
import com.expleague.sensearch.cli.utils.SingleArgOptions;
import com.expleague.sensearch.cli.utils.SingleArgOptions.IntOption;
import com.expleague.sensearch.cli.utils.SingleArgOptions.PathOption;
import com.expleague.sensearch.cli.utils.SingleArgPredicates.ExistingPath;
import com.expleague.sensearch.cli.utils.SingleArgPredicates.PositiveInteger;
import com.expleague.sensearch.core.SenSeArchImpl;
import com.expleague.sensearch.web.SearchServer;
import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RunSearchCmd implements Command {

  private static final String COMMAND_NAME = "start-srch";

  private static final Logger LOG = LoggerFactory.getLogger(RunSearchCmd.class);

  private static final PathOption INDEX_PATH = PathOption.builder()
      .shortOption("i")
      .longOption("index-path")
      .description("Specify path to the index")
      .predicates(ExistingPath.get())
      .build();
  private static final PathOption RANKING_MODEL_PATH = PathOption.builder()
      .shortOption("r")
      .longOption("ranking-model")
      .description("Specify path to the ranking model")
      .predicates(ExistingPath.get())
      .build();
  private static final PathOption FILTER_MODEL_PATH = PathOption.builder()
      .shortOption("f")
      .longOption("filter-model")
      .description("Specify path to the filter model")
      .predicates(ExistingPath.get())
      .build();
  private static final IntOption FILTERED_ITEMS_COUNT = IntOption.builder()
      .longOption("max-filter")
      .description("Set maximum count of items to be filtered for ranking")
      .defaultValue(1000)
      .predicates(PositiveInteger.get())
      .build();
  private static final IntOption RESULT_PAGE_SIZE = IntOption.builder()
      .longOption("serp-size")
      .description("Set results count per page")
      .defaultValue(10)
      .predicates(PositiveInteger.get())
      .build();

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
  private static final CommandLineParser CLI_PARSER = new DefaultParser();
  private static final RunSearchCmd INSTANCE = new RunSearchCmd();

  static {
    INDEX_PATH.addToOptions(OPTIONS);
    RANKING_MODEL_PATH.addToOptions(OPTIONS);
    FILTER_MODEL_PATH.addToOptions(OPTIONS);
    RESULT_PAGE_SIZE.addToOptions(OPTIONS);
    FILTERED_ITEMS_COUNT.addToOptions(OPTIONS);

    OPTIONS.addOption(COMMAND_LINE_SEARCH);
    OPTIONS.addOption(HELP);
  }

  private RunSearchCmd() {

  }

  public static RunSearchCmd instance() {
    return INSTANCE;
  }

  @Override
  public String commandName() {
    return COMMAND_NAME;
  }

  @Override
  public void run(String[] args) throws Exception {
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

    SingleArgOptions.checkOptions(commandLine, INDEX_PATH, RANKING_MODEL_PATH, FILTER_MODEL_PATH,
        RESULT_PAGE_SIZE, FILTERED_ITEMS_COUNT);

    ConfigImpl config = new ConfigImpl();
    config.setTemporaryIndex(INDEX_PATH.value(commandLine).toString());
    // TODO: configure LSH from command line, but why?
    config.setLshNearestFlag(true);
    config.setMaxFilterItems(FILTERED_ITEMS_COUNT.value(commandLine));
    config.setModelPath(RANKING_MODEL_PATH.value(commandLine).toString());
    config.setModelFilterPath(FILTER_MODEL_PATH.value(commandLine).toString());
    config.setPageSize(RESULT_PAGE_SIZE.value(commandLine));

    Injector injector = Guice.createInjector(new AppModule(config));
    if (commandLine.hasOption(COMMAND_LINE_SEARCH.getOpt())) {
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
      SenSeArch senSeArch = injector.getInstance(SenSeArchImpl.class);
      // TODO: stop condition!
      String query;
      Gson gson = new Gson();
      while ((query = bufferedReader.readLine()) != null) {
        ResultPage page = senSeArch.search(query, 1, false, false);
        System.out.println(gson.toJson(page));
      }
    } else {
      injector.getInstance(SearchServer.class).start(injector);
    }
  }

  @Override
  public void printHelp() {
    HelpFormatter helpFormatter = new HelpFormatter();
    helpFormatter.printHelp(COMMAND_NAME, OPTIONS);
  }
}
