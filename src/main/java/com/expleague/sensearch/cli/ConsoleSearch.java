package com.expleague.sensearch.cli;

import com.expleague.sensearch.cli.utils.SingleArgOptions.StringOption;
import org.apache.commons.cli.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleSearch {
  private static final Logger LOG = LoggerFactory.getLogger(ConsoleSearch.class);
  private static final StringOption INPUT = new StringOption(Option.builder("i")
      .longOpt("input")
      .desc("Specify input file from where queries will be read."
          + " If no option was provided, you will be able to pass arguments via console")
      .numberOfArgs(1)
      .build()
  );
  private static final StringOption OUTPUT = new StringOption(Option.builder("o")
      .longOpt("output")
  );
}
