package com.expleague.sensearch.cli.commands;

import com.expleague.sensearch.AppModule;
import com.expleague.sensearch.ConfigImpl;
import com.expleague.sensearch.cli.Command;
import com.expleague.sensearch.cli.utils.SingleArgOptions;
import com.expleague.sensearch.cli.utils.SingleArgOptions.EnumOption;
import com.expleague.sensearch.cli.utils.SingleArgOptions.PathOption;
import com.expleague.sensearch.cli.utils.SingleArgPredicates.ExistingPath;
import com.expleague.sensearch.donkey.IndexBuilder;
import com.expleague.sensearch.donkey.crawler.Crawler;
import com.expleague.sensearch.experiments.joom.CrawlerJoom;
import com.expleague.sensearch.experiments.wiki.CrawlerWiki;
import com.google.inject.Guice;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class BuildIndexCmd implements Command {

  private static final String COMMAND_NAME = "build-idx";

  private static final PathOption DATA_PATH = PathOption.builder()
      .shortOption("d")
      .longOption("data")
      .description("Path to the data for which index will be built")
      .predicates(ExistingPath.get())
      .build();
  private static final PathOption EMBEDDING_PATH = PathOption.builder()
      .shortOption("e")
      .longOption("embeddings")
      .defaultValue("Path to built embeddings")
      .predicates(ExistingPath.get())
      .build();
  private static final EnumOption<DataCrawler> DATA_CRAWLER = EnumOption.builder(DataCrawler.class)
      .shortOption("c")
      .longOption("crawler")
      .description("Specify crawler for the data")
      .build();
  private static final PathOption OUTPUT_PATH = PathOption.builder()
      .shortOption("o")
      .longOption("output")
      .defaultValue("Specify output path for an index")
      .predicates(ExistingPath.negated())
      .build();

  private static final Option HELP = Option.builder()
      .argName("h")
      .longOpt("help")
      .desc("Print help message for the option")
      .required(false)
      .numberOfArgs(0)
      .build();

  private static final CommandLineParser CLI_PARSER = new DefaultParser();
  private static final Options OPTIONS = new Options();
  private static final BuildIndexCmd INSTANCE = new BuildIndexCmd();

  static {
    DATA_PATH.addToOptions(OPTIONS);
    EMBEDDING_PATH.addToOptions(OPTIONS);
    DATA_CRAWLER.addToOptions(OPTIONS);
    OUTPUT_PATH.addToOptions(OPTIONS);

    OPTIONS.addOption(HELP);
  }

  private BuildIndexCmd() {
  }

  public static BuildIndexCmd instance() {
    return INSTANCE;
  }

  @Override
  public void run(String... args) throws Exception {
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

    SingleArgOptions.checkOptions(commandLine, DATA_PATH, DATA_CRAWLER,
        EMBEDDING_PATH, OUTPUT_PATH);

    ConfigImpl config = new ConfigImpl();
    config.setPathToZIP(DATA_PATH.value(commandLine).toString());
    config.setEmbeddingVectors(EMBEDDING_PATH.value(commandLine).toString());
    config.setTemporaryIndex(OUTPUT_PATH.value(commandLine).toString());
    Guice.createInjector(new AppModule(config, DATA_CRAWLER.value(commandLine).crawlerClass()))
        .getInstance(IndexBuilder.class)
        .buildIndex();
  }

  @Override
  public void printUsage() {
    HelpFormatter helpFormatter = new HelpFormatter();
    helpFormatter.printHelp(COMMAND_NAME, OPTIONS);
  }

  @Override
  public String commandName() {
    return COMMAND_NAME;
  }

  private enum DataCrawler {
    WIKI(CrawlerWiki.class),
    JOOM(CrawlerJoom.class);

    private final Class<? extends Crawler> crawlerClass;

    DataCrawler(Class<? extends Crawler> crawlerClass) {
      this.crawlerClass = crawlerClass;
    }

    Class<? extends Crawler> crawlerClass() {
      return crawlerClass;
    }
  }
}
