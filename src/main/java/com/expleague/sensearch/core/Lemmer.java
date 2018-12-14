package com.expleague.sensearch.core;

import com.expleague.commons.text.lemmer.MyStem;
import com.expleague.commons.text.lemmer.MyStemImpl;
import com.google.inject.Singleton;
import java.nio.file.Path;


@Singleton
public class Lemmer {

  public MyStem myStem;

  public Lemmer(Path myStemPath) {
    this.myStem = new MyStemImpl(myStemPath);
  }

  public Lemmer(MyStem myStem) {
    this.myStem = myStem;
  }
}
