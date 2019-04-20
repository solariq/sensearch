package com.expleague.sensearch.cli.commands;

import com.expleague.commons.func.WeakListenerHolder;
import com.expleague.commons.math.Func;
import com.expleague.commons.math.Trans;
import com.expleague.commons.util.Pair;
import com.expleague.commons.util.logging.Interval;
import com.expleague.ml.TargetFunc;
import com.expleague.ml.cli.builders.data.DataBuilder;
import com.expleague.ml.cli.builders.data.ReaderFactory;
import com.expleague.ml.cli.builders.data.impl.DataBuilderCrossValidation;
import com.expleague.ml.cli.builders.methods.grid.GridBuilder;
import com.expleague.ml.cli.builders.methods.impl.GradientBoostingBuilder;
import com.expleague.ml.cli.builders.methods.impl.GreedyObliviousTreeBuilder;
import com.expleague.ml.cli.output.ModelWriter;
import com.expleague.ml.cli.output.printers.DefaultProgressPrinter;
import com.expleague.ml.cli.output.printers.ResultsPrinter;
import com.expleague.ml.data.tools.Pool;
import com.expleague.ml.methods.VecOptimization;
import com.expleague.sensearch.cli.Command;
import com.expleague.sensearch.cli.utils.SingleArgOptions;
import com.expleague.sensearch.cli.utils.SingleArgOptions.DoubleOption;
import com.expleague.sensearch.cli.utils.SingleArgOptions.EnumOption;
import com.expleague.sensearch.cli.utils.SingleArgOptions.IntOption;
import com.expleague.sensearch.cli.utils.SingleArgOptions.LongOption;
import com.expleague.sensearch.cli.utils.SingleArgOptions.PathOption;
import com.expleague.sensearch.cli.utils.SingleArgPredicates.ExistingPath;
import com.expleague.sensearch.cli.utils.SingleArgPredicates.NegativeLong;
import com.expleague.sensearch.cli.utils.SingleArgPredicates.PositiveDouble;
import com.expleague.sensearch.cli.utils.SingleArgPredicates.PositiveInteger;
import com.expleague.sensearch.cli.utils.SingleArgPredicates.SegmentDouble;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FitRankingModelCmd implements Command {

  private static final String COMMAND_NAME = "train-rk";

  private static final Logger LOG = LoggerFactory.getLogger(FitRankingModelCmd.class);
  private static final String MODE_DESCRIPTION = "";

  private static final PathOption TRAIN_DATA = PathOption.builder()
      .shortOption("D")
      .longOption("data")
      .description("Specify path to the train data")
      .predicates(ExistingPath.get())
      .build();
  private static final LongOption SEED = LongOption.builder()
      .shortOption("r")
      .longOption("random-seed")
      .description("Specify random seed to make results reproducible")
      .defaultValue(42)
      .predicates(NegativeLong.get().negate())
      .build();
  private static final DoubleOption TEST_PROPORTION = DoubleOption.builder()
      .shortOption("t")
      .longOption("test-prop")
      .description(
          "Specify proportion of the data that will be used for the testing,"
              + " the rest will be used for training."
              + " Should be the number from (0; 1). Usually, 0.2 is enough."
              + " If no value is given all data will be used for training")
      .defaultValue(0.2)
      .predicates(SegmentDouble.get(0.01, 1))
      .build();
  // gradient boosting on oblivious trees specific arguments (L1, L2 ?)
  // also option for target function should be added
  private static final IntOption DEPTH = IntOption.builder()
      .shortOption("d")
      .longOption("max-depth")
      .description(
          "Specify maximum depth of a tree in the ensemble. Default value is 5."
              + " Usually the depth should not be more than 15")
      .defaultValue(5)
      .predicates(PositiveInteger.get())
      .build();
  private static final DoubleOption STEP = DoubleOption.builder()
      .shortOption("s")
      .longOption("step")
      .description(
          "Specify shrinkage coefficient for single step. The less value is the more iterations is needed to fit a data")
      .defaultValue(0.01)
      .predicates(PositiveDouble.get())
      .build();
  private static final IntOption ITERATIONS = IntOption.builder()
      .shortOption("i")
      .longOption("iterations")
      .description("Specify count of trees in the ensemble, i.e. iterations of the algorithm."
          + " This value depends on the value of the step, the more step is the less iterations is needed")
      .defaultValue(100)
      .predicates(PositiveInteger.get())
      .build();
  private static final IntOption GRID_BINS = IntOption.builder()
      .shortOption("g")
      .longOption("grid-bins")
      .description("Specify degree of binarization of the data. This will determine the number"
          + " of possible bins formed for features")
      .defaultValue(32)
      .predicates(PositiveInteger.get())
      .build();
  private static final EnumOption<Target> TARGET_LOSS = EnumOption.builder(Target.class)
      .shortOption("t")
      .longOption("target-loss")
      .description("Specify target function for optimization")
      .defaultValue(Target.MSE)
      .build();
  private static final EnumOption<Metric> TEST_METRIC = EnumOption.builder(Metric.class)
      .shortOption("m")
      .longOption("test-metric")
      .description("Specify test metric which will be calculated on test data set")
      .defaultValue(Metric.NONE)
      .build();
  private static final PathOption OUTPUT = PathOption.builder()
      .shortOption("o")
      .longOption("output")
      .description(
          "Path to the output file. Execution will be terminated if output file already exists")
      .predicates(ExistingPath.get())
      .build();

  private static final Option VERBOSE = Option.builder("v")
      .longOpt("verbose")
      .desc("Make process more verbose")
      .numberOfArgs(0)
      .build();
  private static final IntOption PRINT_PERIOD = IntOption.builder()
      .shortOption("p")
      .longOption("print-period")
      .description("Print period in iterations")
      .defaultValue(10)
      .predicates(PositiveInteger.get())
      .build();

  private static final Option HELP = Option.builder("h").
      longOpt("help")
      .desc("Show help message")
      .numberOfArgs(0)
      .build();
  private static final Options OPTIONS = new Options();
  private static final CommandLineParser CLI_PARSER = new DefaultParser();
  private static final FitRankingModelCmd INSTANCE = new FitRankingModelCmd();

  static {
    TRAIN_DATA.addToOptions(OPTIONS);
    SEED.addToOptions(OPTIONS);
    TEST_PROPORTION.addToOptions(OPTIONS);
    DEPTH.addToOptions(OPTIONS);
    STEP.addToOptions(OPTIONS);
    ITERATIONS.addToOptions(OPTIONS);
    TARGET_LOSS.addToOptions(OPTIONS);
    OUTPUT.addToOptions(OPTIONS);
    PRINT_PERIOD.addToOptions(OPTIONS);

    OPTIONS.addOption(VERBOSE);
    OPTIONS.addOption(HELP);
  }

  private FitRankingModelCmd() {
  }

  public static FitRankingModelCmd instance() {
    return INSTANCE;
  }

  private static Pair<? extends Pool, ? extends Pool> createDataPools(CommandLine commandLine) {
    DataBuilderCrossValidation dataBuilderCrossValidation = new DataBuilderCrossValidation();
    dataBuilderCrossValidation.setRandomSeed(SEED.value(commandLine));
    // TODO: set partition from double, not only string
    dataBuilderCrossValidation.setPartition(Double.toString(TEST_PROPORTION.value(commandLine)));

    DataBuilder dataBuilder = dataBuilderCrossValidation;
    dataBuilder.setLearnPath(TRAIN_DATA.value(commandLine).toString());
    // TODO: configure pool reader from command line
    // TODO: determine type of the pool from command line
    dataBuilder.setReader(ReaderFactory.createJsonReader());

    return dataBuilder.create();
  }

  private static VecOptimization createOptimizer(CommandLine commandLine, Pool trainData) {
    GradientBoostingBuilder boostingBuilder = new GradientBoostingBuilder();

    GridBuilder gridBuilder = new GridBuilder();
    gridBuilder.setBinsCount(GRID_BINS.value(commandLine));
    gridBuilder.setDataSet(trainData.vecData());

    GreedyObliviousTreeBuilder weakBuilder = new GreedyObliviousTreeBuilder();
    weakBuilder.setDepth(DEPTH.value(commandLine));
    weakBuilder.setGridBuilder(gridBuilder);

    boostingBuilder.setWeak(weakBuilder.create());
    boostingBuilder.setIterations(ITERATIONS.value(commandLine));
    boostingBuilder.setStep(STEP.value(commandLine));
    boostingBuilder.setLocal(TARGET_LOSS.value(commandLine).targetName());

    return boostingBuilder.create();
  }

  @Override
  public String commandName() {
    return COMMAND_NAME;
  }

  @Override
  public void printHelp() {
    HelpFormatter helpFormatter = new HelpFormatter();
    helpFormatter.printHelp(COMMAND_NAME, OPTIONS);
  }

  @SuppressWarnings("unchecked")
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

    SingleArgOptions
        .checkOptions(commandLine, TRAIN_DATA, SEED, TEST_PROPORTION, DEPTH, STEP, ITERATIONS,
            TARGET_LOSS, OUTPUT, PRINT_PERIOD);

    Path outputPath = OUTPUT.value(commandLine);
    Pair<? extends Pool, ? extends Pool> trainTestSplit = createDataPools(commandLine);
    Pool trainPool = trainTestSplit.getFirst();
    Pool testPool = trainTestSplit.getSecond();
    VecOptimization gbotOptimizer = createOptimizer(commandLine, trainPool);
    TargetFunc target = trainPool.targetByName(TARGET_LOSS.value(commandLine).targetName());

    Func[] testMetrics;
    {
      List<Func> testMetricsTmp = new ArrayList<>();
      testMetricsTmp.add(testPool.targetByName(TARGET_LOSS.value(commandLine).targetName()));
      if (TEST_METRIC.value(commandLine) != Metric.NONE) {
        testMetricsTmp.add(testPool.targetByName(TEST_METRIC.value(commandLine).metricName()));
      }
      testMetrics = new Func[testMetricsTmp.size()];
      testMetricsTmp.toArray(testMetrics);
    }

    DefaultProgressPrinter progressPrinter = null;
    if (commandLine.hasOption(VERBOSE.getOpt())) {
      progressPrinter = new DefaultProgressPrinter(trainPool, testPool, target,
          testMetrics,
          PRINT_PERIOD.value(commandLine));
      ((WeakListenerHolder) gbotOptimizer).addListener(
          progressPrinter);
    }
    // TODO: WTF WHY DO I NEED THERE? (otherwise progressPrinter will be GC-ed?)
    System.out.println(progressPrinter);
    // Start fitting
    Interval.start();
    Interval.suspend();
    Trans result = gbotOptimizer.fit(trainPool.vecData(), target);
    Interval.stopAndPrint("Total fit time:");

    if (TEST_PROPORTION.value(commandLine) > 0) {
      ResultsPrinter.printResults(result, trainPool, testPool, target, testMetrics);
    }

    new ModelWriter(outputPath.toString())
        .writeModel(result, trainPool);
  }

  private enum Target {
    QMSE("GroupedL2"),
    MSE("SatL2");

    private final String targetName;

    Target(String targetName) {
      this.targetName = targetName;
    }

    public String targetName() {
      return targetName;
    }
  }

  private enum Metric {
    NDCG("NormalizedDCG"),
    NONE("");
    private final String metricName;

    Metric(String metricName) {
      this.metricName = metricName;
    }

    public String metricName() {
      return metricName();
    }
  }
}
