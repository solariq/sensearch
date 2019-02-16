package com.expleague.sensearch.donkey.plain;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.commons.random.FastRandom;
import com.expleague.commons.seq.CharSeq;
import com.expleague.ml.embedding.Embedding;
import com.expleague.sensearch.core.Tokenizer;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;
import com.expleague.sensearch.index.plain.QuantLSHCosIndexDB;
import gnu.trove.map.TLongLongMap;
import gnu.trove.map.hash.TLongLongHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import org.iq80.leveldb.DB;

import java.io.IOException;
import java.util.Objects;

import static com.expleague.sensearch.donkey.plain.PlainIndexBuilder.DEFAULT_VEC_SIZE;
import static com.expleague.sensearch.donkey.utils.BrandNewIdGenerator.termIdGenerator;

public class EmbeddingBuilder implements AutoCloseable {
  private static final int QUANT_DIM = 10;
  private static final int MIN_DIST = 130;

  private final Tokenizer tokenizer;
  private final Embedding<CharSeq> jmllEmbedding;
  private final TLongSet termIdsInDb = new TLongHashSet();
  private final QuantLSHCosIndexDB nnIdx;
  private final TLongLongMap map = new TLongLongHashMap();
  private final TLongLongMap mapOneMore = new TLongLongHashMap();

  public EmbeddingBuilder(
      DB vecDb,
      Embedding<CharSeq> jmllEmbedding,
      Tokenizer tokenizer) {
    this.jmllEmbedding = jmllEmbedding;
    this.tokenizer = tokenizer;
    nnIdx = new QuantLSHCosIndexDB(new FastRandom(), QUANT_DIM, DEFAULT_VEC_SIZE, MIN_DIST, vecDb);
  }

  @Override
  public void close() throws IOException {
    mapOneMore.forEachEntry((linkId, targetId) -> {
      if (map.containsKey(targetId)) {
        nnIdx.append(linkId, nnIdx.get(map.get(targetId)));
      }
      return true;
    });
    nnIdx.save();
  }

  private long curSecTitleId = 0;
  private long curSecTextId = 0;
  private long curLinkId = 0;

  public void startPage(long originalId, long pageId) {
    curSecTitleId = pageId + IdUtils.START_SEC_TITLE_PREFIX;
    curSecTextId = pageId + IdUtils.START_SEC_TEXT_PREFIX;
    curLinkId = pageId + IdUtils.START_LINK_PREFIX;
    map.put(originalId, pageId);
  }

  public void addSection(CrawlerDocument.Section section) {
    nnIdx.append(curSecTitleId++, toVector(section.title()));
    nnIdx.append(curSecTextId++, toVector(section.text()));
    section.links().forEach(link -> mapOneMore.put(curLinkId++, link.targetId()));

    tokenizer.toWords(section.text()).map(word -> word.toString().toLowerCase()).forEach(word -> {
      long id = termIdGenerator(word).next();
      if (termIdsInDb.contains(id)) {
        return;
      }

      Vec vec = jmllEmbedding.apply(CharSeq.compact(word));
      if (vec != null) {
        nnIdx.append(id, vec);
      }
      termIdsInDb.add(id);
    });
  }

  public void endPage() {
  }

  private Vec toVector(CharSequence text) {
    Vec[] vectors =
        tokenizer
            .parseTextToWords(text)
            .map(word -> word.toString().toLowerCase())
            .map(CharSeq::intern)
            .map(jmllEmbedding)
            .filter(Objects::nonNull)
            .toArray(Vec[]::new);

    if (vectors.length == 0) {
      return null;
    }

    ArrayVec mean = new ArrayVec(DEFAULT_VEC_SIZE);
    for (Vec vec : vectors) {
      VecTools.append(mean, vec);
    }
    return VecTools.scale(mean, 1.0 / vectors.length);
  }
}
