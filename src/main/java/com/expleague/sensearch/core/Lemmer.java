package com.expleague.sensearch.core;

import com.expleague.commons.text.lemmer.MyStem;

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
      instance = new Lemmer(myStemPath);
    }
    return instance;
  }
}
