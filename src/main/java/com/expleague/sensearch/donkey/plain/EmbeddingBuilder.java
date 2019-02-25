package com.expleague.sensearch.donkey.plain;

import static com.expleague.sensearch.donkey.plain.PlainIndexBuilder.DEFAULT_VEC_SIZE;
import static com.expleague.sensearch.donkey.utils.BrandNewIdGenerator.termIdGenerator;

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
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongLongHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.iq80.leveldb.DB;

public class EmbeddingBuilder implements AutoCloseable {
  private static final int QUANT_DIM = 10;
  private static final int BATCH_SIZE = 2000;

  private final Tokenizer tokenizer;
  private final Embedding<CharSeq> jmllEmbedding;
  private final TLongSet termIdsInDb = new TLongHashSet();
  private final QuantLSHCosIndexDB nnIdx;
  private final TLongLongMap wikiIdToIndexIdMap = new TLongLongHashMap();
  private final TLongObjectMap<List<CharSequence>> wikiIdToLinkTexts = new TLongObjectHashMap<>();

  public EmbeddingBuilder(DB vecDb, Embedding<CharSeq> jmllEmbedding, Tokenizer tokenizer) {
    this.jmllEmbedding = jmllEmbedding;
    this.tokenizer = tokenizer;
    nnIdx = new QuantLSHCosIndexDB(new FastRandom(), QUANT_DIM, DEFAULT_VEC_SIZE, BATCH_SIZE, vecDb);
  }

  @Override
  public void close() throws IOException {
    long[] curLinkId = new long[1];
    wikiIdToLinkTexts.forEachEntry((wikiId, linkTexts) -> {
      if (wikiIdToIndexIdMap.containsKey(wikiId)) {
        long pageId = wikiIdToIndexIdMap.get(wikiId);
        curLinkId[0] = IdUtils.toStartLinkId(pageId);
        linkTexts.forEach(text -> {
          Vec textVec = toVector(text);
          if (textVec != null) {
            nnIdx.append(curLinkId[0]++, textVec);
          }
        });
      }
      return true;
    });
    nnIdx.save();
  }

  private long curPageTitleId;
  private long curPageTextId;

  public void startPage(long originalId, long pageId) {
    wikiIdToIndexIdMap.put(originalId, pageId);
    curPageTitleId = IdUtils.toStartSecTitleId(pageId);
    curPageTextId = IdUtils.toStartSecTextId(pageId);
  }

  public void addSection(CrawlerDocument.Section section, long sectionId) {
    final Vec titleVec = toVector(section.title());
    if (titleVec != null) {
      nnIdx.append(curPageTitleId++, titleVec);
    }

    final Vec textVec = toVector(section.text());
    if (textVec != null) {
      nnIdx.append(curPageTextId++, textVec);
    }

    section.links().forEach(link -> {
      long wikiId = link.targetId();
      if (!wikiIdToLinkTexts.containsKey(wikiId)) {
        wikiIdToLinkTexts.put(wikiId, new ArrayList<>());
      }
      wikiIdToLinkTexts.get(wikiId).add(link.text());
    });

    tokenizer.toWords(section.text()).map(word -> word.toString().toLowerCase()).forEach(word -> {
      long id = termIdGenerator(word).next();
      if (termIdsInDb.contains(id)) {
        return;
      }

      final Vec vec = jmllEmbedding.apply(CharSeq.compact(word));
      if (vec != null) {
        nnIdx.append(id, vec);
      }
      termIdsInDb.add(id);
    });
  }

  public void endPage() {}

  private Vec toVector(CharSequence text) {
    final Vec[] vectors = tokenizer.parseTextToWords(text)
        .map(word -> word.toString().toLowerCase())
        .map(CharSeq::intern)
        .map(jmllEmbedding)
        .filter(Objects::nonNull)
        .toArray(Vec[]::new);

    if (vectors.length == 0) {
      return null;
    }

    final Vec mean = new ArrayVec(DEFAULT_VEC_SIZE);
    for (Vec vec : vectors) {
      VecTools.append(mean, vec);
    }
    return VecTools.scale(mean, 1.0 / vectors.length);
  }
}
