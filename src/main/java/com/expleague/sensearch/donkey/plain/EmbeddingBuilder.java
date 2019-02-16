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
  private QuantLSHCosIndexDB nnIdx;

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
    nnIdx.save();
  }

  private long curPageId = 0;
  private Vec curPageVec = null;

  //TODO: impl
  public void startPage(long originalId, long pageId) {
    if (curPageId != 0) {
      throw new IllegalStateException(
          "Invalid call to startPage(): already processing page [" + curPageId + "]");
    }
    curPageId = pageId;
  }

  //TODO: impl
  public void addTitle(String title) {
    curPageVec = toVector(title);
    addText(title);
  }

  //TODO: impl
  public void addSection(CrawlerDocument.Section section) {

  }

  //TODO: impl
  public void endPage() {
    if (curPageVec != null) {
      nnIdx.append(curPageId, curPageVec);
    }
    curPageId = 0;
    curPageVec = null;
  }

  public void addText(String text) {
    tokenizer.toWords(text).map(word -> word.toString().toLowerCase()).forEach(word -> {
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
