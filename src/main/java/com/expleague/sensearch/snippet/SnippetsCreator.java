package com.expleague.sensearch.snippet;

import com.expleague.commons.text.lemmer.LemmaInfo;
import com.expleague.commons.text.lemmer.PartOfSpeech;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.Lemmer;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.query.term.BaseTerm;
import com.expleague.sensearch.snippet.docbased_snippet.DocBasedSnippet;
import com.expleague.sensearch.snippet.docbased_snippet.KeyWord;
import com.expleague.sensearch.snippet.passage.Passage;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by Maxim on 06.10.2018. Email: alvinmax@mail.ru
 */
public class SnippetsCreator {

  private static final long PASSAGES_IN_SNIPPET = 4;
  private static final long NUMBER_OF_KEYWORDS = 6;
  private static final double ALPHA = .4;

  private static final Pattern splitEnglish = Pattern.compile(
      "(?<=[.!?]|[.!?]['\"])(?<!Mr\\.|Mrs\\.|Ms\\.|Jr\\.|Dr\\.|Prof\\.|Vol\\.|A\\.D\\.|B\\.C\\.|Sr\\.|T\\.V\\.A\\.)\\s+");
  private static final Pattern splitRussian = Pattern
      .compile("(?<=[.!?]|[.!?]['\"])(?<!\\(р\\.|\\(род\\.|[А-Я]\\.)");

  private static final Pattern splitPattern = splitRussian;

  private boolean contains(Lemmer lemmer, CharSequence s, CharSequence t) {
    return lemmer.myStem
        .parse(s)
        .stream()
        .anyMatch(x -> (x.lemma() == null ? x.token() : x.lemma().lemma()) == t);
  }

  public Snippet getSnippet(Page document, Query query, Lemmer lemmer) {
    CharSequence title = document.title();
    CharSequence content = document.text();

    List<Passage> passages = Arrays
        .stream(splitPattern.split(content))
        .map(Passage::new)
        .collect(Collectors.toList());

    for (int i = 0; i < passages.size(); i++) {
      passages.get(i).setId(i);
    }

    Set<KeyWord> uniqueWords = passages
        .stream()
        .flatMap(passage -> lemmer.myStem
            .parse(passage
                .getSentence()
                .toString()
                .toLowerCase())
            .stream()
            .map(wordInfo -> new KeyWord(new BaseTerm(wordInfo), 0))
        )
        .collect(Collectors.toSet());

    Predicate<Passage> queryRelevant = y -> query
        .getTerms()
        .stream()
        .anyMatch(x -> contains(lemmer, y.getSentence(), x.getNormalized()));

    Predicate<Passage> notQueryRelevant = y -> query
        .getTerms()
        .stream()
        .noneMatch(x -> contains(lemmer, y.getSentence(), x.getNormalized()));

    List<Passage> passagesWithQueryWords = passages
        .stream()
        .filter(queryRelevant)
        .collect(Collectors.toList());

    List<Passage> passagesWithoutQueryWords = passages
        .stream()
        .filter(notQueryRelevant)
        .collect(Collectors.toList());

    List<KeyWord> keyWords = uniqueWords
        .stream()
        .filter(x -> {
          LemmaInfo lemmaInfo = x.getWord().getLemma();
          if (lemmaInfo == null) {
            return false;
          }
          PartOfSpeech partOfSpeech = lemmaInfo.pos();
          return partOfSpeech == PartOfSpeech.S || partOfSpeech == PartOfSpeech.V;
        })
        .peek(x -> {
          long r = passagesWithQueryWords.stream()
              .filter(y -> contains(lemmer, y.getSentence(), x.getWord().getNormalized()))
              .count();
          long R = passagesWithQueryWords.size();
          long s = passagesWithoutQueryWords.stream()
              .filter(y -> contains(lemmer, y.getSentence(), x.getWord().getNormalized()))
              .count();
          long S = passagesWithoutQueryWords.size();
          double w = Math.log((r + 0.5) * (S - s + 0.5) / ((R - r + 0.5) * (s + 0.5)));
          x.setRank(w);
        })
        .sorted(Comparator.comparingDouble(KeyWord::getRank).reversed())
        .limit(NUMBER_OF_KEYWORDS)
        .collect(Collectors.toList());

    passages = passages
        .stream()
        .peek(x -> {
          double rank = keyWords
              .stream()
              .filter(y -> contains(lemmer, x.getSentence(), y.getWord().getNormalized()))
              .mapToDouble(KeyWord::getRank)
              .sum();
          x.setRating(rank);
        })
        .collect(Collectors.toList());

    double best = passages
        .stream()
        .max(Comparator.comparingDouble(Passage::getRating))
        .get()
        .getRating();

    for (int i = 0; i < passages.size(); i++) {
      double rating = passages.get(i).getRating();
      double newRating = ALPHA * rating / best + (1 - ALPHA) * (1. - (i - 1.) / passages.size());
      passages.get(i).setRating(newRating);
    }

    List<Passage> bestPassages = passages
        .stream()
        .sorted(Comparator.comparing(Passage::getRating).reversed())
        .limit(PASSAGES_IN_SNIPPET)
        .sorted(Comparator.comparingLong(Passage::getId))
        .collect(Collectors.toList());

    return new DocBasedSnippet(title, bestPassages, query);
  }

}
