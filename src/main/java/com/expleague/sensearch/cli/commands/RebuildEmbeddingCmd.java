package com.expleague.sensearch.cli.commands;

import static com.expleague.sensearch.cli.utils.CommandLineTools.checkOptions;

import com.expleague.sensearch.AppModule;
import com.expleague.sensearch.ConfigImpl;
import com.expleague.sensearch.RebuildEmbedding;
import com.expleague.sensearch.cli.Command;
import com.expleague.sensearch.cli.utils.SingleArgOptions;
import com.expleague.sensearch.cli.utils.SingleArgOptions.PathOption;
import com.expleague.sensearch.cli.utils.SingleArgPredicates.ExistingPath;
import com.google.inject.Guice;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RebuildEmbeddingCmd implements Command {
  private static final String COMMAND_NAME = "rebuild-emb";

  private static final PathOption INDEX_PATH = PathOption.builder()
      .shortOption("i")
      .longOption("index-path")
      .description("Specify path to the index")
      .predicates(ExistingPath.get())
      .build();

  private static final PathOption EMBEDDING_VECTORS = PathOption.builder()
      .shortOption("e")
      .longOption("emb-vec")
      .description("Specify path to embedding vectors")
      .predicates(ExistingPath.get())
      .build();

  private static final Option HELP = Option.builder("h")
      .longOpt("help")
      .desc("Print help message")
      .numberOfArgs(0)
      .build();

  private static final Logger LOG = LoggerFactory.getLogger(RebuildEmbeddingCmd.class);
  private static final CommandLineParser CLI_PARSER = new DefaultParser();
  private static final Options OPTIONS = new Options();
  static {
    INDEX_PATH.addToOptions(OPTIONS);
    EMBEDDING_VECTORS.addToOptions(OPTIONS);
    OPTIONS.addOption(HELP);
  }
  private static final RebuildEmbeddingCmd INSTANCE = new RebuildEmbeddingCmd();
  private RebuildEmbeddingCmd() {}

  public static RebuildEmbeddingCmd instance() {
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
    } catch (ParseException e) {
      printHelp();
      return;
    }

    if (commandLine.hasOption(HELP.getOpt())) {
      printHelp();
      return;
    }

    checkOptions(commandLine, INDEX_PATH, EMBEDDING_VECTORS);

    ConfigImpl config = new ConfigImpl();
    config.setTemporaryIndex(INDEX_PATH.value(commandLine).toString());
    config.setEmbeddingVectors(EMBEDDING_VECTORS.value(commandLine).toString());

    Guice.createInjector(new AppModule(config)).getInstance(RebuildEmbedding.class).rebuild();
  }

  @Override
  public void printHelp() {
    HelpFormatter helpFormatter = new HelpFormatter();
    helpFormatter.printHelp(COMMAND_NAME, OPTIONS);
  }
}
