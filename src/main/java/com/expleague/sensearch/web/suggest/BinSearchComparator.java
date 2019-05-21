package com.expleague.sensearch.web.suggest;

public class BinSearchComparator extends PhraseSortingComparator {

  @Override
  protected int compare(String[] words, String[] line) {

    int comparingLength = Math.min(words.length - 1, line.length);

    for (int i = 0; i < comparingLength; i++) {
      int r = words[i].compareTo(line[i]);
      if (r != 0) {
        return r;
      }
    }

    if (line.length < words.length) {
      return 1;
    }
    
    
    if (line[words.length - 1].startsWith(words[words.length - 1])) {
      return 0;
    }

    return words[words.length - 1].compareTo(line[words.length - 1]);
  }
}
