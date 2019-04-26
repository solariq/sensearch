package com.expleague.sensearch.cli.utils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

public class CommandLineTools {

  public static void checkOptions(CommandLine commandLine, CheckableOption... options) {
    for (CheckableOption option : options) {
      option.check(commandLine);
    }
  }

  public static String value(CommandLine commandLine, Option option) {
    return option.getOpt() != null ? commandLine.getOptionValue(option.getOpt()) :
        commandLine.getOptionValue(option.getLongOpt());
  }

  public static boolean hasOption(CommandLine commandLine, Option option) {
    return option.getOpt() != null ? commandLine.hasOption(option.getOpt()) :
        commandLine.hasOption(option.getLongOpt());
  }

  public interface CheckableOption {

    void check(CommandLine commandLine);
  }
}
