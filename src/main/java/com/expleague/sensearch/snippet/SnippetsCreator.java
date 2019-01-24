package com.expleague.sensearch.snippet;

import com.expleague.commons.seq.CharSeqTools;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.Page.SegmentType;
import com.expleague.sensearch.core.PartOfSpeech;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.snippet.docbased_snippet.DocBasedSnippet;
import com.expleague.sensearch.snippet.docbased_snippet.KeyWord;
import com.expleague.sensearch.snippet.passage.Passage;
import com.google.inject.Inject;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by Maxim on 06.10.2018. Email: alvinmax@mail.ru
 */
public class SnippetsCreator {

  private static final long PASSAGES_IN_SNIPPET = 4;
  private static final long NUMBER_OF_KEYWORDS = 6;
  private static final int OPTIMAL_LENGTH_OF_PASSAGE = 150;
  private static final double QUESTION_COEFFICIENT = .77;
  private static final double ALPHA = .4;

  private final Index index;

  @Inject
  public SnippetsCreator(Index index) {
    this.index = index;
  }

  private boolean containsWithLemma(Passage passage, Term term) {
    return passage
        .words()
        .anyMatch(x -> x.lemma() == term.lemma());
  }

  private boolean contains(Passage passage, Term term) {
    return passage
        .words()
        .anyMatch(x -> x == term);
  }


  public Snippet getSnippet(Page document, Query query) {
    CharSequence title = document.content(SegmentType.SECTION_TITLE);
    CharSequence content = document.content();
/*
    document
        .sentences()
        .forEach(System.out::println);
*/
    List<Passage> passages = document
        .sentences(SegmentType.SUB_BODY)
        .map(x -> new Passage(x, index.parse(x)))
        .collect(Collectors.toList());

    for (int i = 0; i < passages.size(); i++) {
      passages.get(i).setId(i);
    }

    Set<KeyWord> uniqueWords =
        passages
            .stream()
            .flatMap(
                passage ->
                    passage
                        .words()
                        .map(KeyWord::new))
            .collect(Collectors.toCollection(HashSet::new));

    Predicate<Passage> queryRelevant =
        passage -> query.terms().stream()
            .anyMatch(term -> containsWithLemma(passage, term));

    Predicate<Passage> notQueryRelevant =
        passage -> query.terms().stream()
            .noneMatch(term -> containsWithLemma(passage, term));

    List<Passage> passagesWithQueryWords =
        passages
            .stream()
            .filter(queryRelevant)
            .collect(Collectors.toList());

    List<Passage> passagesWithoutQueryWords =
        passages
            .stream()
            .filter(notQueryRelevant)
            .collect(Collectors.toList());

    List<KeyWord> keyWords =
        uniqueWords
            .stream()
            .filter(x -> x.word().partOfSpeech() == PartOfSpeech.S)
            .peek(
                x -> {
                  long r =
                      passagesWithQueryWords
                          .stream()
                          .filter(passage -> containsWithLemma(passage, x.word()))
                          .count();
                  long R =
                      passagesWithQueryWords
                          .stream()
                          .count();
                  long s =
                      passagesWithoutQueryWords
                          .stream()
                          .filter(passage -> containsWithLemma(passage, x.word()))
                          .count();
                  long S =
                      passagesWithoutQueryWords
                          .stream()
                          .count();
                  //System.out.println(x.word().text() + " " + r + " " + R + " " + s + " " + S);
                  double w = Math.log((r + 0.5) * (S - s + 0.5) / ((R - r + 0.5) * (s + 0.5)));
                  x.setRank(w);
                })
            .sorted(Comparator.comparingDouble(KeyWord::rank).reversed())
            .limit(NUMBER_OF_KEYWORDS)
            .collect(Collectors.toList());

    passages =
        passages
            .stream()
            .peek(
                passage -> {
                  double rank =
                      keyWords
                          .stream()
                          .filter(keyWord -> containsWithLemma(passage, keyWord.word()))
                          .mapToDouble(KeyWord::rank)
                          .sum();
                  passage.setRating(rank);
                })
            .collect(Collectors.toList());

    double best = passages.stream().mapToDouble(Passage::getRating).max().orElse(1);

    for (Passage passage : passages) {
      double newRating = (passage.getRating() / best)
          * lengthEvaluation(passage)
          //* positionEvaluation(passage, passages.size())
          * questionEvaluation(passage)
          //* queryEvaluation(passage, query)
          //* queryEvaluationWithLemma(passage, query)
          ;

      //System.out.println(newRating + " " + passage.getSentence());

      passage.setRating(newRating);
    }

    List<Passage> bestPassages =
        passages
            .stream()
            .sorted(Comparator.comparing(Passage::getRating).reversed())
            .limit(PASSAGES_IN_SNIPPET)
            // .sorted(Comparator.comparingLong(Passage::getId))
            .collect(Collectors.toList());

    return new DocBasedSnippet(title, bestPassages, query);
  }

  private double queryEvaluation(Passage passage, Query query) {
    return (query.terms().stream().filter(x -> contains(passage, x)).count() + .0) / (
        query.terms().size() + .0);
  }

  private double queryEvaluationWithLemma(Passage passage, Query query) {
    return (query.terms().stream().filter(x -> containsWithLemma(passage, x)).count() + .0) / (
        query.terms().size() + .0);
  }

  private double lengthEvaluation(Passage passage) {
    return Math.pow(
        Math.exp(1),
        Math.pow(
            (passage.getSentence().length() - OPTIMAL_LENGTH_OF_PASSAGE) / 300.,
            2
        )
    );
  }

  private double questionEvaluation(Passage passage) {
    return CharSeqTools.indexOf(passage.getSentence(), "?") == -1 ? 1. : QUESTION_COEFFICIENT;
  }

  private double positionEvaluation(Passage passage, int n) {
    return 1. - (passage.getId() + .0) / (n + .0);
  }


}
