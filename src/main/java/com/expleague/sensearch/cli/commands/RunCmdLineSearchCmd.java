package com.expleague.sensearch.cli.commands;

import static com.expleague.sensearch.cli.utils.CommandLineTools.hasOption;

import com.expleague.sensearch.SenSeArch;
import com.expleague.sensearch.SenSeArch.ResultItem;
import com.expleague.sensearch.SenSeArch.ResultPage;
import com.expleague.sensearch.cli.Command;
import com.expleague.sensearch.cli.utils.SingleArgOptions.EnumOption;
import com.expleague.sensearch.cli.utils.SingleArgOptions.IntOption;
import com.expleague.sensearch.cli.utils.SingleArgOptions.PathOption;
import com.expleague.sensearch.cli.utils.SingleArgPredicates.ExistingPath;
import com.expleague.sensearch.cli.utils.SingleArgPredicates.PositiveInteger;
import com.expleague.sensearch.core.SenSeArchImpl;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunCmdLineSearchCmd implements Command {
  private static final String COMMAND_NAME = "cmdl-srch";

  private enum OutputFormat {
    JSON,
    PROTOBUF
  }

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

  private static final PathOption OUTPUT = PathOption.builder()
      .shortOption("o")
      .longOption("output")
      .description("Specify output file for the session. If no path provided output "
          + "will be done only to the stdout")
      .predicates(ExistingPath.negated())
      .build();
  private static final EnumOption<OutputFormat> OUTPUT_FORMAT = EnumOption.builder(OutputFormat.class)
      .longOption("format")
      .description("Specify output format for result pages."
          + " This option has the meaning when 'output' option is specified")
      .build();

  private static final Option HELP = Option.builder("h")
      .longOpt("help")
      .numberOfArgs(0)
      .build();

  private static final RunCmdLineSearchCmd INSTANCE = new RunCmdLineSearchCmd();
  private static final Logger LOG = LoggerFactory.getLogger(RunCmdLineSearchCmd.class);
  private static final CommandLineParser CLI_PARSER = new DefaultParser();
  private static final Options OPTIONS = new Options();
  static {
    INDEX_PATH.addToOptions(OPTIONS);
    RANKING_MODEL_PATH.addToOptions(OPTIONS);
    FILTER_MODEL_PATH.addToOptions(OPTIONS);
    FILTERED_ITEMS_COUNT.addToOptions(OPTIONS);
    RESULT_PAGE_SIZE.addToOptions(OPTIONS);

    OUTPUT.addToOptions(OPTIONS);
    OUTPUT_FORMAT.addToOptions(OPTIONS);

    OPTIONS.addOption(HELP);
  }

  private RunCmdLineSearchCmd() {}

  public static String commandName() {
    return COMMAND_NAME;
  }

  public static RunCmdLineSearchCmd instance() {
    return INSTANCE;
  }

  @Override
  public void run(String... args) throws Exception {
    CommandLine commandLine;
    try {
      commandLine = CLI_PARSER.parse(OPTIONS, args);
    } catch (ParseException e) {
      printHelp();
      return;
    }

    if (hasOption(commandLine, HELP)) {
      printHelp();
      return;
    }

    System.out.println("Coming soon!");

//    if (commandLine.hasOption(COMMAND_LINE_SEARCH.getOpt())) {
//      System.out.println("Greetings!");
//      System.out.println("Enter empty query to exit search");
//      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
//      SenSeArch senSeArch = injector.getInstance(SenSeArchImpl.class);
//      String query;
//      Gson gson = new GsonBuilder().setPrettyPrinting().create();
//      JsonParser parser = new JsonParser();
//       TODO: correct pretty printing of result pages
//      while ((query = bufferedReader.readLine()) != null) {
//        ResultPage page = senSeArch.search(query, 1, false, false);
//        ResultItem[] resultItems = page.results();
//        System.out.println(gson.toJson(resultItems));
//      }
//    }
  }

  @Override
  public void printHelp() {

  }
}
