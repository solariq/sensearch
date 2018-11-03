package com.expleague.sensearch.core.impl;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.commons.seq.CharSeqTools;
import com.expleague.sensearch.Config;
import com.expleague.sensearch.core.Embedding;
import com.expleague.sensearch.index.IndexedPage;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.query.term.Term;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

public class EmbeddingImpl implements Embedding {

  private static final int VEC_SIZE = 130;
  private static final double EPSILON = 10e-9;

  private static EmbeddingImpl instance;

  private final Map<String, Vec> wordVecMap = new HashMap<>();
  private final Map<Long, Vec> docIdVecMap = new HashMap<>();
  private final Map<Long, Stream<Vec>> docIdVecsMap = new HashMap<>();

  private BiFunction<Vec, Vec, Double> nearestMeasure = VecTools::distanceAV;
  private Function<IndexedPage, Stream<String>> keyWordsFunc = page -> split(page.title());
  private boolean vecForDocMode = true;

  public EmbeddingImpl(Config config) {
    try (Reader input = new InputStreamReader(
            new GZIPInputStream(new FileInputStream(config.getEmbeddingVectors())),
            StandardCharsets.UTF_8)) {
      CharSeqTools.lines(input)
              .parallel()
              .forEach(line -> {
                CharSequence[] parts = CharSeqTools.split(line, ' ');
                final String word = parts[0].toString();
                double[] doubles = Arrays.stream(parts, 1, parts.length)
                        .mapToDouble(CharSeqTools::parseDouble)
                        .toArray();
                final Vec vec = new ArrayVec(doubles);
                synchronized (wordVecMap) {
                  wordVecMap.put(word, vec);
                }
              });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Vec getVec(String word) {
    return wordVecMap.get(word.toLowerCase());
  }

  @Override
  public Vec getVec(Long documentId) {
    return docIdVecMap.get(documentId);
  }

  @Override
  public Vec getVec(Query query) {
    return getArithmeticMean(getVecsForTerms(query).stream());
  }

  @Override
  public Vec getVec(List<Term> terms) {
    return getArithmeticMean(terms.stream()
            .map(t -> getVec(t.getNormalized().toString()))
    );
  }

  @Override
  public List<Vec> getVecsForTerms(Query query) {
    return query.getTerms().stream()
            .map(t -> getVec(t.getNormalized().toString()))
            .collect(Collectors.toList());
  }

  @Override
  public List<String> getNearestWords(Vec mainVec, int numberOfNeighbors) {
    return getNearest(numberOfNeighbors, getComparator(mainVec), wordVecMap);
  }

  @Override
  public List<Long> getNearestDocuments(Vec mainVec, int numberOfNeighbors) {
    Comparator<Vec> comparator = getComparator(mainVec);
    if (vecForDocMode) {
      return getNearest(numberOfNeighbors, comparator, docIdVecMap);
    }
    Map<Long, Vec> newMap = new HashMap<>();
    for (Map.Entry<Long, Stream<Vec>> entry : docIdVecsMap.entrySet()) {
      Optional<Vec> optionalVec = entry.getValue().min(comparator);
      optionalVec.ifPresent(vec -> newMap.put(entry.getKey(), vec));
    }
    return getNearest(numberOfNeighbors, comparator, newMap);
  }

  void setDocuments(Stream<IndexedPage> documentStream) {
    documentStream.forEach(document -> {
      docIdVecMap.put(document.id(), getVec(document));
      /*docIdVecsMap.put(document.id(),
              Arrays.stream(document.text().toString()
                      .split("\n\n"))
                      .map(p -> getArithmeticMean(
                              Arrays.stream(p.split(" ")).map(this::getVec))));*/
    });
  }

  private Vec getArithmeticMean(Stream<Vec> vecs) {
    ArrayVec mean = new ArrayVec(new double[VEC_SIZE]);
    long number = vecs.filter(Objects::nonNull).peek(vec -> mean.add((ArrayVec) vec)).count();
    mean.scale(1.0 / ((double) number));
    return mean;
  }

  private Vec getVec(IndexedPage document) {
    return getArithmeticMean(keyWordsFunc.apply(document).map(this::getVec));
  }

  private Comparator<Vec> getComparator(Vec mainVec) { ;
    return (vec1, vec2) -> {
      double val1 = nearestMeasure.apply(mainVec, vec1);
      double val2 = nearestMeasure.apply(mainVec, vec2);
      if (Math.abs(val1 - val2) < EPSILON) {
        return 0;
      }
      return val1 < val2 ? -1 : 1;
    };
  }

  private <T> List<T> getNearest(int numberOfNeighbors, Comparator<Vec> comparator, Map<T, Vec> map) {
    TreeMap<Vec, T> nearest = new TreeMap<>(comparator);
    for (Map.Entry<T, Vec> e : map.entrySet()) {
      if (nearest.size() < numberOfNeighbors) {
        nearest.put(e.getValue(), e.getKey());
      } else if (comparator.compare(nearest.lastKey(), e.getValue()) > 0) {
        nearest.pollLastEntry();
        nearest.put(e.getValue(), e.getKey());
      }
    }
    return new ArrayList<>(nearest.values());
  }

  public void switchMeasureToEuclidean() {
    nearestMeasure = VecTools::distanceAV;
  }

  public void switchMeasureToCosine() {
    nearestMeasure = (vec1, vec2) -> 1 - VecTools.cosine(vec1, vec2);
  }

  private Stream<String> split(CharSequence charSequence) {
    //todo replace with tokenizer
    //return Arrays.stream(charSequence.toString().split(" "));
    return Arrays.stream(CharSeqTools.split(charSequence, ' ')).map(CharSequence::toString);
  }
}