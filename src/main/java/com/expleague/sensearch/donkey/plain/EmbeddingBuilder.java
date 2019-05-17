package com.expleague.sensearch.donkey.plain;

import static com.expleague.sensearch.donkey.plain.PlainIndexBuilder.DEFAULT_VEC_SIZE;
import static com.expleague.sensearch.donkey.utils.BrandNewIdGenerator.generateTermId;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.commons.random.FastRandom;
import com.expleague.commons.seq.CharSeq;
import com.expleague.ml.embedding.Embedding;
import com.expleague.sensearch.core.IdUtils;
import com.expleague.sensearch.core.Tokenizer;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;
import com.expleague.sensearch.donkey.utils.BrandNewIdGenerator;
import com.expleague.sensearch.index.plain.QuantLSHCosIndexDB;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.log4j.Logger;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;

public class EmbeddingBuilder implements AutoCloseable {

  private static final Logger LOG = Logger.getLogger(EmbeddingBuilder.class);

  private static final int QUANT_DIM = 65;
  private static final int SKETCH_BITS_PER_QUANT = 96;
  private static final int BATCH_SIZE = 1000;

  private static final Options EMBEDDING_DB_OPTIONS =
      new Options()
          .createIfMissing(true)
          .errorIfExists(true)
          .compressionType(CompressionType.SNAPPY);

  private final Tokenizer tokenizer;
  private final Embedding<CharSeq> jmllEmbedding;
  private final TLongSet termIdsInDb = new TLongHashSet();
  private final QuantLSHCosIndexDB nnIdx;
  private final TLongObjectMap<List<CharSequence>> pageIdToIncomingLinkTexts =
      new TLongObjectHashMap<>();
  private final TLongSet existingPageIds = new TLongHashSet();

  public EmbeddingBuilder(DB vecDb, Embedding<CharSeq> jmllEmbedding, Tokenizer tokenizer) {
    this.jmllEmbedding = jmllEmbedding;
    this.tokenizer = tokenizer;
    nnIdx =
        new QuantLSHCosIndexDB(
            new FastRandom(),
            DEFAULT_VEC_SIZE,
            QUANT_DIM,
            SKETCH_BITS_PER_QUANT,
            BATCH_SIZE,
            vecDb);
  }

  @Override
  public void close() throws IOException {
    LOG.info("Finalizing EmbeddingBuilder...");
    long[] curLinkId = new long[1];
    pageIdToIncomingLinkTexts.forEachEntry(
        (targetId, linkTexts) -> {
          if (!existingPageIds.contains(targetId)) {
            return true;
          }
          curLinkId[0] = IdUtils.toStartLinkId(targetId);
          linkTexts.forEach(
              text -> {
                Vec textVec = toVector(text);
                if (textVec != null) {
                  nnIdx.append(curLinkId[0]++, textVec);
                }
              });
          return true;
        });
    nnIdx.save();
  }

  private long curPageTitleId;
  private long curPageTextId;

  public void startPage(long pageId) {
    existingPageIds.add(pageId);
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

    section
        .links()
        .forEach(
            link -> {
              final long targetId = BrandNewIdGenerator.generatePageId(link.targetUri());
              if (!pageIdToIncomingLinkTexts.containsKey(targetId)) {
                pageIdToIncomingLinkTexts.put(targetId, new ArrayList<>());
              }
              pageIdToIncomingLinkTexts.get(targetId).add(link.text());
            });

    tokenizer
        .toWords(section.text())
        .map(word -> word.toString().toLowerCase())
        .forEach(
            word -> {
              long id = generateTermId(word);
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
    final Vec[] vectors =
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

    final Vec mean = new ArrayVec(DEFAULT_VEC_SIZE);
    for (Vec vec : vectors) {
      VecTools.append(mean, vec);
    }
    return VecTools.scale(mean, 1.0 / vectors.length);
  }

  public void addTerm(long id, Vec vec) {
    nnIdx.append(id, vec);
  }
}
