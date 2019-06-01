package com.expleague.sensearch.web.suggest.pool;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import java.util.stream.Collectors;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.index.Index;

public class UnsortedSuggester extends LearnedSuggester {

  private final Random rnd = new Random(10);
  public UnsortedSuggester(Index index, Path suggestIndexRoot) throws IOException {
    super(index, suggestIndexRoot);
  }

  @Override
  List<String> getSuggestions(List<Term> terms) throws IOException {

    if (terms.isEmpty()) {
      return Collections.emptyList();
    }


    TreeSet<StringDoublePair> phraseProb = new TreeSet<>();

    List<QSUGItem> rawResults = getUnsortedSuggestions(terms);

    //System.out.println("number of selected phrases: " + endingPhrases.size());
    for (QSUGItem p : rawResults) {
      phraseProb.add(
          new StringDoublePair(p.suggestion, rnd.nextDouble()));

    }

    return phraseProb.stream()
        .sorted()
        .limit(RETURN_LIMIT)
        .map(p -> p.phrase)
        .collect(Collectors.toList());
  }
  
  @Override
  public String getName() {
    return "Unsorted";
  }

}
