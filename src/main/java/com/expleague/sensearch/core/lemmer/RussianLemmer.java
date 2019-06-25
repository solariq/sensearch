package com.expleague.sensearch.core.lemmer;

import com.expleague.commons.text.lemmer.MyStem;
import com.expleague.commons.text.lemmer.MyStemImpl;
import com.expleague.commons.text.lemmer.WordInfo;

import java.nio.file.Paths;

public class RussianLemmer implements Lemmer {

  private MyStem myStem;

  RussianLemmer() {
    this.myStem = new MyStemImpl(Paths.get("./resources/mystem"));
  }

  @Override
  public WordInfo parse(CharSequence cs) {
    return myStem.parse(cs).get(0);
  }
}
