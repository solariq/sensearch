package com.expleague.sensearch.donkey.plain;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.commons.seq.CharSeqTools;
import com.expleague.sensearch.Config;
import com.expleague.sensearch.core.Tokenizer;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

/**
 * Created by sandulmv on 11.11.18.
 */
public class EmbeddingBuilder {

  private static final Logger LOG = Logger.getLogger(EmbeddingBuilder.class.getName());
  private final TLongObjectMap<Vec> vectorsMap = new TLongObjectHashMap<>();
  private final Map<String, Vec> wordVectorsMap = new HashMap<>();
  private final Function<CrawlerDocument, Stream<String>> keyWordsFunc =
      page -> Stream.of(Tokenizer.tokenize(page.getTitle()));
  private final int VEC_SIZE = 50;

  private final Config config;
  EmbeddingBuilder(Config config) {

    this.config = config;;
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
            synchronized (wordVectorsMap) {
              wordVectorsMap.put(word, vec);
            }
          });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  void addNewPageVector(CrawlerDocument crawlerDocument, long pageId) {
    if (vectorsMap.containsKey(pageId)) {
      LOG.severe("Page ID collision while trying to add new vector fot the page!");
      throw new RuntimeException("Page ID collision while trying to add new vector fot the page!");
    }

    vectorsMap.put(pageId, buildDocumentVector(crawlerDocument));
  }

  /**
   * @param knownWordIds String to Long map, contains all know mapping to ids for words from
   * collection
   * @return String->Long map enriched with words from already existing String->Vec map
   */
  TObjectIntMap<String> replaceWordsWithIds(TObjectIntMap<String> knownWordIds) {
    TObjectIntMap<String> enrichedIdMap = new TObjectIntHashMap<>(knownWordIds);
    int currentWordId = enrichedIdMap.size();
    for (Map.Entry<String, Vec> gloveEntry : wordVectorsMap.entrySet()) {
      String word = gloveEntry.getKey();
      if (!enrichedIdMap.containsKey(word)) {
        enrichedIdMap.putIfAbsent(word, currentWordId++);
      }

      long vecId = enrichedIdMap.get(word);
      vectorsMap.put(vecId, gloveEntry.getValue());
    }
    return enrichedIdMap;
  }

  void flushVectors() {
    // TODO: save vectors to disk
  }

  private Vec getArithmeticMean(Stream<Vec> vecs) {
    ArrayVec mean = new ArrayVec(new double[VEC_SIZE]);
    long number = vecs.filter(Objects::nonNull).peek(vec -> mean.add((ArrayVec) vec)).count();
    mean.scale(1.0 / ((double) number));
    return mean;
  }

  private Vec buildDocumentVector(CrawlerDocument crawlerDocument) {
    return getArithmeticMean(keyWordsFunc.apply(crawlerDocument)
        .map(w -> wordVectorsMap.get(w.toLowerCase())));
  }
}
