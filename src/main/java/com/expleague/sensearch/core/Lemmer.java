package com.expleague.sensearch.core;

import com.expleague.commons.text.lemmer.MyStem;
import com.expleague.sensearch.Config;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Lemmer {

  private static Lemmer instance;

  public MyStem myStem;
  private static Path myStemPath;

  private Lemmer(Path myStemPath) {
    Lemmer.myStemPath = myStemPath;
    this.myStem = new MyStem(myStemPath);
  }

  private Lemmer(String path) {
    this.myStem = new MyStem(Paths.get(path));
  }

  public static synchronized Lemmer getInstance() {
    if (instance == null) {
      // TODO(tehnar): REFACTOR THIS
      // Making Lemmer @Singleton and @Inject-ing Config right now is a bit hard as we'll have to propagate
      // changes along all the codebase up to refactoring SearchPhase
      instance = new Lemmer(Paths.get("./resources/mystem"));
    }
    return instance;
  }
}
