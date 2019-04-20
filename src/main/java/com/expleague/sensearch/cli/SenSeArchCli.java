package com.expleague.sensearch.cli;

import com.expleague.sensearch.cli.commands.BuildFilterPoolCmd;
import com.expleague.sensearch.cli.commands.BuildIndexCmd;
import com.expleague.sensearch.cli.commands.BuildRankingPoolCmd;
import com.expleague.sensearch.cli.commands.FitRankingModelCmd;
import com.expleague.sensearch.cli.commands.RebuildEmbeddingCmd;
import com.expleague.sensearch.cli.commands.RunSearchCmd;
import com.expleague.sensearch.cli.commands.TrainEmbeddingCmd;
import com.expleague.sensearch.cli.commands.TransformPoolDataCmd;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.log4j.PropertyConfigurator;

public class SenSeArchCli {
  private static final String HELP = "help";

  private static final Map<String, Command> COMMANDS = new HashMap<String, Command>() {
    {
      put(RunSearchCmd.commandName(), RunSearchCmd.instance());
      put(TrainEmbeddingCmd.commandName(), TrainEmbeddingCmd.instance());
      put(BuildIndexCmd.commandName(), BuildIndexCmd.instance());
      put(FitRankingModelCmd.commandName(), FitRankingModelCmd.instance());
      put(BuildFilterPoolCmd.commandName(), BuildFilterPoolCmd.instance());
      put(BuildRankingPoolCmd.commandName(), BuildRankingPoolCmd.instance());
      put(RebuildEmbeddingCmd.commandName(), RebuildEmbeddingCmd.instance());
      put(TransformPoolDataCmd.commandName(), TransformPoolDataCmd.instance());
    }
  };

  public static void main(String... args) throws Exception {
    if (args.length < 1) {
      System.out.println("No arguments provided! Help message will be printed");
      System.out.println("Use 'help' option to print help message");
      System.exit(1);
    }

    String option = args[0];
    if (HELP.equals(option)) {
      printHelp();
      return;
    }

    if (!COMMANDS.containsKey(option)) {
      System.out.printf("Option '%s' was not found in the list of available options!"
          + " Use 'help' to print help message", option);
      System.exit(1);
    }

    Properties logProperties = new Properties();
    logProperties.load(Files.newInputStream(Paths.get("log4j.properties")));
    PropertyConfigurator.configure(logProperties);

    COMMANDS.get(option).run(args);
  }

  private static void printHelp() {
    for (Command command : COMMANDS.values()) {
      command.printHelp();
      System.out.println();
      System.out.println("================================");
    }
  }
}
