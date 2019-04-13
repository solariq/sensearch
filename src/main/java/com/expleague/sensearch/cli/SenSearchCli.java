package com.expleague.sensearch.cli;

import static com.expleague.sensearch.donkey.plain.PlainIndexBuilder.DEFAULT_VEC_SIZE;

import com.expleague.commons.seq.CharSeq;
import com.expleague.ml.embedding.impl.EmbeddingImpl;
import com.expleague.sensearch.AppModule;
import com.expleague.sensearch.ConfigImpl;
import com.expleague.sensearch.RebuildEmbedding;
import com.expleague.sensearch.donkey.IndexBuilder;
import com.expleague.sensearch.donkey.crawler.Crawler;
import com.expleague.sensearch.donkey.plain.JmllEmbeddingBuilder;
import com.expleague.sensearch.experiments.joom.CrawlerJoom;
import com.expleague.sensearch.experiments.joom.JoomCsvTransformer;
import com.expleague.sensearch.experiments.wiki.CrawlerWiki;
import com.expleague.sensearch.miner.pool.builders.FilterPoolBuilder;
import com.expleague.sensearch.miner.pool.builders.RankingPoolBuilder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.File;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO:
// - filter pool builder
// - filter model train
// - fix ranking pool builder/model train
// - fix printing usage
public class SenSearchCli {

  private static final String DATA_PATH_OPTION = "data";
  private static final String INDEX_PATH_OPTION = "index";
  // TODO: handle corpus
  private static final String EMBEDDING_PATH_OPTION = "embedding";
  private static final String EMBEDDING_OUTPUT_PATH_OPTION = "decomp";
  private static final String DO_NOT_USE_LSH_OPTION = "nolsh";
  private static final String MAX_FILTER_ITEMS = "maxfilter";
  private static final String POOL_PATH_OPTION = "pool";
  private static final String RAW_POOL_PATH_OPTION = "rawPool";

  private static final String RANK_MODEL_PATH_OPTION = "rankmodel";
  private static final String FILTER_MODEL_PATH_OPTION = "filtermodel";

  private static final String POOL_ITERATION_OPTION = "poolIteration";

  private static final String TRAIN_EMBEDDING_COMMAND = "trainEmbedding";

  private static final String REBUILD_EMBEDDING_COMMAND = "rebuildEmbeddingCommand";

  private static final String DATA_FORMAT_OPTION = "dataFormat";
  private static final String WIKI_FORMAT = "wiki";
  private static final String JOOM_FORMAT = "joom";

  private static final String BUILD_INDEX_COMMAND = "buildIndex";
  private static final String START_SERVER_COMMAND = "srch";

  private static final String TRANSFORM_POOL_DATA_COMMAND = "poolTransform";

  private static final String BUILD_FILTER_POOL_COMMAND = "buildFilterPool";
  private static final String BUILD_RANK_POOL_COMMAND = "buildRankPool";
  private static final String TRAIN_RANK_MODEL_COMMAND = "fitrk";

  private static Options trainEmbeddingOptions = new Options();

  private static Options rebuildEmbeddingOptions = new Options();

  private static Options buildIndexOptions = new Options();
  private static Options startServerOptions = new Options();

  private static Options transformDataOptions = new Options();

  private static Options buildFilterPoolOptions = new Options();
  private static Options buildRankPoolOptions = new Options();

