package com.expleague.sensearch.core.impl;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.commons.seq.CharSeqTools;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

public class EmbeddingImpl implements Embedding {

  private static EmbeddingImpl instance;

  private final Map<String, Vec> wordVecMap = new HashMap<>();
  private final Map<Long, Vec> docIdVecMap = new HashMap<>();

  private EmbeddingImpl() {
    try (Reader input = new InputStreamReader(
        new GZIPInputStream(new FileInputStream(Constants.getEmbeddingVectors())),
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

  public static synchronized EmbeddingImpl getInstance() {
    if (instance == null) {
      instance = new EmbeddingImpl();
    }
    return instance;
  }

  void setDocuments(Stream<IndexedPage> documentStream) {
    documentStream.forEach(document -> docIdVecMap.put(document.id(), getVec(document)));
  }

  Map<String, Vec> getWordVecMap() {
    return this.wordVecMap;
  }

  Map<Long, Vec> getDocIdVecMap() {
    return this.docIdVecMap;
  }

  private static Vec getArithmeticMean(List<Vec> vecs) {
    ArrayVec mean = new ArrayVec(new double[130]);
    int nullNumber = 0;
    for (Vec vec : vecs) {
      if (vec != null) {
        mean.add((ArrayVec) vec);
      } else {
        nullNumber++;
      }
    }
    mean.scale(1.0 / ((double) vecs.size() - nullNumber));
    return mean;
  }

  @Override
  public Vec getVec(String word) {
    return this.wordVecMap.get(word.toLowerCase());
  }

  @Override
  public Vec getVec(Long documentId) {
    return this.docIdVecMap.get(documentId);
  }

  @Override
  public Vec getVec(Query query) {
    return getArithmeticMean(getVecsForTerms(query));
  }

  public Vec getVec(List<Term> terms) {
    return getArithmeticMean(terms.stream()
        .map(t -> getVec(t.getNormalized().toString()))
        .collect(Collectors.toList()));
  }

  @Override
  public List<Vec> getVecsForTerms(Query query) {
    return query.getTerms().stream()
        .map(t -> getVec(t.getNormalized().toString()))
        .collect(Collectors.toList());
  }

  private Vec getVec(IndexedPage document) {
    //todo replace for "smart" tokenizer when it zavezut
    return getArithmeticMean(Arrays.stream(
        document.title().toString().split(" "))
        .map(this::getVec)
        .collect(Collectors.toList()));
  }
}
