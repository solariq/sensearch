package com.expleague.sensearch.cli.commands;

import static com.expleague.sensearch.cli.utils.CommandLineTools.checkOptions;

import com.expleague.commons.random.FastRandom;
import com.expleague.sensearch.ConfigImpl;
import com.expleague.sensearch.cli.Command;
import com.expleague.sensearch.cli.utils.SingleArgOptions.IntOption;
import com.expleague.sensearch.cli.utils.SingleArgOptions.PathOption;
import com.expleague.sensearch.cli.utils.SingleArgPredicates.ExistingPath;
import com.expleague.sensearch.metrics.SearchEvaluator;
import com.expleague.sensearch.miner.pool.QueryAndResults;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class EvalCmd implements Command {

  private static final String COMMAND_NAME = "eval";

  private static final PathOption INDEX_PATH = PathOption.builder()
      .shortOption("i")
      .longOption("index")
      .description("Specify path to the index")
      .predicates(ExistingPath.get())
      .build();

  private static final PathOption FILTER_MODEL_PATH = PathOption.builder()
      .shortOption("f")
      .longOption("filter-model")
      .description("Specify path to the filter model")
      .build();
  private static final PathOption RANKING_MODEL_PATH = PathOption.builder()
      .shortOption("r")
      .longOption("ranking-model")
      .description("Specify path to the filter model")
      .predicates(ExistingPath.get())
      .build();

  private static final PathOption QUERIES_PATH = PathOption.builder()
      .shortOption("q")
      .longOption("queries-path")
      .description("Specify path to the test queries")
      .build();

  private static final IntOption NUM_OF_QUERIES = IntOption.builder()
      .shortOption("n")
      .longOption("num-queries")
      .description("Number of queries to be used for evaluation")
      .build();

  private static final IntOption MIN_RECALL = IntOption.builder()
      .longOption("min-recall")
      .description("Min recall num for evaluation")
      .defaultValue(10)
      .build();

  private static final IntOption MAX_RECALL = IntOption.builder()
      .longOption("max-recall")
      .description("Max recall num for evaluation")
      .defaultValue(100_000)
      .build();

  private static final IntOption RECALL_STEP = IntOption.builder()
      .shortOption("s")
      .longOption("recall-step")
      .description("Step of recall num")
      .defaultValue(10)
      .build();


  private static final Option HELP = Option.builder("h").
      longOpt("help")
      .desc("Show help message")
      .numberOfArgs(0)
      .build();

  private static final Options OPTIONS = new Options();
  private static final CommandLineParser CLI_PARSER = new DefaultParser();
  private static final EvalCmd INSTANCE = new EvalCmd();
  private static final FastRandom random = new FastRandom(239);

  static {
    INDEX_PATH.addToOptions(OPTIONS);
    FILTER_MODEL_PATH.addToOptions(OPTIONS);
    RANKING_MODEL_PATH.addToOptions(OPTIONS);
    QUERIES_PATH.addToOptions(OPTIONS);
    NUM_OF_QUERIES.addToOptions(OPTIONS);

    MIN_RECALL.addToOptions(OPTIONS);
    MAX_RECALL.addToOptions(OPTIONS);
    RECALL_STEP.addToOptions(OPTIONS);

    OPTIONS.addOption(HELP);
  }

  private EvalCmd() {
  }

  public static EvalCmd instance() {
    return INSTANCE;
  }

  public static String commandName() {
    return COMMAND_NAME;
  }

  @Override
  public void run(String... args) throws Exception {
    CommandLine commandLine;
    try {
      commandLine = CLI_PARSER.parse(OPTIONS, args);
    } catch (Exception e) {
      System.out.println(e.getMessage());
      printHelp();
      return;
    }

    if (commandLine.hasOption(HELP.getOpt())) {
      printHelp();
      return;
    }

    checkOptions(commandLine, INDEX_PATH, QUERIES_PATH);

    ConfigImpl config = new ConfigImpl();
    config.setTemporaryIndex(INDEX_PATH.value(commandLine).toString());
    if (FILTER_MODEL_PATH.hasOption(commandLine)) {
      config.setModelFilterPath(FILTER_MODEL_PATH.value(commandLine).toString());
    }
    if (RANKING_MODEL_PATH.hasOption(commandLine)) {
      config.setModelPath(RANKING_MODEL_PATH.value(commandLine).toString());
    }

    ObjectMapper mapper = new ObjectMapper();
    QueryAndResults[] queryData = mapper.readValue(
        Files.newBufferedReader(QUERIES_PATH.value(commandLine), StandardCharsets.UTF_8), QueryAndResults[].class);

    int num = NUM_OF_QUERIES.hasOption(commandLine) ? NUM_OF_QUERIES.value(commandLine) : queryData.length;
    List<Integer> ids = IntStream.range(0, queryData.length).boxed().collect(Collectors.toList());
    Collections.shuffle(ids, random);
    QueryAndResults[] sampledQueryData = ids.stream()
        .limit(num)
        .map(i -> queryData[i])
        .toArray(QueryAndResults[]::new);

    SearchEvaluator searchEvaluator = new SearchEvaluator(
        config,
        MIN_RECALL.value(commandLine),
        MAX_RECALL.value(commandLine),
        RECALL_STEP.value(commandLine));
    searchEvaluator.evalAndPrint(sampledQueryData);
  }

  @Override
  public void printHelp() {
    HelpFormatter helpFormatter = new HelpFormatter();
    helpFormatter.printHelp(COMMAND_NAME, OPTIONS);

  }
}
