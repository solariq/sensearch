package com.expleague.sensearch.cli.commands;

import static com.expleague.sensearch.cli.utils.CommandLineTools.checkOptions;

import com.expleague.sensearch.AppModule;
import com.expleague.sensearch.ConfigImpl;
import com.expleague.sensearch.cli.Command;
import com.expleague.sensearch.cli.utils.SingleArgOptions.PathOption;
import com.expleague.sensearch.cli.utils.SingleArgPredicates.ExistingPath;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.index.Index;
import com.google.inject.Guice;
import java.util.Scanner;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class RunViewEmbeddingCmd implements Command {

  private static final String COMMAND_NAME = "view-embedding";

  private static final PathOption INDEX_PATH = PathOption.builder()
      .shortOption("i")
      .longOption("index")
      .description("Specify path to the index")
      .predicates(ExistingPath.get())
      .build();

  private static final Option HELP = Option.builder("h")
      .longOpt("help")
      .required(false)
      .numberOfArgs(0)
      .build();

  private static final Options OPTIONS = new Options();
  private static final CommandLineParser CLI_PARSER = new DefaultParser();
  private static final RunViewEmbeddingCmd INSTANCE = new RunViewEmbeddingCmd();

  static {
    INDEX_PATH.addToOptions(OPTIONS);
    OPTIONS.addOption(HELP);
  }

  public static RunViewEmbeddingCmd instance() {
    return INSTANCE;
  }

  @Override
  public void run(String... args) throws Exception {
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

    checkOptions(commandLine, INDEX_PATH);

    ConfigImpl config = new ConfigImpl();
    config.setTemporaryIndex(INDEX_PATH.value(commandLine).toString());
    config.setMaxFilterItems(50);

    Index index = Guice.createInjector(new AppModule(config)).getInstance(Index.class);

    System.out.println("Index loaded!");
    System.out.println("Usage: <word> <skip> <take>: print 'take' nearest words to 'word' ignoring first 'skip'");
    Scanner scanner = new Scanner(System.in);
    while (true) {
      System.out.print(">");
      String line = scanner.nextLine();
      String[] tokens = line.split(" ");
      if (tokens.length != 3) {
        System.out.println("Invalid command");
        continue;
      }

      int skip, take;
      try {
        skip = Integer.parseInt(tokens[1]);
        take = Integer.parseInt(tokens[2]);
      } catch (NumberFormatException e) {
        System.out.println("Invalid number");
        continue;
      }
      Term term = index.term(tokens[0]);
      if (term == null) {
        System.out.println("Cannot find term " + tokens[0]);
        continue;
      }

      term.synonymsWithDistance().skip(skip).limit(take).forEach(x -> {
        System.out.println(x.term().text() + "\t" + x.distance());
      });
    }
  }

  @Override
  public void printHelp() {
    HelpFormatter helpFormatter = new HelpFormatter();
    helpFormatter.printHelp(COMMAND_NAME, OPTIONS);

  }

  public static String commandName() {
    return COMMAND_NAME;
  }
}
