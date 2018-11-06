package com.expleague.sensearch.core;

import com.expleague.commons.text.lemmer.MyStem;
import java.nio.file.Path;

public class Lemmer {

  public MyStem myStem;

  public Lemmer(Path myStemPath) {
    this.myStem = new MyStem(myStemPath);
  }

}