  static {
    Option dataOption =
        Option.builder()
            .longOpt(DATA_PATH_OPTION)
            .desc("path to zipped data")
            .hasArg()
            .required()
            .build();

    trainEmbeddingOptions.addOption(dataOption);
    buildIndexOptions.addOption(dataOption);

    Option indexPathOption =
        Option.builder()
            .longOpt(INDEX_PATH_OPTION)
            .desc("path to index")
            .hasArg()
            .required()
            .build();
    buildIndexOptions.addOption(indexPathOption);
    startServerOptions.addOption(indexPathOption);
    buildFilterPoolOptions.addOption(indexPathOption);
    buildRankPoolOptions.addOption(indexPathOption);
    rebuildEmbeddingOptions.addOption(indexPathOption);

    Option rawPoolPathOption =
        Option.builder()
            .longOpt(RAW_POOL_PATH_OPTION)
            .desc("Path to unformatted data")
            .hasArg()
            .required()
            .build();
    transformDataOptions.addOption(rawPoolPathOption);

    Option dataFormatOption =
        Option.builder()
            .longOpt(DATA_FORMAT_OPTION)
            .desc("Input file format")
            .hasArg()
            .required()
            .build();
    buildIndexOptions.addOption(dataFormatOption);

    Option embeddingOutputPathOption =
        Option.builder()
            .longOpt(EMBEDDING_OUTPUT_PATH_OPTION)
            .desc("path to result embedding vecs")
            .hasArg()
            .required()
            .build();
    trainEmbeddingOptions.addOption(embeddingOutputPathOption);
    buildIndexOptions.addOption(embeddingOutputPathOption);
    rebuildEmbeddingOptions.addOption(embeddingOutputPathOption);

    Option embeddingPathOption =
        Option.builder()
            .longOpt(EMBEDDING_PATH_OPTION)
            .desc("path to build embedding vectors")
            .hasArg()
            .required()
            .build();
    trainEmbeddingOptions.addOption(embeddingPathOption);

    startServerOptions.addOption(
        Option.builder().longOpt(DO_NOT_USE_LSH_OPTION).desc("disables LSH").build());

    Option poolIterationOption =
        Option.builder()
            .longOpt(POOL_ITERATION_OPTION)
            .desc("Pool iteration")
            .hasArg()
            .required()
            .build();
    buildFilterPoolOptions.addOption(poolIterationOption);
    buildRankPoolOptions.addOption(poolIterationOption);

    Option maxFilterOption =
        Option.builder()
            .longOpt(MAX_FILTER_ITEMS)
            .desc("maximal number of items to be left after filtering")
            .hasArg()
            .required()
            .build();
    startServerOptions.addOption(maxFilterOption);
    buildFilterPoolOptions.addOption(maxFilterOption);
    buildRankPoolOptions.addOption(maxFilterOption);

    Option poolPathOption =
        Option.builder()
            .longOpt(POOL_PATH_OPTION)
            .desc("path to model pool")
            .hasArg()
            .required()
            .build();

    buildFilterPoolOptions.addOption(poolPathOption);
    buildRankPoolOptions.addOption(poolPathOption);
    transformDataOptions.addOption(poolPathOption);

    Option rankModelPathOption =
        Option.builder()
            .longOpt(RANK_MODEL_PATH_OPTION)
            .desc("path to rank model")
            .hasArg()
            .required()
            .build();
    startServerOptions.addOption(rankModelPathOption);
    buildRankPoolOptions.addOption(rankModelPathOption);

    Option filterModelPathOption =
        Option.builder()
            .longOpt(FILTER_MODEL_PATH_OPTION)
            .desc("path to filter model")
            .hasArg()
            .required()
            .build();
    startServerOptions.addOption(filterModelPathOption);
    buildFilterPoolOptions.addOption(filterModelPathOption);
    buildRankPoolOptions.addOption(filterModelPathOption);
  }

  private static final Logger LOG = LoggerFactory.getLogger(SenSearchCli.class);

  public static void main(String[] args) throws Exception {
    if (args.length < 1) {
      System.out.println("No arguments provided! Help message will be printed");
      printUsage();
      return;
    }

    Properties logProperties = new Properties();
    logProperties.load(Files.newInputStream(Paths.get("log4j.properties")));
    PropertyConfigurator.configure(logProperties);

    CommandLineParser cliParser = new DefaultParser();
    try {
      CommandLine parser;
      ConfigImpl config = new ConfigImpl();
      Injector injector;
      switch (args[0]) {
        case TRAIN_EMBEDDING_COMMAND:
          parser = cliParser.parse(trainEmbeddingOptions, args);
          try (Writer w =
              Files.newBufferedWriter(
                  Paths.get(parser.getOptionValue(EMBEDDING_OUTPUT_PATH_OPTION)))) {
            final JmllEmbeddingBuilder embeddingBuilder =
                new JmllEmbeddingBuilder(
                    DEFAULT_VEC_SIZE, Paths.get(parser.getOptionValue(EMBEDDING_PATH_OPTION)));
            EmbeddingImpl<CharSeq> embedding =
                (EmbeddingImpl<CharSeq>)
                    embeddingBuilder.build(
                        new CrawlerWiki(Paths.get(parser.getOptionValue(DATA_PATH_OPTION)))
                            .makeStream());
            embedding.write(w);
          }
          break;

        case BUILD_INDEX_COMMAND:
          parser = cliParser.parse(buildIndexOptions, args);
          config.setPathToZIP(parser.getOptionValue(DATA_PATH_OPTION));
          config.setEmbeddingVectors(parser.getOptionValue(EMBEDDING_OUTPUT_PATH_OPTION));
          config.setTemporaryIndex(parser.getOptionValue(INDEX_PATH_OPTION));

          Class<? extends Crawler> crawlerClass;
          switch (parser.getOptionValue(DATA_FORMAT_OPTION)) {
            case WIKI_FORMAT:
              crawlerClass = CrawlerWiki.class;
              break;
            case JOOM_FORMAT:
              crawlerClass = CrawlerJoom.class;
          }

          Guice.createInjector(new AppModule(config)).getInstance(IndexBuilder.class).buildIndex();
          break;

        case START_SERVER_COMMAND:
          RunSearchCli.run(args);
          break;

        case BUILD_FILTER_POOL_COMMAND:
          parser = cliParser.parse(buildFilterPoolOptions, args);
          config.setTemporaryIndex(parser.getOptionValue(INDEX_PATH_OPTION));

          config.setMaxFilterItems(Integer.parseInt(parser.getOptionValue(MAX_FILTER_ITEMS)));

          if (parser.hasOption(FILTER_MODEL_PATH_OPTION)) {
            config.setModelFilterPath(parser.getOptionValue(FILTER_MODEL_PATH_OPTION));
          }

          Path poolPath = Paths.get(parser.getOptionValue(POOL_PATH_OPTION));
          int iteration = Integer.parseInt(parser.getOptionValue(POOL_ITERATION_OPTION));

          Guice.createInjector(new AppModule(config))
              .getInstance(FilterPoolBuilder.class)
              .build(poolPath, iteration);
          break;

        case BUILD_RANK_POOL_COMMAND:
          parser = cliParser.parse(buildRankPoolOptions, args);
          config.setTemporaryIndex(parser.getOptionValue(INDEX_PATH_OPTION));

          config.setMaxFilterItems(Integer.parseInt(parser.getOptionValue(MAX_FILTER_ITEMS)));

          if (parser.hasOption(FILTER_MODEL_PATH_OPTION)) {
            config.setModelFilterPath(parser.getOptionValue(FILTER_MODEL_PATH_OPTION));
          }
          if (parser.hasOption(RANK_MODEL_PATH_OPTION)) {
            config.setModelPath(parser.getOptionValue(RANK_MODEL_PATH_OPTION));
          }

          poolPath = Paths.get(parser.getOptionValue(POOL_PATH_OPTION));
          Guice.createInjector(new AppModule(config))
              .getInstance(RankingPoolBuilder.class)
              .build(poolPath, Integer.parseInt(parser.getOptionValue(POOL_ITERATION_OPTION)));
          break;
        case TRAIN_RANK_MODEL_COMMAND:
          LOG.debug("Executing command 'fitrk'");
          FitRankingCli.run(args);
          break;

        case REBUILD_EMBEDDING_COMMAND:
          parser = cliParser.parse(rebuildEmbeddingOptions, args);
          config.setTemporaryIndex(parser.getOptionValue(INDEX_PATH_OPTION));
          config.setEmbeddingVectors(parser.getOptionValue(EMBEDDING_OUTPUT_PATH_OPTION));

          injector = Guice.createInjector(new AppModule(config));
          RebuildEmbedding rebuildEmbedding = injector.getInstance(RebuildEmbedding.class);
          rebuildEmbedding.rebuild();
          break;

        case TRANSFORM_POOL_DATA_COMMAND:
          parser = cliParser.parse(transformDataOptions, args);

          Path originalDataPath = Paths.get(parser.getOptionValue(RAW_POOL_PATH_OPTION));
          Path dataPath = Paths.get(parser.getOptionValue(POOL_PATH_OPTION));
          new JoomCsvTransformer().transformData(originalDataPath, dataPath);
      }
    } catch (ParseException e) {
      System.out.println(e.getLocalizedMessage());
      printUsage(args[0]);
    }
  }

