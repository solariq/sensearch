package com.expleague.sensearch.cli;

import static com.expleague.sensearch.donkey.plain.PlainIndexBuilder.DEFAULT_VEC_SIZE;

import com.expleague.commons.seq.CharSeq;
import com.expleague.ml.embedding.impl.EmbeddingImpl;
import com.expleague.sensearch.AppModule;
import com.expleague.sensearch.ConfigImpl;
import com.expleague.sensearch.donkey.IndexBuilder;
import com.expleague.sensearch.donkey.crawler.CrawlerXML;
import com.expleague.sensearch.donkey.plain.JmllEmbeddingBuilder;
import com.expleague.sensearch.miner.pool.RankingPoolBuilder;
import com.expleague.sensearch.web.SearchServer;
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

public class SenSearchCli {

  private static final String DATA_PATH_OPTION = "data";
  private static final String INDEX_PATH_OPTION = "index";
  // TODO: handle corpus
  private static final String EMBEDDING_PATH_OPTION = "embedding";
  private static final String EMBEDDING_OUTPUT_PATH_OPTION = "decomp";
  private static final String DO_NOT_USE_LSH_OPTION = "nolsh";
  private static final String MAX_FILTER_ITEMS = "maxfilter";
  private static final String RANK_POOL_PATH_OPTION = "rankpool";
  private static final String RANK_MODEL_PATH_OPTION = "rankmodel";

  private static final String TRAIN_EMBEDDING_COMMAND = "trainEmbedding";
  private static final String BUILD_INDEX_COMMAND = "buildIndex";
  private static final String START_SEVER_COMMAND = "startServer";
  private static final String BUILD_RANK_POOL_COMMAND = "buildRankPool";
  private static final String TRAIN_RANK_MODEL_COMMAND = "fitrk";

  private static Options trainEmbeddingOptions = new Options();
  private static Options buildIndexOptions = new Options();
  private static Options startServerOptions = new Options();
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
    buildRankPoolOptions.addOption(indexPathOption);

    Option embeddingOutputPathOption =
        Option.builder()
            .longOpt(EMBEDDING_OUTPUT_PATH_OPTION)
            .desc("path to result embedding vecs")
            .hasArg()
            .required()
            .build();
    trainEmbeddingOptions.addOption(embeddingOutputPathOption);
    buildIndexOptions.addOption(embeddingOutputPathOption);

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
    startServerOptions.addOption(
        Option.builder()
            .longOpt(MAX_FILTER_ITEMS)
            .desc("maximal number of items to be left after filtering")
            .hasArg()
            .required()
            .build());

    Option rankPoolPathOption =
        Option.builder()
            .longOpt(RANK_POOL_PATH_OPTION)
            .desc("path to rank model pool")
            .hasArg()
            .required()
            .build();
    buildRankPoolOptions.addOption(rankPoolPathOption);
  }

  private static final Logger LOG = LoggerFactory.getLogger(SenSearchCli.class);
  public static void main(String[] args) throws Exception {
    if (args.length < 1) {
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
      switch (args[0]) {
        case TRAIN_EMBEDDING_COMMAND:
          parser = cliParser.parse(trainEmbeddingOptions, args);
          try (Writer w = Files.newBufferedWriter(Paths.get(parser.getOptionValue(EMBEDDING_OUTPUT_PATH_OPTION)))) {
            final JmllEmbeddingBuilder embeddingBuilder = new JmllEmbeddingBuilder(
                DEFAULT_VEC_SIZE,
                Paths.get(parser.getOptionValue(EMBEDDING_PATH_OPTION))
            );
            EmbeddingImpl<CharSeq> embedding = (EmbeddingImpl<CharSeq>)embeddingBuilder
                .build(new CrawlerXML(Paths.get(parser.getOptionValue(DATA_PATH_OPTION))).makeStream());
            embedding.write(w);
          }
          break;

        case BUILD_INDEX_COMMAND:
          parser = cliParser.parse(buildIndexOptions, args);
          config.setPathToZIP(parser.getOptionValue(DATA_PATH_OPTION));
          config.setEmbeddingVectors(parser.getOptionValue(EMBEDDING_OUTPUT_PATH_OPTION));
          config.setTemporaryIndex(parser.getOptionValue(INDEX_PATH_OPTION));

          Guice.createInjector(new AppModule(config)).getInstance(IndexBuilder.class).buildIndex();
          break;

        case START_SEVER_COMMAND:
          parser = cliParser.parse(startServerOptions, args);
          config.setTemporaryIndex(parser.getOptionValue(INDEX_PATH_OPTION));
          config.setLshNearestFlag(!parser.hasOption(DO_NOT_USE_LSH_OPTION));
          config.setMaxFilterItems(Integer.parseInt(parser.getOptionValue(MAX_FILTER_ITEMS)));
          config.setModelPath(parser.getOptionValue(RANK_MODEL_PATH_OPTION));

          Injector injector = Guice.createInjector(new AppModule(config));
          injector.getInstance(SearchServer.class).start(injector);
          break;

        case BUILD_RANK_POOL_COMMAND:
          parser = cliParser.parse(buildRankPoolOptions, args);
          config.setTemporaryIndex(parser.getOptionValue(INDEX_PATH_OPTION));
          Path poolPath = Paths.get(parser.getOptionValue(RANK_POOL_PATH_OPTION));
          Guice.createInjector(new AppModule(config))
              .getInstance(RankingPoolBuilder.class)
              .build(poolPath);
          break;

        case TRAIN_RANK_MODEL_COMMAND:
          LOG.debug("Executing command 'fitrk'");
          FitRankingCli.run(args);
          //          JMLLCLI.main(
//              new String[]{
//                  "fit",
//                  "--print-period",
//                  "10",
//                  "--json-format",
//                  "--learn",
//                  parse.getOptionValue(RANK_POOL_PATH_OPTION),
//                  "-X",
//                  "1/0.7",
//                  "-v",
//                  "-O",
//                  "'GradientBoosting(local=SatL2, weak=GreedyObliviousTree(depth=6), step=0.01, iterations=600)'"
//              });
//          Files.move(
//              Paths.get(parser.getOptionValue(RANK_POOL_PATH_OPTION) + ".model"),
//              Paths.get(parser.getOptionValue(RANK_MODEL_PATH_OPTION)));
      }
    } catch (ParseException e) {
      System.out.println(e.getLocalizedMessage());
      printUsage();
    }

    //    cliParser.parse(startServerOptions, args).getOptionValue()
    //    System.out.println(options.);
  }

  private static void printUsage() {
    printUsage(TRAIN_EMBEDDING_COMMAND);
    printUsage(BUILD_INDEX_COMMAND);
    printUsage(START_SEVER_COMMAND);
    printUsage(BUILD_RANK_POOL_COMMAND);
    printUsage(TRAIN_RANK_MODEL_COMMAND);
  }

  private static Options getCommandOptions(String command) {
    switch (command) {
      case TRAIN_EMBEDDING_COMMAND:
        return trainEmbeddingOptions;
      case BUILD_INDEX_COMMAND:
        return buildIndexOptions;
      case START_SEVER_COMMAND:
        return startServerOptions;
      case BUILD_RANK_POOL_COMMAND:
        return buildRankPoolOptions;
      case TRAIN_RANK_MODEL_COMMAND:
        return null;
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
      case START_SEVER_COMMAND:
        return "Starts server. Requires index to be built";
      case BUILD_RANK_POOL_COMMAND:
        return "Collects pool for ranking model training. Requires index to be built";
      case TRAIN_RANK_MODEL_COMMAND:
        return "Trains ranking model. Requires rank pool to be collected";
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
