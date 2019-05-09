package com.expleague.sensearch.web.suggest;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import com.expleague.sensearch.core.Term;

public class MultigramWrapper {
  public final Term[] phrase;
  public final double coeff;
  public MultigramWrapper(@NotNull Term[] phrase, double coeff) {
    this.phrase = phrase;
    this.coeff = coeff;
  }
  
  @Override
  public String toString() {
    return Arrays.stream(phrase)
        .map(t -> t.text())
        .collect(Collectors.joining(" "));
  }
}
