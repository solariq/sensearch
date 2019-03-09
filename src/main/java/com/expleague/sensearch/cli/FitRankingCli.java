package com.expleague.sensearch.cli;

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
import com.expleague.ml.data.tools.Pool;
import com.expleague.ml.methods.VecOptimization;
import com.expleague.sensearch.cli.utils.SingleArgOptions.DoubleOption;
import com.expleague.sensearch.cli.utils.SingleArgOptions.EnumOption;
import com.expleague.sensearch.cli.utils.SingleArgOptions.IntOption;
import com.expleague.sensearch.cli.utils.SingleArgOptions.LongOption;
import com.expleague.sensearch.cli.utils.SingleArgOptions.PathOption;
import com.expleague.sensearch.cli.utils.SingleArgOptions.StringOption;
import com.expleague.sensearch.cli.utils.SingleArgPredicates.ExistingPath;
import com.expleague.sensearch.cli.utils.SingleArgPredicates.NegativeLong;
import com.expleague.sensearch.cli.utils.SingleArgPredicates.PositiveDouble;
import com.expleague.sensearch.cli.utils.SingleArgPredicates.PositiveInteger;
import com.expleague.sensearch.cli.utils.SingleArgPredicates.SegmentDouble;
import java.nio.file.Path;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FitRankingCli {

  private static final Logger LOG = LoggerFactory.getLogger(FitRankingCli.class);
  private static final String MODE_DESCRIPTION = "";

  private enum Target {
    SAT_L2("SatL2");

    private final String targetName;

    Target(String targetName) {
      this.targetName = targetName;
    }

    public String targetName() {
      return targetName;
    }
  }

  private static final PathOption TRAIN_DATA = new PathOption(Option.builder("D")
      .longOpt("data")
      .desc("Specify path to the train data")
      .numberOfArgs(1)
      .build(), ExistingPath.get());

  private static final LongOption SEED = new LongOption(Option.builder("r")
      .longOpt("random-seed")
      .desc("Specify random seed to make results reproducible")
      .numberOfArgs(1)
      .build(), 42, NegativeLong.get());

  private static final DoubleOption CROSS_VALIDATION = new DoubleOption(Option.builder("c")
      .longOpt("cross-val")
      .desc(
          "Specify proportion of the data that will be used for the testing, the rest will be used for training."
              + " Should be the number from (0; 1). Usually, 0.2 is enough."
              + " If no value is given all data will be used for training")
      .numberOfArgs(1)
      .build(), 0, new SegmentDouble(0, 1));

  // gradient boosting on oblivious trees specific arguments (L1, L2 ?)
  // also option for target function should be added
  private static final IntOption DEPTH = new IntOption(Option.builder("d")
      .longOpt("max-depth")
      .desc(
          "Specify maximum depth of a tree in the ensemble. Default value is 5. Usually the depth should not be more than 15")
      .numberOfArgs(1)
      .build(), 5, PositiveInteger.get());
  private static final DoubleOption STEP = new DoubleOption(Option.builder("s")
      .longOpt("step")
      .desc(
          "Specify shrinkage coefficient for single step. The less value is the more iterations is needed to fit a data")
      .numberOfArgs(1)
      .build(), 0.01, PositiveDouble.get());
  private static final IntOption ITERATIONS = new IntOption(Option.builder("i")
      .longOpt("iterations")
      .desc("Specify count of trees in the ensemble, i.e. iterations of the algorithm."
          + " This value depends on the value of the step, the more step is the less iterations is needed")
      .numberOfArgs(1)
      .build(), 100, PositiveInteger.get());
  private static final IntOption GRID_BINS = new IntOption(Option.builder("g")
      .longOpt("grid-bins")
      .desc("Specify count of grid bins")
      .numberOfArgs(1)
      .build(), 32, PositiveInteger.get()
  );
  private static final EnumOption<Target> TARGET = new EnumOption<>(Option.builder("t")
      .longOpt("target")
      .desc("Specify target function which will be optimized")
      .numberOfArgs(1)
      .build(), Target.SAT_L2, Target.class
  );

  // output option
  private static final PathOption OUTPUT = new PathOption(Option.builder("o")
      .longOpt("output")
      .desc("Path to the output file. Execution will be terminated if output file already exists")
      .numberOfArgs(1)
      .build(), ExistingPath.negated());

  private static final Option VERBOSE = Option.builder("v")
      .longOpt("verbose")
      .desc("Make process more verbose")
      .numberOfArgs(0)
      .build();
  private static final IntOption PRINT_PERIOD = new IntOption(Option.builder("p")
      .longOpt("print-period")
      .desc("Print period in iterations")
      .build(), 10, PositiveInteger.get()
  );
  private static final StringOption LOG_FILE = new StringOption(Option.builder("l")
      .longOpt("log-file")
      .desc("Specify log file to which all messages will be written. If no file is given then will"
          + " be used standard output")
      .numberOfArgs(1)
      .build());

  private static final Option HELP = Option.builder("h").
      longOpt("help")
      .desc("Show help message")
      .numberOfArgs(0)
      .build();

  private static final Options OPTIONS = new Options();

  static {
    TRAIN_DATA.addToOptions(OPTIONS);
    SEED.addToOptions(OPTIONS);
    CROSS_VALIDATION.addToOptions(OPTIONS);
    DEPTH.addToOptions(OPTIONS);
    STEP.addToOptions(OPTIONS);
    ITERATIONS.addToOptions(OPTIONS);
    TARGET.addToOptions(OPTIONS);
    OUTPUT.addToOptions(OPTIONS);
    OPTIONS.addOption(VERBOSE);
    OPTIONS.addOption(HELP);
  }

  private static final CommandLineParser CLI_PARSER = new DefaultParser();

  @SuppressWarnings("unchecked")
  static void run(String[] args) throws Exception {
    CommandLine commandLine;
    try {
      commandLine = CLI_PARSER.parse(OPTIONS, args);
    } catch (Exception e) {
      HelpFormatter helpFormatter = new HelpFormatter();
      helpFormatter.printHelp("fitrk", OPTIONS);
      return;
    }

    Path outputPath = OUTPUT.value(commandLine);
    Pair<? extends Pool, ? extends Pool> trainTestSplit = createDataPools(commandLine);
    Pool trainPool = trainTestSplit.getFirst();
    Pool testPool = trainTestSplit.getSecond();
    VecOptimization gbotOptimizer = createOptimizer(commandLine, trainPool);
    TargetFunc target = trainPool.targetByName(TARGET.value(commandLine).targetName());
    // TODO: configure metrics from cmd line
    Func[] testMetrics = {testPool.targetByName(TARGET.value(commandLine).targetName)};
    if (commandLine.hasOption(VERBOSE.getOpt())) {
      ((WeakListenerHolder) gbotOptimizer).addListener(
          new DefaultProgressPrinter(trainPool, testPool, target, testMetrics,
              PRINT_PERIOD.value(commandLine)));
    }

    // Start fitting
    Interval.start();
    Interval.suspend();
    Trans result = gbotOptimizer.fit(trainPool.vecData(), target);
    Interval.stopAndPrint("Total fit time:");

    new ModelWriter(outputPath.toString())
        .writeModel(result, trainPool);
  }

  private static Pair<? extends Pool, ? extends Pool> createDataPools(CommandLine commandLine) {
    DataBuilderCrossValidation dataBuilderCrossValidation = new DataBuilderCrossValidation();
    dataBuilderCrossValidation.setRandomSeed(SEED.value(commandLine));
    // TODO: set partition from double, not only string
    dataBuilderCrossValidation.setPartition(Double.toString(CROSS_VALIDATION.value(commandLine)));

    DataBuilder dataBuilder = dataBuilderCrossValidation;
    dataBuilder.setLearnPath(TRAIN_DATA.value(commandLine).toString());
    // TODO: configure pool reader from command line
    dataBuilder.setReader(ReaderFactory.createFeatureTxtReader());

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
    boostingBuilder.setLocal(TARGET.value(commandLine).targetName());

    return boostingBuilder.create();
  }
}
