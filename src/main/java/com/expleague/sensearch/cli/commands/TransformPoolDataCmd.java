package com.expleague.sensearch.cli.commands;

import com.expleague.sensearch.cli.Command;
import com.expleague.sensearch.cli.utils.SingleArgOptions;
import com.expleague.sensearch.cli.utils.SingleArgOptions.PathOption;
import com.expleague.sensearch.cli.utils.SingleArgPredicates.ExistingPath;
import com.expleague.sensearch.experiments.joom.JoomCsvTransformer;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransformPoolDataCmd implements Command {
  private static final String COMMAND_NAME = "trans-pool-data";

  private static final PathOption SOURCE_POOL = PathOption.builder()
      .shortOption("p")
      .longOption("source-pool")
      .description("Specify path to the pool to be transformed")
      .predicates(ExistingPath.get())
      .build();

  private static final PathOption OUTPUT_PATH = PathOption.builder()
      .shortOption("o")
      .longOption("output")
      .description("Specify output path for the transformed pool")
      .predicates(ExistingPath.negated())
      .build();

  private static final Option HELP = Option.builder("h")
      .longOpt("help")
      .desc("Print help message")
      .numberOfArgs(0)
      .build();

  private static final Logger LOG = LoggerFactory.getLogger(TransformPoolDataCmd.class);
  private static final TransformPoolDataCmd INSTANCE = new TransformPoolDataCmd();
  private static final CommandLineParser CLI_PARSER = new DefaultParser();
  private static final Options OPTIONS = new Options();
  static {
    SOURCE_POOL.addToOptions(OPTIONS);
    OUTPUT_PATH.addToOptions(OPTIONS);
    OPTIONS.addOption(HELP);
  }
  private TransformPoolDataCmd() {}

  public static TransformPoolDataCmd instance() {
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

    SingleArgOptions.checkOptions(commandLine, SOURCE_POOL, OUTPUT_PATH);
    new JoomCsvTransformer().transformData(SOURCE_POOL.value(commandLine),
        OUTPUT_PATH.value(commandLine));
  }

  @Override
  public void printHelp() {
    HelpFormatter helpFormatter = new HelpFormatter();
    helpFormatter.printHelp(COMMAND_NAME, OPTIONS);
  }
}