  private static void printUsage() {
    printUsage(TRAIN_EMBEDDING_COMMAND);
    printUsage(BUILD_INDEX_COMMAND);
    printUsage(BUILD_FILTER_POOL_COMMAND);
    printUsage(BUILD_RANK_POOL_COMMAND);
    printUsage(TRANSFORM_POOL_DATA_COMMAND);
    FitRankingCli.printUsage();
    RunSearchCli.printUsage();
  }

  private static Options getCommandOptions(String command) {
    switch (command) {
      case TRAIN_EMBEDDING_COMMAND:
        return trainEmbeddingOptions;
      case BUILD_INDEX_COMMAND:
        return buildIndexOptions;
      case BUILD_FILTER_POOL_COMMAND:
        return buildFilterPoolOptions;
      case BUILD_RANK_POOL_COMMAND:
        return buildRankPoolOptions;
      default:
        System.out.println("Unknown command " + command);
        printUsage();
        System.exit(1);
    }
    return null;
  }

  private static String getCommandDescription(String command) {
    switch (command) {
      case TRAIN_EMBEDDING_COMMAND:
        return "Trains embedding for given data.";
      case BUILD_INDEX_COMMAND:
        return "Builds index. Requires embedding to be already trained";
      case BUILD_FILTER_POOL_COMMAND:
        return "Collects pool for filter model training. Requires index to be built";
      case BUILD_RANK_POOL_COMMAND:
        return "Collects pool for ranking model training. Requires index to be built";
      case TRAIN_RANK_MODEL_COMMAND:
        return "Trains ranking model. Requires rank pool to be collected";
      case TRANSFORM_POOL_DATA_COMMAND:
        return "Transforms Joom json data to format compatible with pool buidlers";
      default:
        System.out.println("Unknown command " + command);
        printUsage();
        System.exit(1);
    }
    return null;
  }

  private static void printUsage(String command) {
    Options options = getCommandOptions(command);
    String commandDescription = getCommandDescription(command);

    String jarName =
        new File(SenSearchCli.class.getProtectionDomain().getCodeSource().getLocation().getPath())
            .getName();

    HelpFormatter helpFormatter = new HelpFormatter();
    helpFormatter.printHelp(
        "java -jar " + jarName + " " + command, commandDescription, options, "=======");
  }
}
