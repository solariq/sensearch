package com.expleague.sensearch.donkey.writers;

import static com.expleague.sensearch.donkey.plain.PlainIndexCreator.DEFAULT_VEC_SIZE;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.random.FastRandom;
import com.expleague.sensearch.core.PageIdUtils;
import com.expleague.sensearch.donkey.embedding.EmbeddedPage;
import com.expleague.sensearch.index.plain.QuantLSHCosIndexDB;
import java.io.IOException;
import org.iq80.leveldb.DB;

public class EmbeddingWriter implements Writer<EmbeddedPage> {

  private static final int QUANT_DIM = 65;
  private static final int SKETCH_BITS_PER_QUANT = 96;
  private static final int BATCH_SIZE = 1000;

  private final QuantLSHCosIndexDB nnIdx;

  public EmbeddingWriter(DB vecDb) {
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
  public void write(EmbeddedPage page) {
    {
      int titleVecCnt = 0;
      for (Vec titleVec : page.titleVecs()) {
        nnIdx.append(PageIdUtils.toStartSecTitleId(page.pageId()) + titleVecCnt, titleVec);
        titleVecCnt++;
      }
    }

    {
      int textVecCnt = 0;
      for (Vec textVec : page.textVecs()) {
        nnIdx.append(PageIdUtils.toStartSecTextId(page.pageId()) + textVecCnt, textVec);
        textVecCnt++;
      }
    }

    {
      int linkVecCnt = 0;
      for (Vec linkVec : page.linkVecs()) {
        nnIdx.append(PageIdUtils.toStartLinkId(page.pageId()) + linkVecCnt, linkVec);
        linkVecCnt++;
      }
    }
  }

  @Override
  public void close() throws IOException {
    nnIdx.save();
    nnIdx.close();
  }

  @Override
  public void flush() throws IOException {
    throw new UnsupportedOperationException("Batching should be added to LSH firstly");
  }
}
