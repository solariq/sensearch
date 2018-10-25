package components.embedding.impl;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.commons.seq.CharSeqTools;
import components.embedding.Embedding;
import components.index.IndexedDocument;
import components.query.Query;
import components.query.term.Term;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

public class EmbeddingImpl implements Embedding {

  private static EmbeddingImpl instance;

  private final Map<String, Vec> wordVecMap = new HashMap<>();
  private final Map<Long, Vec> docIdVecMap = new HashMap<>();

  private EmbeddingImpl() {
    try (Reader input = new InputStreamReader(
        new GZIPInputStream(new FileInputStream("resources/vectors.txt.gz")),
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

  public void setDocuments(Stream<IndexedDocument> documentStream) {
    documentStream.forEach(document -> docIdVecMap.put(document.getId(), getVec(document)));
  }

  Map<String, Vec> getWordVecMap() {
    return this.wordVecMap;
  }

  Map<Long, Vec> getDocIdVecMap() {
    return this.docIdVecMap;
  }

  private Vec getArithmeticMean(List<Vec> vecs) {
    Vec mean = Vec.EMPTY;
    for (Vec vec : vecs) {
      ((ArrayVec) mean).add((ArrayVec) vec);
    }
    ((ArrayVec) mean).scale(1.0 / ((double) vecs.size()));
    return mean;
  }

  @Override
  public Vec getVec(String word) {
    return this.wordVecMap.get(word);
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

  private Vec getVec(IndexedDocument document) {
    //todo replace for "smart" tokenizer when it zavezut
    return getArithmeticMean(Arrays.stream(
        document.getTitle().toString().split(" "))
        .map(this::getVec)
        .collect(Collectors.toList()));
  }
}
