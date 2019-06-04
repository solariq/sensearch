package com.expleague.sensearch.donkey.plain;

import com.expleague.sensearch.donkey.StreamPage;
import gnu.trove.list.TIntList;
import java.util.Arrays;
import java.util.stream.IntStream;

public class PlainStreamPage implements StreamPage {

  private TIntList page;

  public PlainStreamPage(TIntList page) {
    this.page = page;
  }

  @Override
  public IntStream stream() {
    return Arrays.stream(page.toArray());
  }
}
