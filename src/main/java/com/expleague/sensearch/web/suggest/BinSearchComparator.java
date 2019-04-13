package com.expleague.sensearch.web.suggest;

public class BinSearchComparator extends PhraseSortingComparator {

  @Override
  protected int compare(String[] words, String[] line) {

    if (words.length < line.length)
      return -1;

    if (words.length > line.length)
      return 1;

    int len = words.length;

    for (int i = 0; i < len - 1; i++) {
      int r = words[i].compareTo(line[i]);
      if (r != 0) {
        return r;
      }
    }

    String lastTerm = line[len - 1];
    String qt = words[len - 1];

    if (qt.length() < lastTerm.length()) {
      return qt.compareTo(lastTerm.substring(0, qt.length()));
    } else {
      return qt.compareTo(lastTerm);
    }
  }

}
