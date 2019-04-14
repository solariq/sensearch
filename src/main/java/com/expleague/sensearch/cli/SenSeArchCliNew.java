package com.expleague.sensearch.cli;

import com.expleague.sensearch.cli.commands.BuildIndexCmd;
import com.expleague.sensearch.cli.commands.FitRankingModelCmd;
import com.expleague.sensearch.cli.commands.RunSearchCmd;
import com.expleague.sensearch.cli.commands.TrainEmbeddingCmd;
import java.util.HashMap;
import java.util.Map;

public class SenSeArchCliNew {

  private static final Map<String, Command> COMMANDS = new HashMap<String, Command>() {
    {
      put(RunSearchCmd.instance().commandName(), RunSearchCmd.instance());
      put(TrainEmbeddingCmd.instance().commandName(), TrainEmbeddingCmd.instance());
      put(BuildIndexCmd.instance().commandName(), BuildIndexCmd.instance());
      put(FitRankingModelCmd.instance().commandName(), FitRankingModelCmd.instance());
    }
  };

  public static void main(String... args) throws Exception {

  }
}
