package components.snippeter;

import com.expleague.commons.text.lemmer.PartOfSpeech;
import components.Lemmer;
import components.index.IndexedDocument;
import components.query.Query;
import components.query.term.Term;
import components.snippeter.docbased_snippet.DocBasedSnippet;
import components.snippeter.docbased_snippet.KeyWord;
import components.snippeter.passage.Passage;
import components.snippeter.passage.Passages;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import com.expleague.commons.text.lemmer.WordInfo;

/**
 * Created by Maxim on 06.10.2018. Email: alvinmax@mail.ru
 */
public class SnippetsCreator {

  private static final int PASSAGES_IN_SNIPPET = 4;

  private static final Pattern splitEnglish = Pattern.compile(
      "(?<=[.!?]|[.!?]['\"])(?<!Mr\\.|Mrs\\.|Ms\\.|Jr\\.|Dr\\.|Prof\\.|Vol\\.|A\\.D\\.|B\\.C\\.|Sr\\.|T\\.V\\.A\\.)\\s+");
  private static final Pattern splitRussian = Pattern
      .compile("(?<=[.!?]|[.!?]['\"])(?<!\\(р\\.|\\(род\\.|[А-Я]\\.)");

  private static final Pattern splitPattern = splitRussian;
  private static final Lemmer lemmer = Lemmer.getInstance();

  public Snippet getSnippet(IndexedDocument document, Query query) {

    CharSequence title = document.getTitle();
    CharSequence content = document.getContent();

    List<Passage> passages = Arrays
        .stream(splitPattern.split(content))
        .map(Passage::new)
        .collect(Collectors.toList());

    for (int i = 0; i < passages.size(); i++) {
      passages.get(i).setId(i);
    }
/*
    System.out.println(passages.size());
    for (Passage x : passages) {
      System.out.println(x.getSentence());
    }
*/
    Set<CharSequence> uniqueWords = passages
        .stream()
        .flatMap(x -> Arrays
            .stream(x
                .getSentence()
                .toString()
                .toLowerCase()
                .split("[\\s\\p{Punct}]+")))
        .collect(Collectors.toSet());
/*
    System.out.println(uniqueWords.size());
    for (CharSequence word : uniqueWords) {
      System.out.println(word);
    }
*/

    Predicate<Passage> queryRelevant = y -> query.getTerms().stream()
        .anyMatch(x -> Passages.contains(y.getSentence(), x.getRaw()));

    Predicate<Passage> notQueryRelevant = y -> query.getTerms().stream()
        .noneMatch(x -> Passages.contains(y.getSentence(), x.getRaw()));

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
          WordInfo wordInfo = lemmer.myStem.parse(x).get(0);
          if (wordInfo == null || wordInfo.lemma() == null) {
            return false;
          }
          PartOfSpeech partOfSpeech = wordInfo.lemma().pos();
          return partOfSpeech == PartOfSpeech.S || partOfSpeech == PartOfSpeech.V;
        })
        .map(x -> {
          long r = passagesWithQueryWords.stream()
              .filter(y -> Passages.contains(y.getSentence(), x))
              .count();
          long R = passagesWithQueryWords.size();
          long s = passagesWithoutQueryWords.stream()
              .filter(y -> Passages.contains(y.getSentence(), x))
              .count();
          long S = passagesWithoutQueryWords.size();
          double w = Math.log((r + 0.5) * (S - s + 0.5) / ((R - r + 0.5) * (s + 0.5)));
          return new KeyWord(x, w);
        })
        .sorted(Comparator.comparingDouble(KeyWord::getRank).reversed())
        .collect(Collectors.toList())
        .subList(0, Math.min(6, passages.size()));

    passages = passages
        .stream()
        .peek(x -> {
          double rank = keyWords
              .stream()
              .filter(y -> Passages.contains(x.getSentence(), y.getWord()))
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

    double alpha = 0.4;

    for (int i = 0; i < passages.size(); i++) {
      double rating = passages.get(i).getRating();
      double newRating = alpha * rating / best + (1 - alpha) * (1.0 - (i - 1.0) / passages.size());
      passages.get(i).setRating(newRating);
    }

    passages.sort(Comparator.comparing(Passage::getRating).reversed());

    List<Passage> bestPassages = passages.subList(0, Math.min(PASSAGES_IN_SNIPPET, passages.size()));
    bestPassages.sort(Comparator.comparingLong(Passage::getId));

    return new DocBasedSnippet(title, bestPassages, query);
  }

}
