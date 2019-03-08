package com.expleague.sensearch;

import com.expleague.ml.cli.builders.data.DataBuilder;
import com.expleague.ml.cli.builders.data.impl.DataBuilderCrossValidation;
import com.expleague.ml.cli.builders.methods.impl.GradientBoostingBuilder;
import com.expleague.ml.cli.builders.methods.impl.GreedyObliviousTreeBuilder;
import com.expleague.ml.methods.VecOptimization;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FitRankingCli {

  private static final Logger LOG = LoggerFactory.getLogger(FitRankingCli.class);
  private static final String MODE_DESCRIPTION = "";

  private static final Option TRAIN_DATA = Option.builder("D")
      .longOpt("data")
      .desc("Specify path to the train data")
      .numberOfArgs(1)
      .required(false)
      .build();
  private static final int DEFAULT_SEED = 42;
  private static final Option SEED = Option.builder("r")
      .longOpt("random-seed")
      .desc("Specify random seed to make results reproducible")
      .numberOfArgs(1)
      .required(false)
      .build();
  private static final double DEFAULT_CROSS_VALIDATION = 0;
  private static final Option CROSS_VALIDATION = Option.builder("c")
      .longOpt("cross-val")
      .desc(
          "Specify proportion of the data that will be used for the testing, the rest will be used for training."
              + " Should be the number from (0; 1). Usually, 0.2 is enough."
              + " If no value is given all data will be used for training")
      .numberOfArgs(1)
      .required(false)
      .build();

  // gradient boosting on oblivious trees specific arguments (L1, L2 ?)
  // also option for target function should be added
  private static final int DEFAULT_DEPTH = 5;
  private static final Option DEPTH = Option.builder("d")
      .longOpt("max-depth")
      .desc(
          "Specify maximum depth of a tree in the ensemble. Default value is 5. Usually the depth should not be more than 15")
      .numberOfArgs(1)
      .required(false)
      .build();
  private static final double DEFAULT_STEP = 0.01;
  private static final Option STEP = Option.builder("s")
      .longOpt("step")
      .desc(
          "Specify shrinkage coefficient for single step. The less value is the more iterations is needed to fit a data")
      .numberOfArgs(1)
      .required(false)
      .build();
  private static final int DEFAULT_ITERATIONS = 100;
  private static final Option ITERATIONS = Option.builder("i")
      .longOpt("iterations")
      .desc("Specify count of trees in the ensemble, i.e. iterations of the algorithm."
          + " This value depends on the value of the step, the more step is the less iterations is needed")
      .numberOfArgs(1)
      .required(false)
      .build();

  // output option
  private static final Option OUTPUT = Option.builder("o")
      .longOpt("output")
      .desc("Path to the output file. Execution will be terminated if output file already exists")
      .numberOfArgs(1)
      .required(false)
      .build();

  private static final Option VERBOSE = Option.builder("v")
      .longOpt("verbose")
      .desc("Make process more verbose")
      .numberOfArgs(0)
      .required(false)
      .build();

  private static final Option LOG_FILE = Option.builder("l")
      .longOpt("log-file")
      .desc("Specify log file to which all messages will be written. If no file is given then will"
          + " be used standard output")
      .numberOfArgs(1)
      .required(false)
      .build();

  private static final Option HELP = Option.builder("h").
      longOpt("help")
      .desc("Show help message")
      .numberOfArgs(0)
      .required(false)
      .build();

  private static Options OPTIONS = new Options()
      .addOption(TRAIN_DATA)
      .addOption(SEED)
      .addOption(CROSS_VALIDATION)
      .addOption(DEPTH)
      .addOption(STEP)
      .addOption(ITERATIONS)
      .addOption(OUTPUT)
      .addOption(VERBOSE)
//      .addOption(LOG_FILE)
      .addOption(HELP);

  private static final CommandLineParser CLI_PARSER = new DefaultParser();

  static void run(String[] args) throws Exception {
    CommandLine commandLine;
    try {
      commandLine = CLI_PARSER.parse(OPTIONS, args);
    } catch (Exception e) {
      printHelp();
      return;
    }

    if (!commandLine.hasOption(TRAIN_DATA.getOpt())) {
      System.err
          .printf("Path to a training data must be specified! Set parameter '-%s' to do that!",
              TRAIN_DATA.getOpt());
      return;
    }

    if (!commandLine.hasOption(OUTPUT.getOpt())) {
      System.err.printf("Path for the model must be provided! Set parameter '-%s' to do that!",
          OUTPUT.getOpt());
      return;
    } else if (Files.exists(Paths.get(commandLine.getOptionValue(OUTPUT.getOpt())))) {
      System.err.printf("File or directory already exists by given path %s",
          commandLine.getOptionValue(OUTPUT.getOpt()));
      return;
    }

    final DataBuilder dataBuilder;
    if (commandLine.hasOption(CROSS_VALIDATION_OPTION)) {
      final DataBuilderCrossValidation dataBuilderCrossValidation = new DataBuilderCrossValidation();
      final String[] cvOptions = StringUtils
          .split(command.getOptionValue(CROSS_VALIDATION_OPTION), "/", 2);
      dataBuilderCrossValidation.setRandomSeed(Long.valueOf(cvOptions[0]));
      dataBuilderCrossValidation.setPartition(cvOptions[1]);
      dataBuilder = dataBuilderCrossValidation;
    } else {
      dataBuilder = new DataBuilderClassic();
      ((DataBuilderClassic) dataBuilder).setTestPath(command.getOptionValue(TEST_OPTION));
    }
    dataBuilder.setLearnPath(command.getOptionValue(LEARN_OPTION));
    CliPoolReaderHelper.setPoolReader(command, dataBuilder);

    final Pair<? extends Pool, ? extends Pool> pools = dataBuilder.create();
    final Pool learn = pools.getFirst();
    final Pool test = pools.getSecond();

    VecOptimization optimizer;
    try {
      optimizer = createOptimizer(commandLine);
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }

  @Nullable
  private static VecOptimization createOptimizer(CommandLine commandLine) throws Exception {
    int treeMaxDepth;
    treeMaxDepth = intOption(commandLine, DEPTH, DEFAULT_DEPTH);
    if (treeMaxDepth < 1) {
      throw new Exception(
          String.format("Parameter '-%s' must be a positive integer number! Received %d instead",
              DEPTH.getOpt(), treeMaxDepth));
    }

    int iterations;
    iterations = intOption(commandLine, ITERATIONS, DEFAULT_ITERATIONS);
    if (iterations < 1) {
      throw new Exception(
          String.format("Parameter '-%s' must be a positive integer number! Received %d instead",
              ITERATIONS.getOpt(), iterations));
    }

    double step;
    step = doubleOption(commandLine, STEP, DEFAULT_STEP);
    if (step < Double.MIN_VALUE) {
      throw new Exception(
          String.format("Parameter '-%s' must be a positive real number! Received %f instead",
              STEP.getOpt(), step));
    }

    GradientBoostingBuilder boostingBuilder = new GradientBoostingBuilder();

    GreedyObliviousTreeBuilder weakBuilder = new GreedyObliviousTreeBuilder();
    weakBuilder.setDepth(treeMaxDepth);

    boostingBuilder.setWeak(weakBuilder.create());
    boostingBuilder.setIterations(iterations);
    boostingBuilder.setStep(step);
    // TODO: parse loss from a command line!
    boostingBuilder.setLocal("SatL2");

    return boostingBuilder.create();
  }

  static void printHelp() {

  }

  private static int intOption(CommandLine commandLine, Option option, int defaultValue)
      throws Exception {
    if (!commandLine.hasOption(option.getOpt())) {
      return defaultValue;
    }
    String optionValue = commandLine.getOptionValue(option.getOpt());
    try {
      return Integer.parseInt(optionValue);
    } catch (NumberFormatException e) {
      throw new Exception(String.format("Value for the parameter %s must be an integer,"
          + " received '-%s' instead", option.getOpt(), option));
    }
  }

  private static double doubleOption(CommandLine commandLine, Option option, double defaultValue)
      throws Exception {
    if (!commandLine.hasOption(option.getOpt())) {
      return defaultValue;
    }
    String optionValue = commandLine.getOptionValue(option.getOpt());
    try {
      return Double.parseDouble(optionValue);
    } catch (NumberFormatException e) {
      throw new Exception(String.format("Value for the parameter '-%s' must be a real with a dot"
          + " as decimal separator, received %s instead", option.getOpt(), option));
    }
  }
}
